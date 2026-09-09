<template>
  <div class="app-container" v-loading="loading">
    <el-alert
      title="后台保存的配置优先于 application.yml，并会立即用于支付和退款回调地址。"
      type="info"
      :closable="false"
      show-icon
      class="config-alert"
    />

    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="card-title">商城回调配置</div>
      </template>
      <el-form ref="configRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item label="后台外网访问域名" prop="notifyHost">
          <el-input v-model.trim="form.notifyHost" maxlength="500" placeholder="例如：https://admin.example.com/prod-api" />
          <div class="form-tip">请填写后台服务可被微信访问的完整域名及部署前缀，末尾无需添加 /。</div>
        </el-form-item>
        <el-form-item label="支付回调地址">
          <el-text>{{ callbackUrl('/weixin/api/ma/orderinfo/notify-order') }}</el-text>
        </el-form-item>
        <el-form-item label="退款回调地址">
          <el-text>{{ callbackUrl('/weixin/api/ma/orderinfo/notify-refunds') }}</el-text>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['system:mallconfig:edit']">
            保存配置
          </el-button>
          <el-button :disabled="saving" @click="loadConfig">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="MallConfig">
import { getMallConfig, updateMallConfig } from '@/api/system/appConfig'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  notifyHost: ''
})
const rules = {
  notifyHost: [
    { required: true, message: '后台外网访问域名不能为空', trigger: 'blur' },
    { pattern: /^https?:\/\/\S+$/, message: '请输入以 http:// 或 https:// 开头的地址', trigger: 'blur' }
  ]
}

function callbackUrl(path) {
  const host = form.notifyHost.replace(/\/+$/, '')
  return host ? host + path : '请先填写后台外网访问域名'
}

function loadConfig() {
  loading.value = true
  getMallConfig().then(response => {
    form.notifyHost = response.data.notifyHost || ''
    proxy.$refs.configRef?.clearValidate()
  }).finally(() => {
    loading.value = false
  })
}

function submitForm() {
  proxy.$refs.configRef.validate(valid => {
    if (!valid) return
    saving.value = true
    updateMallConfig(form).then(() => {
      proxy.$modal.msgSuccess('商城配置保存成功')
      loadConfig()
    }).finally(() => {
      saving.value = false
    })
  })
}

loadConfig()
</script>

<style scoped>
.config-alert {
  margin-bottom: 16px;
}

.config-card {
  max-width: 920px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.config-card :deep(.el-input) {
  max-width: 650px;
}

.form-tip {
  width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 20px;
}
</style>
