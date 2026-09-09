<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="设置渠道场景后生成参数二维码，用户扫码关注或进入公众号时可识别来源。" type="info" show-icon :closable="false" />
    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" header="生成设置">
          <el-form :model="qrcodeForm" label-width="120px" class="qrcode-form">
            <el-form-item label="二维码类型">
              <el-radio-group v-model="qrcodeForm.permanent" @change="clearResult">
                <el-radio-button :label="false">临时二维码</el-radio-button>
                <el-radio-button :label="true">永久二维码</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-alert class="type-tip" :title="qrcodeForm.permanent ? '长期固定投放使用，生成后永久有效。' : '短期活动使用，到期后二维码失效。'" type="success" :closable="false" show-icon />
            <el-form-item label="渠道场景值" required>
              <el-input v-model="qrcodeForm.scene" maxlength="64" show-word-limit placeholder="例如 store_001、campaign_autumn" />
              <div class="field-tip">建议使用可读的渠道编码，用户扫码事件会携带该值，便于统计来源。</div>
            </el-form-item>
            <el-form-item v-if="!qrcodeForm.permanent" label="有效时间">
              <el-input-number v-model="qrcodeForm.expireSeconds" :min="60" :max="2592000" :step="3600" />
              <span class="unit">秒（约 {{ expireDays }} 天）</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="generating" v-hasPermi="['wxmp:operations:edit']" @click="generateQrcode">生成二维码</el-button>
              <el-button v-if="qrcodeResult.imageUrl" @click="openOriginal">打开原图</el-button>
              <el-button v-if="qrcodeResult.imageUrl" @click="clearResult">重新填写</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" header="生成结果" class="preview-card">
          <div v-if="qrcodeResult.imageUrl" class="preview-content">
            <el-image :src="qrcodeResult.imageUrl" class="qrcode-image" :preview-src-list="[qrcodeResult.imageUrl]" />
            <el-descriptions :column="1" border class="result-block">
              <el-descriptions-item label="场景值">{{ qrcodeForm.scene }}</el-descriptions-item>
              <el-descriptions-item label="有效期">{{ qrcodeForm.permanent ? '永久有效' : `${qrcodeResult.expireSeconds || qrcodeForm.expireSeconds} 秒` }}</el-descriptions-item>
              <el-descriptions-item label="二维码凭证">
                <span class="ticket">{{ qrcodeResult.ticket }}</span>
                <el-button link type="primary" @click="copyText(qrcodeResult.ticket)">复制</el-button>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="完成左侧设置后生成二维码" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="WxMpQrcode">
import { computed, getCurrentInstance, reactive, ref } from "vue";
import { createQrcode } from "@/api/wxmp/wxoperations";

const { proxy } = getCurrentInstance();
const qrcodeForm = reactive({ scene: "channel_001", permanent: false, expireSeconds: 2592000 });
const qrcodeResult = reactive({});
const generating = ref(false);
const expireDays = computed(() => Math.max(1, Math.round(qrcodeForm.expireSeconds / 86400)));

function clearResult() { Object.keys(qrcodeResult).forEach(key => delete qrcodeResult[key]); }
async function generateQrcode() {
  if (!qrcodeForm.scene.trim()) return proxy.$modal.msgWarning("请填写渠道场景值");
  generating.value = true;
  try {
    const response = await createQrcode(qrcodeForm);
    clearResult();
    Object.assign(qrcodeResult, response.data || {});
    proxy.$modal.msgSuccess("参数二维码生成成功");
  } finally { generating.value = false; }
}
function openOriginal() {
  const link = document.createElement("a");
  link.href = qrcodeResult.imageUrl;
  link.target = "_blank";
  link.rel = "noopener";
  link.click();
}
async function copyText(value) {
  if (!value) return;
  try {
    await navigator.clipboard.writeText(value);
    proxy.$modal.msgSuccess("二维码凭证已复制");
  } catch {
    proxy.$modal.msgWarning("浏览器未允许自动复制，请手动选择二维码凭证复制");
  }
}
</script>

<style scoped>
.content-row { margin-top: 16px; }
.qrcode-form { max-width: 720px; }
.type-tip { margin: 0 0 18px; }
.field-tip { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.unit { margin-left: 8px; color: var(--el-text-color-secondary); }
.preview-card { min-height: 470px; }
.preview-content { display: flex; flex-direction: column; align-items: center; gap: 16px; }
.qrcode-image { width: 260px; height: 260px; }
.result-block { width: 100%; }
.ticket { display: inline-block; max-width: 220px; margin-right: 8px; overflow: hidden; text-overflow: ellipsis; vertical-align: middle; white-space: nowrap; }
</style>
