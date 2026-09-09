<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="使用系统中唯一的公众号配置，选择模板后即可填写字段并发送。" type="info" show-icon :closable="false" />
    <div class="toolbar">
      <el-button type="primary" v-hasPermi="['wxmp:operations:query']" @click="loadTemplates">刷新模板</el-button>
      <el-button v-hasPermi="['wxmp:operations:edit']" @click="templateDialog = true">添加模板</el-button>
    </div>
    <el-table v-loading="loading" :data="templates" border stripe>
      <el-table-column prop="title" label="模板标题" min-width="180" />
      <el-table-column prop="templateId" label="模板 ID" min-width="290" show-overflow-tooltip />
      <el-table-column prop="primaryIndustry" label="一级行业" width="140" />
      <el-table-column prop="deputyIndustry" label="二级行业" width="140" />
      <el-table-column label="模板内容" min-width="260" show-overflow-tooltip>
        <template #default="scope">{{ readableTemplateContent(scope.row.content) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" v-hasPermi="['wxmp:operations:send']" @click="openSend(scope.row)">发送</el-button>
          <el-button link type="danger" v-hasPermi="['wxmp:operations:edit']" @click="removeTemplate(scope.row.templateId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-alert v-if="lastMessageId" class="result-alert" type="success" show-icon :closable="false" :title="`发送成功，消息 ID：${lastMessageId}`" />

    <el-dialog v-model="templateDialog" title="添加公众号模板" width="520px">
      <el-form :model="templateAddForm" label-width="120px">
        <el-form-item label="模板库短 ID" required><el-input v-model="templateAddForm.shortTemplateId" placeholder="例如 TM00015" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="templateAddForm.keywords" placeholder="多个关键词用英文逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAddTemplate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sendDialog" title="发送公众号模板消息" width="840px">
      <el-form :model="sendForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="接收人 OpenID" required><wx-user-select v-model="sendForm.toUser" app-type="2" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所选模板"><el-input :model-value="selectedTemplate.title" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="跳转链接"><el-input v-model="sendForm.url" placeholder="https://..." /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户端消息 ID"><el-input v-model="sendForm.clientMsgId" placeholder="选填，用于防重复" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="跳转小程序"><el-switch v-model="useMiniProgram" /></el-form-item>
        <el-row v-if="useMiniProgram" :gutter="16">
          <el-col :span="12"><el-form-item label="小程序 AppID"><el-input v-model="miniProgram.appid" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="页面路径"><el-input v-model="miniProgram.pagePath" placeholder="pages/index/index" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="模板内容">
          <div class="field-list">
            <el-alert v-if="!templateData.length" title="该模板没有解析到可填写的内容项，请刷新模板后重试。" type="warning" :closable="false" show-icon />
            <el-table v-else :data="templateData" border>
              <el-table-column prop="label" label="内容项" width="210" />
              <el-table-column label="填写内容"><template #default="scope"><el-input v-model="scope.row.value" :placeholder="`请输入${scope.row.label}`" /></template></el-table-column>
              <el-table-column label="文字颜色" width="120" align="center"><template #default="scope"><el-color-picker v-model="scope.row.color" /></template></el-table-column>
            </el-table>
            <div class="template-preview">消息展示结构：<span>{{ selectedTemplate.content }}</span></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTemplate">发送消息</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WxMpTemplateMessage">
import { getCurrentInstance, reactive, ref } from "vue";
import WxUserSelect from "@/components/WxUserSelect/index.vue";
import { getTemplates, addTemplate, deleteTemplate, sendTemplate } from "@/api/wxmp/wxoperations";

const { proxy } = getCurrentInstance();
const templates = ref([]);
const loading = ref(false);
const submitting = ref(false);
const templateDialog = ref(false);
const sendDialog = ref(false);
const useMiniProgram = ref(false);
const lastMessageId = ref("");
const templateAddForm = reactive({ shortTemplateId: "", keywords: "" });
const sendForm = reactive({ toUser: "", templateId: "", url: "", clientMsgId: "" });
const miniProgram = reactive({ appid: "", pagePath: "" });
const selectedTemplate = reactive({ title: "", content: "" });
const templateData = ref([]);

async function loadTemplates() {
  loading.value = true;
  try {
    const response = await getTemplates();
    templates.value = Array.isArray(response.data) ? response.data : [];
  } finally { loading.value = false; }
}
async function submitAddTemplate() {
  if (!templateAddForm.shortTemplateId) return proxy.$modal.msgWarning("请填写模板库短 ID");
  submitting.value = true;
  try {
    await addTemplate({ shortTemplateId: templateAddForm.shortTemplateId, keywordNameList: templateAddForm.keywords.split(",").map(item => item.trim()).filter(Boolean) });
    templateDialog.value = false;
    proxy.$modal.msgSuccess("模板添加成功");
    loadTemplates();
  } finally { submitting.value = false; }
}
async function removeTemplate(templateId) {
  await proxy.$modal.confirm("确认删除该公众号模板吗？");
  await deleteTemplate(templateId);
  proxy.$modal.msgSuccess("删除成功");
  loadTemplates();
}
function openSend(row) {
  Object.assign(sendForm, { toUser: "", templateId: row.templateId, url: "", clientMsgId: "" });
  Object.assign(miniProgram, { appid: "", pagePath: "" });
  Object.assign(selectedTemplate, { title: row.title || "公众号模板", content: readableTemplateContent(row.content) });
  templateData.value = extractTemplateFields(row.content);
  useMiniProgram.value = false;
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
    const rawLabel = match[1].replace(/^[\s\-—·•]+/, "").replace(/[：:\s]+$/, "").trim();
    const keywordIndex = name.match(/\d+$/)?.[0];
    const fallback = name === "first" ? "开头内容" : name === "remark" ? "备注内容" : `模板内容${keywordIndex ? ` ${keywordIndex}` : ` ${fields.length + 1}`}`;
    fields.push({ name, label: rawLabel || fallback, value: "", color: "#173177" });
  }
  return fields;
}
async function submitTemplate() {
  if (!sendForm.toUser) return proxy.$modal.msgWarning("请填写接收人 OpenID");
  if (!templateData.value.length) return proxy.$modal.msgWarning("该模板没有可发送的内容项");
  const emptyField = templateData.value.find(item => !String(item.value || "").trim());
  if (emptyField) return proxy.$modal.msgWarning(`请填写“${emptyField.label}”`);
  if (useMiniProgram.value && (!miniProgram.appid || !miniProgram.pagePath)) return proxy.$modal.msgWarning("请填写小程序 AppID 和页面路径");
  const data = templateData.value.map(({ name, value, color }) => ({ name, value, color }));
  submitting.value = true;
  try {
    const response = await sendTemplate({
      ...sendForm,
      miniProgram: useMiniProgram.value ? { ...miniProgram } : null,
      data,
    });
    lastMessageId.value = response.data || "已提交";
    proxy.$modal.msgSuccess("模板消息发送成功");
    sendDialog.value = false;
  } finally { submitting.value = false; }
}

loadTemplates();
</script>

<style scoped>
.toolbar { margin: 16px 0; }
.result-alert { margin-top: 16px; }
.field-list { width: 100%; }
.template-preview { margin-top: 10px; color: var(--el-text-color-secondary); font-size: 13px; white-space: pre-line; }
.template-preview span { color: var(--el-text-color-regular); }
</style>
