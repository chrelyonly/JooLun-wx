<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="先选择检测类型并提交内容；文本立即返回结果，图片和音频提交后需等待微信异步回调。" type="info" show-icon :closable="false" />
    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="13">
        <el-card shadow="never">
          <el-tabs v-model="activeType" stretch>
            <el-tab-pane label="文本检测" name="text">
              <el-form :model="textSecurityForm" label-width="110px" class="security-form">
                <el-form-item label="用户 OpenID" required><el-input v-model="userOpenId" placeholder="产生该内容的小程序用户" /></el-form-item>
                <el-form-item label="业务场景"><el-select v-model="textSecurityForm.scene"><el-option v-for="item in scenes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
                <el-form-item label="检测内容" required><el-input v-model="textSecurityForm.content" type="textarea" :rows="6" maxlength="2500" show-word-limit /></el-form-item>
                <el-form-item label="内容标题"><el-input v-model="textSecurityForm.title" placeholder="选填，有助于提高识别准确率" /></el-form-item>
                <el-form-item label="用户昵称"><el-input v-model="textSecurityForm.nickname" placeholder="选填" /></el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="checking" v-hasPermi="['wxma:operations:edit']" @click="submitTextSecurity">开始检测</el-button>
                  <el-button @click="resetText">清空内容</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="图片/音频检测" name="media">
              <el-form :model="mediaSecurityForm" label-width="110px" class="security-form">
                <el-form-item label="用户 OpenID" required><el-input v-model="userOpenId" placeholder="产生该内容的小程序用户" /></el-form-item>
                <el-form-item label="媒体类型"><el-radio-group v-model="mediaSecurityForm.mediaType"><el-radio-button :label="2">图片</el-radio-button><el-radio-button :label="1">音频</el-radio-button></el-radio-group></el-form-item>
                <el-form-item label="业务场景"><el-select v-model="mediaSecurityForm.scene"><el-option v-for="item in scenes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
                <el-form-item label="媒体地址" required>
                  <el-input v-model="mediaSecurityForm.mediaUrl" placeholder="https://..." />
                  <div class="field-tip">必须是微信服务器可以直接访问的 HTTPS 地址。</div>
                </el-form-item>
                <el-form-item><el-button type="primary" :loading="checking" v-hasPermi="['wxma:operations:edit']" @click="submitMediaSecurity">提交异步检测</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="11">
        <el-card shadow="never" header="检测结果" class="result-card">
          <el-alert v-if="securityTraceId" :title="`异步任务已提交，任务编号：${securityTraceId}`" type="success" :closable="false" show-icon />
          <el-form v-if="activeType === 'media'" :inline="true" class="result-actions">
            <el-form-item label="任务编号"><el-input v-model="securityTraceId" placeholder="输入任务编号" style="width: 260px" /></el-form-item>
            <el-form-item><el-button :loading="querying" v-hasPermi="['wxma:operations:query']" @click="loadMediaResult">刷新结果</el-button></el-form-item>
          </el-form>
          <div v-loading="checking || querying" class="result-area"><api-data-view :value="securityResult" empty-text="提交检测后在此查看结果" /></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="history-card">
      <template #header><div class="card-header"><span>最近异步检测记录</span><el-button :loading="querying" v-hasPermi="['wxma:operations:query']" @click="loadRecentMediaResults">刷新记录</el-button></div></template>
      <api-data-view :value="recentResults" empty-text="点击刷新记录查看最近异步检测结果" />
    </el-card>
    <p class="callback-hint">异步结果接收地址：/weixin/api/ma/portal，请同时在微信公众平台配置该消息推送地址。</p>
  </div>
</template>

<script setup name="WxMaContentSecurity">
import { getCurrentInstance, reactive, ref } from "vue";
import ApiDataView from "@/components/ApiDataView/index.vue";
import { checkText, checkMedia, getRecentMediaResults, getMediaResult } from "@/api/wxma/wxoperations";

const { proxy } = getCurrentInstance();
const activeType = ref("text");
const userOpenId = ref("");
const textSecurityForm = reactive({ scene: 2, content: "", nickname: "", title: "", signature: "" });
const mediaSecurityForm = reactive({ mediaUrl: "", mediaType: 2, scene: 2 });
const securityResult = ref(null);
const recentResults = ref(null);
const securityTraceId = ref("");
const checking = ref(false);
const querying = ref(false);
const scenes = [{ value: 1, label: "用户资料" }, { value: 2, label: "评论" }, { value: 3, label: "论坛" }, { value: 4, label: "社交日志" }];

function requireValue(value, label) {
  if (!String(value || "").trim()) { proxy.$modal.msgWarning(`请填写${label}`); return false; }
  return true;
}
function resetText() {
  Object.assign(textSecurityForm, { scene: 2, content: "", nickname: "", title: "", signature: "" });
  securityResult.value = null;
}
async function submitTextSecurity() {
  if (!requireValue(userOpenId.value, "用户 OpenID") || !requireValue(textSecurityForm.content, "检测内容")) return;
  checking.value = true;
  try {
    securityResult.value = (await checkText({ ...textSecurityForm, openId: userOpenId.value })).data;
  } finally { checking.value = false; }
}
async function submitMediaSecurity() {
  if (!requireValue(userOpenId.value, "用户 OpenID") || !requireValue(mediaSecurityForm.mediaUrl, "媒体地址")) return;
  if (!/^https:\/\//i.test(mediaSecurityForm.mediaUrl)) return proxy.$modal.msgWarning("媒体地址必须以 https:// 开头");
  checking.value = true;
  try {
    securityResult.value = (await checkMedia({ ...mediaSecurityForm, openId: userOpenId.value })).data;
    securityTraceId.value = securityResult.value?.traceId || securityTraceId.value;
    proxy.$modal.msgSuccess("已提交，请稍后刷新检测结果");
  } finally { checking.value = false; }
}
async function loadMediaResult() {
  if (!requireValue(securityTraceId.value, "任务编号")) return;
  querying.value = true;
  try { securityResult.value = (await getMediaResult(securityTraceId.value)).data; }
  finally { querying.value = false; }
}
async function loadRecentMediaResults() {
  querying.value = true;
  try { recentResults.value = (await getRecentMediaResults(20)).data; }
  finally { querying.value = false; }
}
</script>

<style scoped>
.content-row { margin-top: 16px; }
.security-form { padding: 8px 10px 0; }
.field-tip { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.result-card { min-height: 475px; }
.result-actions { margin-top: 16px; }
.result-area { min-height: 220px; }
.history-card { margin-top: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.callback-hint { color: var(--el-text-color-secondary); font-size: 13px; }
</style>
