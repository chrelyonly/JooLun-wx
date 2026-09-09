<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="填写投放场景和落地页面，生成后可预览并下载，用于渠道推广或线下入口。" type="info" show-icon :closable="false" />
    <el-row :gutter="20" class="content-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" header="生成设置">
          <el-form :model="qrcodeForm" label-width="120px" class="qrcode-form">
            <el-form-item label="码类型">
              <el-radio-group v-model="qrcodeForm.type" @change="clearResult">
                <el-radio-button label="UNLIMIT">无限制小程序码</el-radio-button>
                <el-radio-button label="WXA">普通小程序码</el-radio-button>
                <el-radio-button label="QR">普通二维码</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-alert class="type-tip" :title="typeTip" type="success" :closable="false" show-icon />
            <el-form-item v-if="qrcodeForm.type === 'UNLIMIT'" label="渠道场景值" required>
              <el-input v-model="qrcodeForm.scene" maxlength="32" show-word-limit placeholder="例如 channel=store_001" />
            </el-form-item>
            <el-form-item label="落地页面" required>
              <el-input v-model="qrcodeForm.path" placeholder="pages/index/index">
                <template #prepend>/</template>
              </el-input>
              <div class="field-tip">必须填写所选版本中真实存在的小程序页面路径，不要以“/”开头；无限制码的业务参数请填写在渠道场景值中。</div>
            </el-form-item>
            <template v-if="qrcodeForm.type !== 'QR'">
              <el-form-item label="小程序版本">
                <el-select v-model="qrcodeForm.envVersion">
                  <el-option label="正式版" value="release" />
                  <el-option label="体验版" value="trial" />
                  <el-option label="开发版" value="develop" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="qrcodeForm.type === 'UNLIMIT'" label="校验页面路径"><el-switch v-model="qrcodeForm.checkPath" /></el-form-item>
              <el-form-item label="透明底色"><el-switch v-model="qrcodeForm.hyaline" /></el-form-item>
            </template>
            <el-form-item label="图片宽度"><el-input-number v-model="qrcodeForm.width" :min="280" :max="1280" :step="10" /><span class="unit">像素</span></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="generating" v-hasPermi="['wxma:operations:edit']" @click="generateQrcode">生成预览</el-button>
              <el-button :disabled="!qrcodeUrl" @click="downloadQrcode">下载 PNG</el-button>
              <el-button v-if="qrcodeUrl" @click="clearResult">重新填写</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" header="生成结果" class="preview-card">
          <div v-if="qrcodeUrl" class="preview-content">
            <el-image :src="qrcodeUrl" class="qrcode-image" :preview-src-list="[qrcodeUrl]" fit="contain" />
            <el-tag type="success">生成成功，可点击图片放大查看</el-tag>
          </div>
          <el-alert
            v-else-if="generationError"
            title="生成失败"
            :description="generationError"
            type="error"
            show-icon
            :closable="false"
          />
          <el-empty v-else description="完成左侧设置后生成预览" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="WxMaQrcode">
import { computed, getCurrentInstance, onBeforeUnmount, reactive, ref } from "vue";
import { createQrcode } from "@/api/wxma/wxoperations";

const { proxy } = getCurrentInstance();
const qrcodeForm = reactive({ type: "UNLIMIT", path: "", scene: "channel=001", width: 430, envVersion: "release", checkPath: true, hyaline: false });
const qrcodeUrl = ref("");
const qrcodeBlob = ref(null);
const generating = ref(false);
const generationError = ref("");
const typeTip = computed(() => ({
  UNLIMIT: "推荐用于渠道投放：数量不受限制，扫码后可获得渠道场景值。",
  WXA: "适合固定页面入口：页面路径可携带参数，但生成数量有限制。",
  QR: "二维码样式，适合需要兼容普通扫码场景的固定页面入口。",
}[qrcodeForm.type]));

function clearResult() {
  if (qrcodeUrl.value) URL.revokeObjectURL(qrcodeUrl.value);
  qrcodeUrl.value = "";
  qrcodeBlob.value = null;
  generationError.value = "";
}
async function generateQrcode() {
  if (qrcodeForm.type === "UNLIMIT" && !qrcodeForm.scene.trim()) return proxy.$modal.msgWarning("请填写渠道场景值");
  if (!qrcodeForm.path.trim()) return proxy.$modal.msgWarning("请填写落地页面");
  if (qrcodeForm.path.startsWith("/")) return proxy.$modal.msgWarning("落地页面不能以“/”开头");
  generating.value = true;
  generationError.value = "";
  try {
    const image = await createQrcode(qrcodeForm);
    clearResult();
    qrcodeBlob.value = image;
    qrcodeUrl.value = URL.createObjectURL(image);
    proxy.$modal.msgSuccess("小程序码生成成功");
  } catch (error) {
    clearResult();
    generationError.value = readableError(error);
    proxy.$modal.msgError(generationError.value);
  } finally { generating.value = false; }
}
function readableError(error) {
  const message = error?.message || "小程序码生成失败，请稍后重试";
  if (message.includes("41030") || message.toLowerCase().includes("invalid page")) {
    return "落地页面无效：请填写所选小程序版本中真实存在的页面路径，并确认路径没有以“/”开头。开发版或体验版页面可核对版本后再试。";
  }
  return message;
}
function downloadQrcode() {
  if (!qrcodeBlob.value) return;
  const link = document.createElement("a");
  link.href = qrcodeUrl.value;
  link.download = `小程序码-${qrcodeForm.scene || "页面"}-${Date.now()}.png`;
  link.click();
}

onBeforeUnmount(clearResult);
</script>

<style scoped>
.content-row { margin-top: 16px; }
.qrcode-form { max-width: 720px; }
.type-tip { margin: 0 0 18px; }
.field-tip { margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.5; }
.unit { margin-left: 8px; color: var(--el-text-color-secondary); }
.preview-card { min-height: 510px; }
.preview-content { display: flex; flex-direction: column; align-items: center; gap: 16px; padding-top: 24px; }
.qrcode-image { width: 320px; height: 320px; }
</style>
