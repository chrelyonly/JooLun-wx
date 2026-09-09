<template>
  <div class="app-container" v-loading="loading">
    <el-alert
      title="后台保存的配置优先于 application.yml，并会立即应用。敏感字段不会回显，留空表示保持原值。"
      type="info"
      :closable="false"
      show-icon
      class="config-alert"
    />

    <el-form ref="configRef" :model="form" :rules="rules" label-width="170px">
      <el-card shadow="never" class="config-card">
        <template #header>
          <div class="card-title">微信公众号</div>
        </template>
        <el-form-item label="AppID" prop="mp.appId">
          <el-input v-model.trim="form.mp.appId" maxlength="64" placeholder="请输入微信公众号 AppID" />
        </el-form-item>
        <el-form-item label="AppSecret" prop="mp.secret">
          <el-input
            v-model="form.mp.secret"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.mp.secretConfigured, 'AppSecret')"
          />
        </el-form-item>
        <el-form-item label="Token" prop="mp.token">
          <el-input
            v-model="form.mp.token"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.mp.tokenConfigured, 'Token')"
          />
        </el-form-item>
        <el-form-item label="EncodingAESKey" prop="mp.aesKey">
          <el-input
            v-model="form.mp.aesKey"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.mp.aesKeyConfigured, 'EncodingAESKey')"
          />
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="config-card">
        <template #header>
          <div class="card-title">微信小程序与支付</div>
        </template>
        <el-form-item label="小程序 AppID" prop="ma.appId">
          <el-input v-model.trim="form.ma.appId" maxlength="64" placeholder="请输入微信小程序 AppID" />
        </el-form-item>
        <el-form-item label="小程序 AppSecret" prop="ma.secret">
          <el-input
            v-model="form.ma.secret"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.ma.secretConfigured, 'AppSecret')"
          />
        </el-form-item>
        <el-form-item label="小程序 Token" prop="ma.token">
          <el-input
            v-model="form.ma.token"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.ma.tokenConfigured, 'Token（消息推送可选）')"
          />
        </el-form-item>
        <el-form-item label="小程序 EncodingAESKey" prop="ma.aesKey">
          <el-input
            v-model="form.ma.aesKey"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.ma.aesKeyConfigured, 'EncodingAESKey（加密推送可选）')"
          />
        </el-form-item>
        <el-form-item label="小程序消息格式" prop="ma.msgDataFormat">
          <el-radio-group v-model="form.ma.msgDataFormat"><el-radio value="JSON">JSON</el-radio><el-radio value="XML">XML</el-radio></el-radio-group>
          <div class="form-tip">内容安全异步检测结果使用小程序消息推送，建议选择 JSON。</div>
        </el-form-item>
        <el-form-item label="微信支付商户号" prop="ma.mchId">
          <el-input v-model.trim="form.ma.mchId" maxlength="64" placeholder="请输入微信支付商户号" />
        </el-form-item>
        <el-divider content-position="left">微信支付 API v3</el-divider>
        <el-form-item label="API v3 密钥" prop="ma.apiV3Key">
          <el-input
            v-model="form.ma.apiV3Key"
            type="password"
            show-password
            maxlength="200"
            autocomplete="new-password"
            :placeholder="secretPlaceholder(form.ma.apiV3KeyConfigured, 'API v3 密钥')"
          />
        </el-form-item>
        <el-form-item label="商户 API 证书" prop="ma.pkcs12Path">
          <el-input v-model.trim="form.ma.pkcs12Path" maxlength="500" placeholder="apiclient_cert.p12 的绝对路径或 classpath: 路径" />
          <div class="form-tip">仅支持 PKCS#12 格式，证书密码使用上方填写的微信支付商户号。</div>
        </el-form-item>
        <el-form-item label="微信支付公钥 ID" prop="ma.publicKeyId">
          <el-input v-model.trim="form.ma.publicKeyId" maxlength="128" placeholder="请输入 PUB_KEY_ID_ 开头的微信支付公钥 ID" />
        </el-form-item>
        <el-form-item label="微信支付公钥路径" prop="ma.publicKeyPath">
          <el-input v-model.trim="form.ma.publicKeyPath" maxlength="500" placeholder="pub_key.pem 的绝对路径或 classpath: 路径" />
          <div class="form-tip">必须使用商户平台下载的微信支付公钥，不能填写商户 API 证书或商户私钥。</div>
        </el-form-item>
      </el-card>

      <div class="form-actions">
        <el-button type="primary" :loading="saving" @click="submitForm" v-hasPermi="['system:wxconfig:edit']">
          保存配置
        </el-button>
        <el-button :disabled="saving" @click="loadConfig">重新加载</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup name="WxConfig">
import { getWxAccountConfig, updateWxAccountConfig } from '@/api/system/appConfig'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const form = reactive(createEmptyForm())

const requiredSecret = (configuredField, label) => (_rule, value, callback) => {
  if (!value && !configuredField()) {
    callback(new Error(`${label}不能为空`))
    return
  }
  callback()
}

const rules = {
  'mp.appId': [{ required: true, message: '公众号 AppID 不能为空', trigger: 'blur' }],
  'mp.secret': [{ validator: requiredSecret(() => form.mp.secretConfigured, '公众号 AppSecret'), trigger: 'blur' }],
  'ma.appId': [{ required: true, message: '小程序 AppID 不能为空', trigger: 'blur' }],
  'ma.secret': [{ validator: requiredSecret(() => form.ma.secretConfigured, '小程序 AppSecret'), trigger: 'blur' }],
  'ma.mchId': [{ required: true, message: '微信支付商户号不能为空', trigger: 'blur' }],
  'ma.apiV3Key': [{ validator: requiredSecret(() => form.ma.apiV3KeyConfigured, 'API v3 密钥'), trigger: 'blur' }],
  'ma.pkcs12Path': [{ required: true, message: '商户 API 证书路径不能为空', trigger: 'blur' }],
  'ma.publicKeyId': [{ required: true, message: '微信支付公钥 ID 不能为空', trigger: 'blur' }],
  'ma.publicKeyPath': [{ required: true, message: '微信支付公钥路径不能为空', trigger: 'blur' }]
}

function createEmptyForm() {
  return {
    mp: {
      appId: '',
      secret: '',
      token: '',
      aesKey: '',
      secretConfigured: false,
      tokenConfigured: false,
      aesKeyConfigured: false
    },
    ma: {
      appId: '',
      secret: '',
      token: '',
      aesKey: '',
      msgDataFormat: 'JSON',
      mchId: '',
      apiV3Key: '',
      pkcs12Path: '',
      publicKeyId: '',
      publicKeyPath: '',
      secretConfigured: false,
      tokenConfigured: false,
      aesKeyConfigured: false,
      apiV3KeyConfigured: false
    }
  }
}

function secretPlaceholder(configured, label) {
  return configured ? `${label} 已配置，留空则不修改` : `请输入${label}`
}

function loadConfig() {
  loading.value = true
  getWxAccountConfig().then(response => {
    Object.assign(form.mp, createEmptyForm().mp, response.data.mp)
    Object.assign(form.ma, createEmptyForm().ma, response.data.ma)
    proxy.$refs.configRef?.clearValidate()
  }).finally(() => {
    loading.value = false
  })
}

function submitForm() {
  proxy.$refs.configRef.validate(valid => {
    if (!valid) return
    saving.value = true
    updateWxAccountConfig(form).then(() => {
      proxy.$modal.msgSuccess('微信账号配置保存成功')
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
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.config-card :deep(.el-input) {
  max-width: 620px;
}

.form-tip {
  width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 20px;
}

.form-actions {
  max-width: 920px;
  padding-left: 170px;
}
</style>
