<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="使用系统中唯一的小程序配置；用户在小程序端订阅后才能接收消息。" type="info" show-icon :closable="false" />

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <span>我的订阅模板</span>
          <el-button type="primary" v-hasPermi="['wxma:operations:query']" @click="loadTemplates">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="templateLoading" :data="templates" border stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="priTmplId" label="模板 ID" min-width="290" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="scope"><el-tag>{{ templateType(scope.row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="模板内容" min-width="280" show-overflow-tooltip>
          <template #default="scope">{{ readableTemplateContent(scope.row.content) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" v-hasPermi="['wxma:operations:send']" @click="openSend(scope.row)">发送</el-button>
            <el-button link type="danger" v-hasPermi="['wxma:operations:edit']" @click="removeTemplate(scope.row.priTmplId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header><span>从公共模板库添加</span></template>
      <el-form :inline="true" :model="publicTemplateForm">
        <el-form-item label="服务类目">
          <el-select v-model="publicTemplateForm.categoryIds" multiple collapse-tags placeholder="选择一个或多个类目" style="width: 360px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-hasPermi="['wxma:operations:query']" @click="loadCategories">刷新类目</el-button>
          <el-button type="primary" v-hasPermi="['wxma:operations:query']" @click="loadPublicTemplates">查询公共模板</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="publicLoading" :data="publicTemplates" border stripe>
        <el-table-column prop="tid" label="标题 ID" width="110" />
        <el-table-column prop="title" label="标题" min-width="260" />
        <el-table-column prop="categoryId" label="类目 ID" width="150" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="scope"><el-tag type="info">{{ templateType(scope.row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button link type="primary" v-hasPermi="['wxma:operations:edit']" @click="openAddTemplate(scope.row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addDialog" title="添加订阅模板" width="760px">
      <el-form :model="templateAddForm" label-width="100px">
        <el-form-item label="模板标题"><el-input :model-value="templateAddForm.title" disabled /></el-form-item>
        <el-form-item label="场景说明"><el-input v-model="templateAddForm.sceneDesc" maxlength="15" show-word-limit /></el-form-item>
        <el-form-item label="选择关键词">
          <el-table v-loading="keywordLoading" :data="keywords" border @selection-change="handleKeywordSelection">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="kid" label="ID" width="80" />
            <el-table-column prop="name" label="关键词" width="160" />
            <el-table-column prop="example" label="示例" min-width="180" />
            <el-table-column prop="rule" label="规则" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAddTemplate">添加模板</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sendDialog" title="发送订阅消息" width="820px">
      <el-form :model="sendForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="接收人 OpenID" required><wx-user-select v-model="sendForm.toUser" app-type="1" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所选模板"><el-input :model-value="selectedTemplate.title" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="跳转页面"><el-input v-model="sendForm.page" placeholder="pages/index/index?foo=bar" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="版本"><el-select v-model="sendForm.miniprogramState"><el-option label="正式版" value="formal" /><el-option label="体验版" value="trial" /><el-option label="开发版" value="developer" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="语言"><el-select v-model="sendForm.lang"><el-option label="简体中文" value="zh_CN" /><el-option label="英文" value="en_US" /><el-option label="繁体中文" value="zh_HK" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="模板内容">
          <div class="field-list">
            <el-alert v-if="!sendData.length" title="该模板没有解析到可填写的内容项，请刷新模板后重试。" type="warning" :closable="false" show-icon />
            <el-table v-else :data="sendData" border>
              <el-table-column label="内容项" width="230">
                <template #default="scope">
                  <div class="field-label">{{ scope.row.label }}</div>
                  <el-text size="small" type="info">{{ scope.row.hint }}</el-text>
                </template>
              </el-table-column>
              <el-table-column label="填写内容">
                <template #default="scope"><el-input v-model="scope.row.value" :placeholder="`请输入${scope.row.label}`" /></template>
              </el-table-column>
            </el-table>
            <div class="template-preview">消息展示结构：<span>{{ selectedTemplate.content }}</span></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="sendSubscribe">发送消息</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WxMaSubscribeMessage">
import { getCurrentInstance, reactive, ref } from "vue";
import WxUserSelect from "@/components/WxUserSelect/index.vue";
import {
  getTemplateCategories, getPublicTemplates, getPublicTemplateKeywords, getTemplates,
  addTemplate, deleteTemplate, sendSubscribeMessage,
} from "@/api/wxma/wxoperations";

const { proxy } = getCurrentInstance();
const templates = ref([]);
const categories = ref([]);
const publicTemplates = ref([]);
const keywords = ref([]);
const templateLoading = ref(false);
const publicLoading = ref(false);
const keywordLoading = ref(false);
const submitting = ref(false);
const addDialog = ref(false);
const sendDialog = ref(false);
const publicTemplateForm = reactive({ categoryIds: [] });
const templateAddForm = reactive({ templateTitleId: "", title: "", keywordIds: [], sceneDesc: "" });
const sendForm = reactive({ toUser: "", templateId: "", page: "pages/index/index", miniprogramState: "formal", lang: "zh_CN" });
const selectedTemplate = reactive({ title: "", content: "" });
const sendData = ref([]);

function templateType(type) { return Number(type) === 2 ? "一次性" : Number(type) === 3 ? "长期" : "订阅"; }
async function loadTemplates() {
  templateLoading.value = true;
  try {
    const response = await getTemplates();
    templates.value = Array.isArray(response.data) ? response.data : [];
  } finally { templateLoading.value = false; }
}
async function loadCategories() {
  const response = await getTemplateCategories();
  categories.value = Array.isArray(response.data) ? response.data : [];
}
async function loadPublicTemplates() {
  if (!publicTemplateForm.categoryIds.length) return proxy.$modal.msgWarning("请先选择服务类目");
  publicLoading.value = true;
  try {
    const response = await getPublicTemplates({ ids: publicTemplateForm.categoryIds.join(","), start: 0, limit: 30 });
    publicTemplates.value = response.data?.data || [];
  } finally { publicLoading.value = false; }
}
async function openAddTemplate(row) {
  templateAddForm.templateTitleId = String(row.tid);
  templateAddForm.title = row.title;
  templateAddForm.keywordIds = [];
  templateAddForm.sceneDesc = "";
  addDialog.value = true;
  keywordLoading.value = true;
  try {
    const response = await getPublicTemplateKeywords(row.tid);
    keywords.value = Array.isArray(response.data) ? response.data : [];
  } finally { keywordLoading.value = false; }
}
function handleKeywordSelection(rows) { templateAddForm.keywordIds = rows.map(item => Number(item.kid)); }
async function submitAddTemplate() {
  if (!templateAddForm.keywordIds.length) return proxy.$modal.msgWarning("请至少选择一个关键词");
  submitting.value = true;
  try {
    await addTemplate({ templateTitleId: templateAddForm.templateTitleId, keywordIds: templateAddForm.keywordIds, sceneDesc: templateAddForm.sceneDesc });
    proxy.$modal.msgSuccess("订阅模板添加成功");
    addDialog.value = false;
    loadTemplates();
  } finally { submitting.value = false; }
}
async function removeTemplate(templateId) {
  await proxy.$modal.confirm("确认删除该订阅模板吗？");
  await deleteTemplate(templateId);
  proxy.$modal.msgSuccess("删除成功");
  loadTemplates();
}
function openSend(row) {
  sendForm.templateId = row.priTmplId;
  selectedTemplate.title = row.title || "订阅消息模板";
  selectedTemplate.content = readableTemplateContent(row.content);
  sendData.value = extractTemplateFields(row.content);
  sendDialog.value = true;
}
function readableTemplateContent(content) {
  return String(content || "").replace(/\{\{\s*[a-zA-Z0-9_]+\.DATA\s*\}\}/g, "【待填写】");
}
function extractTemplateFields(content) {
  const fields = [];
  const seen = new Set();
  const pattern = /([^{}\r\n]*)\{\{\s*([a-zA-Z0-9_]+)\.DATA\s*\}\}/g;
  let match;
  while ((match = pattern.exec(String(content || ""))) !== null) {
    const name = match[2];
    if (seen.has(name)) continue;
    seen.add(name);
    const label = match[1].replace(/^[\s\-—·•]+/, "").replace(/[：:\s]+$/, "").trim() || `模板内容第 ${fields.length + 1} 项`;
    fields.push({ name, label, hint: templateFieldHint(name), value: "" });
  }
  return fields;
}
function templateFieldHint(name) {
  const type = name.replace(/\d+$/, "");
  return ({
    thing: "普通文本", number: "数字", letter: "字母", symbol: "符号",
    character_string: "数字、字母或符号组合", time: "时间", date: "日期",
    amount: "金额", phone_number: "手机号码", car_number: "车牌号码",
    name: "名称或姓名", phrase: "简短汉字内容",
  })[type] || "请按模板要求填写";
}
async function sendSubscribe() {
  if (!sendForm.toUser) return proxy.$modal.msgWarning("请填写接收人 OpenID");
  if (!sendData.value.length) return proxy.$modal.msgWarning("该模板没有可发送的内容项");
  const emptyField = sendData.value.find(item => !String(item.value || "").trim());
  if (emptyField) return proxy.$modal.msgWarning(`请填写“${emptyField.label}”`);
  const data = sendData.value.map(({ name, value }) => ({ name, value }));
  submitting.value = true;
  try {
    await sendSubscribeMessage({ ...sendForm, data });
    proxy.$modal.msgSuccess("订阅消息发送成功");
    sendDialog.value = false;
  } finally { submitting.value = false; }
}

loadTemplates();
loadCategories();
</script>

<style scoped>
.section-card { margin-top: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.field-list { width: 100%; }
.field-label { margin-bottom: 4px; font-weight: 600; }
.template-preview { margin-top: 10px; color: var(--el-text-color-secondary); font-size: 13px; white-space: pre-line; }
.template-preview span { color: var(--el-text-color-regular); }
</style>
