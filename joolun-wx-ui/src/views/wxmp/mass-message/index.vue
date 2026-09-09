<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="选择接收范围和消息类型后提交群发；图文、图片、语音和视频可直接从公众号素材库中选择。" type="info" show-icon :closable="false" />

    <el-card shadow="never" class="section-card">
      <template #header><span>创建群发任务</span></template>
      <el-form :model="sendForm" label-width="120px" class="send-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="发送方式"><el-select v-model="sendForm.action" @change="resetRecipient"><el-option label="按标签群发" value="tag" /><el-option label="按 OpenID 群发" value="openids" /><el-option label="发送预览" value="preview" /></el-select></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="消息类型"><el-select v-model="sendForm.msgType" @change="handleMessageTypeChange"><el-option label="文本" value="text" /><el-option label="图文" value="mpnews" /><el-option label="图片" value="image" /><el-option label="语音" value="voice" /><el-option label="视频" value="mpvideo" /></el-select></el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="防重复编号"><el-input v-model="sendForm.clientMsgId" placeholder="选填"><template #append><el-button @click="generateClientId">生成</el-button></template></el-input></el-form-item></el-col>
        </el-row>

        <template v-if="sendForm.action === 'tag'">
          <el-form-item label="全部用户"><el-switch v-model="sendForm.sendAll" /></el-form-item>
          <el-form-item v-if="!sendForm.sendAll" label="接收标签" required>
            <el-select v-model="sendForm.tagId" filterable placeholder="请选择用户标签" style="width: 280px" :loading="tagLoading">
              <el-option v-for="item in tags" :key="item.id" :label="`${item.name}${item.count != null ? `（${item.count}人）` : ''}`" :value="Number(item.id)" />
            </el-select>
            <el-button class="refresh-button" :loading="tagLoading" @click="loadTags">刷新标签</el-button>
          </el-form-item>
        </template>
        <el-form-item v-else-if="sendForm.action === 'openids'" label="用户 OpenID" required>
          <el-input v-model="sendForm.openIds" type="textarea" :rows="5" placeholder="每行填写一个 OpenID，也支持逗号分隔" />
          <div class="field-tip">已识别 {{ openIdCount }} 个接收人</div>
        </el-form-item>
        <template v-else>
          <el-form-item label="预览接收方式"><el-radio-group v-model="sendForm.previewTarget"><el-radio label="openid">OpenID</el-radio><el-radio label="wxname">微信号</el-radio></el-radio-group></el-form-item>
          <el-form-item :label="sendForm.previewTarget === 'openid' ? '接收人 OpenID' : '接收人微信号'" required>
            <wx-user-select v-if="sendForm.previewTarget === 'openid'" v-model="sendForm.previewReceiver" app-type="2" />
            <el-input v-else v-model="sendForm.previewReceiver" />
          </el-form-item>
        </template>

        <el-form-item v-if="sendForm.msgType === 'text'" label="文本内容" required><el-input v-model="sendForm.content" type="textarea" :rows="5" maxlength="600" show-word-limit /></el-form-item>
        <el-form-item v-else label="发送素材" required>
          <div class="material-field">
            <el-input v-model.trim="sendForm.mediaId" clearable placeholder="请选择素材，也可以直接粘贴 Media ID" @input="handleMediaIdInput">
              <template #append><el-button icon="FolderOpened" @click="openMaterialDialog">从素材库选择</el-button></template>
            </el-input>
            <div v-if="selectedMaterial" class="selected-material">
              <el-tag type="success" effect="plain">已选择{{ materialTypeLabel }}</el-tag>
              <span class="material-name">{{ selectedMaterial.name }}</span>
              <span v-if="selectedMaterial.updateTime" class="material-time">更新于 {{ formatMaterialTime(selectedMaterial.updateTime) }}</span>
            </div>
            <div v-else class="field-tip">选择后系统会自动填写素材编号，无需再到素材管理页面复制。</div>
          </div>
        </el-form-item>
        <el-form-item v-if="sendForm.msgType === 'mpnews' && sendForm.action !== 'preview'" label="忽略转载校验"><el-switch v-model="sendForm.sendIgnoreReprint" /></el-form-item>
        <el-alert class="send-summary" :title="recipientSummary" :type="sendForm.action === 'preview' ? 'success' : 'warning'" show-icon :closable="false" />
        <el-form-item><el-button type="primary" :loading="submitting" v-hasPermi="['wxmp:operations:send']" @click="submitMass">{{ sendForm.action === 'preview' ? '发送预览' : '确认并提交群发' }}</el-button><el-button @click="resetSendForm">清空内容</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header><span>任务与速度管理</span></template>
      <el-form :inline="true" :model="taskForm">
        <el-form-item label="消息 ID"><el-input v-model="taskForm.msgId" placeholder="群发返回的消息 ID" /></el-form-item>
        <el-form-item label="文章序号"><el-input-number v-model="taskForm.articleIndex" :min="1" placeholder="选填" /></el-form-item>
        <el-form-item>
          <el-button :loading="taskLoading" v-hasPermi="['wxmp:operations:query']" @click="queryMassStatus">查询状态</el-button>
          <el-button type="danger" v-hasPermi="['wxmp:operations:edit']" @click="removeMass">删除群发</el-button>
        </el-form-item>
      </el-form>
      <el-form :inline="true" :model="taskForm">
        <el-form-item label="群发速度"><el-slider v-model="taskForm.speed" :min="0" :max="4" :marks="speedMarks" show-stops style="width: 300px" /></el-form-item>
        <el-form-item>
          <el-button v-hasPermi="['wxmp:operations:query']" @click="queryMassSpeed">查询当前速度</el-button>
          <el-button type="primary" v-hasPermi="['wxmp:operations:edit']" @click="saveMassSpeed">保存速度</el-button>
        </el-form-item>
      </el-form>
      <api-data-view :value="massResult" empty-text="发送或查询后在此展示任务结果" />
    </el-card>

    <el-dialog
      v-model="materialDialogVisible"
      :title="`选择${materialTypeLabel}`"
      width="80%"
      append-to-body
      destroy-on-close
    >
      <el-alert
        v-if="sendForm.msgType === 'mpnews'"
        title="请选择公众号草稿箱中的图文内容，选中后会自动使用该草稿的 Media ID。"
        type="info"
        show-icon
        :closable="false"
        class="material-dialog-tip"
      />
      <wx-material-select
        v-if="materialDialogVisible"
        :key="materialPickerType"
        :obj-data="{ repType: materialPickerType }"
        :news-type="sendForm.msgType === 'mpnews' ? '2' : '1'"
        @select-material="selectMaterial"
      />
      <template #footer><el-button @click="materialDialogVisible = false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="WxMpMassMessage">
import { computed, getCurrentInstance, reactive, ref } from "vue";
import ApiDataView from "@/components/ApiDataView/index.vue";
import WxUserSelect from "@/components/WxUserSelect/index.vue";
import WxMaterialSelect from "@/components/wx-material-select/main.vue";
import { getList as getUserTags } from "@/api/wxmp/wxusertags";
import {
  sendMassByTag, sendMassByOpenIds, previewMass, getMassStatus,
  deleteMass, getMassSpeed, updateMassSpeed,
} from "@/api/wxmp/wxoperations";

const { proxy } = getCurrentInstance();
const submitting = ref(false);
const taskLoading = ref(false);
const tagLoading = ref(false);
const tags = ref([]);
const massResult = ref(null);
const materialDialogVisible = ref(false);
const selectedMaterial = ref(null);
const sendForm = reactive({
  action: "tag", msgType: "mpnews", tagId: undefined, sendAll: false, openIds: "",
  previewTarget: "openid", previewReceiver: "", content: "", mediaId: "",
  sendIgnoreReprint: false, clientMsgId: "",
});
const taskForm = reactive({ msgId: "", articleIndex: undefined, speed: 0 });
const speedMarks = { 0: "慢", 2: "中", 4: "快" };
const openIdCount = computed(() => splitOpenIds(sendForm.openIds).length);
const materialPickerType = computed(() => ({ mpnews: "news", mpvideo: "video" })[sendForm.msgType] || sendForm.msgType);
const materialTypeLabel = computed(() => ({ mpnews: "图文草稿", image: "图片素材", voice: "语音素材", mpvideo: "视频素材" })[sendForm.msgType] || "素材");
const recipientSummary = computed(() => {
  if (sendForm.action === "preview") return "预览只发送给一个测试接收人，不会发起正式群发。";
  if (sendForm.action === "openids") return `本次将向 ${openIdCount.value} 个指定用户提交群发。`;
  if (sendForm.sendAll) return "本次将向全部关注用户提交群发，请在发送前核对内容。";
  const tag = tags.value.find(item => Number(item.id) === Number(sendForm.tagId));
  return tag ? `本次将向“${tag.name}”标签用户提交群发。` : "请选择接收标签。";
});

function resetRecipient() {
  sendForm.openIds = "";
  sendForm.previewReceiver = "";
}
function handleMessageTypeChange() {
  sendForm.content = "";
  sendForm.mediaId = "";
  selectedMaterial.value = null;
}
function openMaterialDialog() { materialDialogVisible.value = true; }
function materialName(item) {
  const articles = item?.content?.articles || item?.content?.newsItem || [];
  return item?.name || item?.title || articles[0]?.title || "未命名素材";
}
function selectMaterial(item) {
  const mediaId = item?.mediaId || item?.articleId;
  if (!mediaId) return proxy.$modal.msgWarning("该素材没有可用的 Media ID，请选择其他素材");
  sendForm.mediaId = mediaId;
  selectedMaterial.value = { name: materialName(item), updateTime: item.updateTime };
  materialDialogVisible.value = false;
  proxy.$modal.msgSuccess(`${materialTypeLabel.value}已选择`);
}
function handleMediaIdInput() { selectedMaterial.value = null; }
function formatMaterialTime(value) {
  if (!value) return "";
  const date = /^\d+$/.test(String(value)) ? new Date(Number(value) * (String(value).length <= 10 ? 1000 : 1)) : new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("zh-CN", { hour12: false });
}
function splitOpenIds(value) { return value.split(/[\n,，;；]+/).map(item => item.trim()).filter(Boolean); }
function generateClientId() { sendForm.clientMsgId = `joolun-${Date.now()}`; }
async function loadTags() {
  tagLoading.value = true;
  try { tags.value = (await getUserTags()).data || []; }
  finally { tagLoading.value = false; }
}
function resetSendForm() {
  Object.assign(sendForm, { content: "", mediaId: "", openIds: "", previewReceiver: "", sendIgnoreReprint: false });
  selectedMaterial.value = null;
  massResult.value = null;
}
function commonMessage() {
  const result = { msgType: sendForm.msgType, clientMsgId: sendForm.clientMsgId || undefined };
  if (sendForm.msgType === "text") result.content = sendForm.content;
  else result.mediaId = sendForm.mediaId;
  return result;
}
async function submitMass() {
  if (sendForm.msgType === "text" && !sendForm.content) return proxy.$modal.msgWarning("请填写文本内容");
  if (sendForm.msgType !== "text" && !sendForm.mediaId) return proxy.$modal.msgWarning("请填写素材 Media ID");
  let api;
  let body = commonMessage();
  if (sendForm.action === "tag") {
    if (!sendForm.sendAll && !tags.value.some(item => Number(item.id) === Number(sendForm.tagId))) return proxy.$modal.msgWarning("请选择有效的接收标签");
    api = sendMassByTag;
    body = { ...body, tagId: sendForm.sendAll ? null : sendForm.tagId, sendAll: sendForm.sendAll, sendIgnoreReprint: sendForm.sendIgnoreReprint };
  } else if (sendForm.action === "openids") {
    const toUsers = splitOpenIds(sendForm.openIds);
    if (!toUsers.length) return proxy.$modal.msgWarning("请填写接收人 OpenID");
    api = sendMassByOpenIds;
    body = { ...body, toUsers, sendIgnoreReprint: sendForm.sendIgnoreReprint };
  } else {
    if (!sendForm.previewReceiver) return proxy.$modal.msgWarning("请填写预览接收人");
    api = previewMass;
    delete body.clientMsgId;
    body = {
      ...body,
      toWxUserOpenid: sendForm.previewTarget === "openid" ? sendForm.previewReceiver : undefined,
      toWxUserName: sendForm.previewTarget === "wxname" ? sendForm.previewReceiver : undefined,
    };
  }
  if (sendForm.action !== "preview") await proxy.$modal.confirm(`${recipientSummary.value} 群发提交后无法撤回，确认继续吗？`);
  submitting.value = true;
  try {
    massResult.value = (await api(body)).data;
    const msgId = massResult.value?.msgId || massResult.value?.msg_id;
    if (msgId) taskForm.msgId = String(msgId);
    proxy.$modal.msgSuccess(sendForm.action === "preview" ? "预览发送成功" : "群发请求已提交，可在下方查询状态");
  } finally { submitting.value = false; }
}
async function queryMassStatus() {
  if (!taskForm.msgId) return proxy.$modal.msgWarning("请填写消息 ID");
  taskLoading.value = true;
  try { massResult.value = (await getMassStatus(taskForm.msgId)).data; }
  finally { taskLoading.value = false; }
}
async function removeMass() {
  if (!taskForm.msgId) return proxy.$modal.msgWarning("请填写消息 ID");
  await proxy.$modal.confirm("确认删除该群发任务吗？");
  massResult.value = (await deleteMass(taskForm.msgId, taskForm.articleIndex)).data;
  proxy.$modal.msgSuccess("群发任务已删除");
}
async function queryMassSpeed() { massResult.value = (await getMassSpeed()).data; }
async function saveMassSpeed() {
  massResult.value = (await updateMassSpeed(taskForm.speed)).data;
  proxy.$modal.msgSuccess("群发速度已设置");
}

loadTags();
</script>

<style scoped>
.section-card { margin-top: 16px; }
.send-form { max-width: 1100px; }
.refresh-button { margin-left: 8px; }
.field-tip { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.material-field { width: min(720px, 100%); }
.selected-material { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.material-name { color: var(--el-text-color-primary); font-weight: 500; }
.material-time { color: var(--el-text-color-secondary); font-size: 12px; }
.material-dialog-tip { margin-bottom: 16px; }
.send-summary { margin: 0 0 18px 120px; width: calc(100% - 120px); }
</style>
