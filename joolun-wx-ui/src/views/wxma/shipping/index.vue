<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="商城后台发货会自动同步；此页面用于能力检查、补偿上传、订单查询和提醒收货。" type="success" show-icon :closable="false" />

    <el-row :gutter="16" class="status-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <div class="capability-status">
            <span class="capability-title">发货信息管理能力</span>
            <el-tag size="large" :type="shippingEnabled === true ? 'success' : shippingEnabled === false ? 'danger' : 'info'">
              {{ shippingEnabled === true ? '已开通' : shippingEnabled === false ? '未开通' : '待查询' }}
            </el-tag>
          </div>
          <el-button class="status-button" :loading="capabilityLoading" v-hasPermi="['wxma:operations:query']" @click="checkShippingEnabled">刷新状态</el-button>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header><span>消息跳转路径</span></template>
          <el-form :inline="true">
            <el-form-item label="小程序页面"><el-input v-model="jumpPath" placeholder="pages/order/detail" style="width: 360px" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="savingPath" v-hasPermi="['wxma:operations:edit']" @click="saveJumpPath">保存路径</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-header">
          <span>发货信息操作</span>
          <el-radio-group v-model="action" @change="shippingResult = null">
            <el-radio-button label="upload">补偿上传</el-radio-button>
            <el-radio-button label="query">查询订单</el-radio-button>
            <el-radio-button label="list">订单列表</el-radio-button>
            <el-radio-button label="notify">提醒收货</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-form v-if="action === 'upload'" :model="uploadForm" label-width="130px" class="operation-form">
        <el-row :gutter="18">
          <el-col :span="8"><el-form-item label="订单号类型"><el-select v-model="uploadForm.orderNumberType"><el-option label="商户订单号" :value="1" /><el-option label="微信支付单号" :value="2" /></el-select></el-form-item></el-col>
          <el-col v-if="uploadForm.orderNumberType === 2" :span="16"><el-form-item label="微信支付单号" required><el-input v-model="uploadForm.transactionId" /></el-form-item></el-col>
          <template v-else>
            <el-col :span="8"><el-form-item label="商户号" required><el-input v-model="uploadForm.mchId" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="商户订单号" required><el-input v-model="uploadForm.outTradeNo" /></el-form-item></el-col>
          </template>
          <el-col :span="8"><el-form-item label="物流模式"><el-select v-model="uploadForm.logisticsType"><el-option label="实体物流" :value="1" /><el-option label="同城配送" :value="2" /><el-option label="虚拟商品" :value="3" /><el-option label="用户自提" :value="4" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="发货模式"><el-select v-model="uploadForm.deliveryMode"><el-option label="统一发货" :value="1" /><el-option label="分拆发货" :value="2" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="全部发货"><el-switch v-model="uploadForm.isAllDelivered" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="支付人 OpenID" required><el-input v-model="uploadForm.openid" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="发货时间" required><el-date-picker v-model="uploadForm.uploadTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col>
        </el-row>
        <el-alert class="operation-tip" :title="uploadTip" type="success" :closable="false" show-icon />
        <el-divider content-position="left">发货商品明细</el-divider>
        <el-table :data="shippingItems" border>
          <el-table-column v-if="requiresTracking" label="快递公司编码" min-width="160"><template #default="scope"><el-input v-model="scope.row.expressCompany" placeholder="例如 SF" /></template></el-table-column>
          <el-table-column v-if="requiresTracking" label="运单号" min-width="180"><template #default="scope"><el-input v-model="scope.row.trackingNo" /></template></el-table-column>
          <el-table-column label="商品描述" min-width="180"><template #default="scope"><el-input v-model="scope.row.itemDesc" /></template></el-table-column>
          <el-table-column label="寄件人联系方式" min-width="180"><template #default="scope"><el-input v-model="scope.row.consignorContact" placeholder="支持掩码" /></template></el-table-column>
          <el-table-column label="收件人联系方式" min-width="180"><template #default="scope"><el-input v-model="scope.row.receiverContact" placeholder="支持掩码" /></template></el-table-column>
          <el-table-column width="70" align="center"><template #default="scope"><el-button link type="danger" @click="removeShippingItem(scope.$index)">删除</el-button></template></el-table-column>
        </el-table>
        <el-button class="add-item" @click="addShippingItem">新增物流明细</el-button>
      </el-form>

      <el-form v-else-if="action === 'query'" :model="orderForm" label-width="130px" class="operation-form">
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="微信支付单号"><el-input v-model="orderForm.transactionId" placeholder="与商户订单号二选一" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商户订单号"><el-input v-model="orderForm.merchantTradeNo" placeholder="与微信支付单号二选一" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商户号"><el-input v-model="orderForm.merchantId" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="子商户号"><el-input v-model="orderForm.subMerchantId" placeholder="选填" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <el-form v-else-if="action === 'list'" :model="listForm" label-width="120px" class="operation-form">
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="支付时间"><el-date-picker v-model="listForm.payTimeRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="订单状态"><el-select v-model="listForm.orderState" clearable><el-option label="待发货" :value="1" /><el-option label="已发货" :value="2" /><el-option label="确认收货" :value="3" /><el-option label="交易完成" :value="4" /><el-option label="已退款" :value="5" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="每页数量"><el-input-number v-model="listForm.pageSize" :min="1" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="支付人 OpenID"><el-input v-model="listForm.openId" placeholder="选填" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="翻页游标"><el-input v-model="listForm.lastIndex" placeholder="查询下一页时填写" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <el-form v-else :model="notifyForm" label-width="130px" class="operation-form">
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="微信支付单号"><el-input v-model="notifyForm.transactionId" placeholder="与商户订单号二选一" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商户订单号"><el-input v-model="notifyForm.merchantTradeNo" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="商户号"><el-input v-model="notifyForm.merchantId" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="子商户号"><el-input v-model="notifyForm.subMerchantId" placeholder="选填" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="收货时间"><el-date-picker v-model="notifyForm.receivedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col>
        </el-row>
      </el-form>

      <div class="action-buttons">
        <el-button type="primary" :loading="submitting" :disabled="!canExecute" @click="submitAction">{{ actionButtonText }}</el-button>
        <el-button @click="resetCurrentAction">重置当前表单</el-button>
        <el-button v-if="action === 'list' && hasNextPage" type="success" :loading="submitting" @click="queryNextPage">查询下一页</el-button>
      </div>
      <el-card shadow="never" class="result-card" header="操作结果">
        <api-data-view :value="shippingResult" empty-text="执行操作后在此展示订单和物流结果" />
      </el-card>
    </el-card>
  </div>
</template>

<script setup name="WxMaShipping">
import { computed, getCurrentInstance, reactive, ref } from "vue";
import ApiDataView from "@/components/ApiDataView/index.vue";
import {
  getShippingEnabled, uploadShipping, queryShipping, getShippingList, notifyConfirm, setShippingJumpPath,
} from "@/api/wxma/wxoperations";
import useUserStore from "@/store/modules/user";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const action = ref("upload");
const submitting = ref(false);
const capabilityLoading = ref(false);
const savingPath = ref(false);
const shippingEnabled = ref(null);
const jumpPath = ref("");
const shippingResult = ref(null);
const uploadForm = reactive({ orderNumberType: 2, transactionId: "", mchId: "", outTradeNo: "", logisticsType: 1, deliveryMode: 1, isAllDelivered: true, openid: "", uploadTime: formatDateTime(new Date()) });
const shippingItems = ref([{ trackingNo: "", expressCompany: "", itemDesc: "商品", consignorContact: "", receiverContact: "" }]);
const orderForm = reactive({ transactionId: "", merchantId: "", subMerchantId: "", merchantTradeNo: "" });
const listForm = reactive({ payTimeRange: [formatDateTime(new Date(Date.now() - 7 * 86400000)), formatDateTime(new Date())], orderState: undefined, openId: "", lastIndex: "", pageSize: 20 });
const notifyForm = reactive({ transactionId: "", merchantId: "", subMerchantId: "", merchantTradeNo: "", receivedTime: "" });

const actionButtonText = computed(() => ({ upload: "上传发货信息", query: "查询订单", list: "查询订单列表", notify: "发送收货提醒" }[action.value]));
const requiresTracking = computed(() => uploadForm.logisticsType === 1);
const uploadTip = computed(() => requiresTracking.value ? "实体快递需要填写快递公司编码和运单号；顺丰还需填写寄件人或收件人联系方式。" : "当前发货方式不需要填写快递公司和运单号，只需填写商品描述。" );
const hasNextPage = computed(() => Boolean(shippingResult.value?.hasMore && shippingResult.value?.lastIndex));
const canExecute = computed(() => {
  const permission = ["query", "list"].includes(action.value) ? "wxma:operations:query" : "wxma:operations:edit";
  return userStore.permissions.includes("*:*:*") || userStore.permissions.includes(permission);
});

function toTimestamp(value) { return value ? Math.floor(new Date(value.replace(" ", "T")).getTime() / 1000) : undefined; }
function toRfc3339(value) { return value ? new Date(value.replace(" ", "T")).toISOString() : undefined; }
function pad(value) { return String(value).padStart(2, "0"); }
function formatDateTime(value) { return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`; }
function addShippingItem() {
  if (shippingItems.value.length >= 10) return proxy.$modal.msgWarning("物流明细最多添加 10 条");
  shippingItems.value.push({ trackingNo: "", expressCompany: "", itemDesc: "", consignorContact: "", receiverContact: "" });
}
function removeShippingItem(index) {
  if (shippingItems.value.length === 1) return proxy.$modal.msgWarning("至少保留一条物流明细");
  shippingItems.value.splice(index, 1);
}
async function checkShippingEnabled() {
  capabilityLoading.value = true;
  try {
    const response = await getShippingEnabled();
    const value = response.data;
    shippingEnabled.value = typeof value === "boolean" ? value : Boolean(value?.isTradeManaged ?? value?.enabled);
  } finally { capabilityLoading.value = false; }
}
async function saveJumpPath() {
  if (!jumpPath.value) return proxy.$modal.msgWarning("请填写消息跳转路径");
  if (jumpPath.value.startsWith("/")) return proxy.$modal.msgWarning("小程序页面路径不能以“/”开头");
  savingPath.value = true;
  try { await setShippingJumpPath(jumpPath.value); proxy.$modal.msgSuccess("跳转路径已保存"); }
  finally { savingPath.value = false; }
}
function buildUploadRequest() {
  const orderKey = { orderNumberType: uploadForm.orderNumberType };
  if (uploadForm.orderNumberType === 2) orderKey.transactionId = uploadForm.transactionId;
  else Object.assign(orderKey, { mchId: uploadForm.mchId, outTradeNo: uploadForm.outTradeNo });
  return {
    orderKey,
    logisticsType: uploadForm.logisticsType,
    deliveryMode: uploadForm.deliveryMode,
    isAllDelivered: uploadForm.isAllDelivered,
    uploadTime: toRfc3339(uploadForm.uploadTime),
    payer: { openid: uploadForm.openid },
    shippingList: shippingItems.value.map(item => ({
      trackingNo: requiresTracking.value ? item.trackingNo : undefined,
      expressCompany: requiresTracking.value ? item.expressCompany : undefined,
      itemDesc: item.itemDesc,
      contact: item.consignorContact || item.receiverContact ? { consignorContact: item.consignorContact, receiverContact: item.receiverContact } : undefined,
    })),
  };
}
async function submitAction() {
  let api;
  let body;
  if (action.value === "upload") {
    if (uploadForm.orderNumberType === 2 && !uploadForm.transactionId) return proxy.$modal.msgWarning("请填写微信支付单号");
    if (uploadForm.orderNumberType === 1 && (!uploadForm.mchId || !uploadForm.outTradeNo)) return proxy.$modal.msgWarning("请填写商户号和商户订单号");
    if (!uploadForm.openid || !uploadForm.uploadTime) return proxy.$modal.msgWarning("请填写支付人 OpenID 和发货时间");
    if (shippingItems.value.some(item => !item.itemDesc)) return proxy.$modal.msgWarning("请填写每条发货明细的商品描述");
    if (requiresTracking.value && shippingItems.value.some(item => !item.expressCompany || !item.trackingNo)) return proxy.$modal.msgWarning("请填写每条物流明细的快递公司编码和运单号");
    api = uploadShipping; body = buildUploadRequest();
  } else if (action.value === "query") {
    if (!orderForm.transactionId && !orderForm.merchantTradeNo) return proxy.$modal.msgWarning("请填写微信支付单号或商户订单号");
    if (!orderForm.transactionId && orderForm.merchantTradeNo && !orderForm.merchantId) return proxy.$modal.msgWarning("使用商户订单号查询时，请同时填写商户号");
    api = queryShipping; body = { ...orderForm };
  } else if (action.value === "list") {
    api = getShippingList;
    body = {
      payTimeRange: listForm.payTimeRange?.length === 2 ? { beginTime: toTimestamp(listForm.payTimeRange[0]), endTime: toTimestamp(listForm.payTimeRange[1]) } : undefined,
      orderState: listForm.orderState, openId: listForm.openId || undefined,
      lastIndex: listForm.lastIndex || undefined, pageSize: listForm.pageSize,
    };
  } else {
    if (!notifyForm.transactionId && !notifyForm.merchantTradeNo) return proxy.$modal.msgWarning("请填写微信支付单号或商户订单号");
    if (!notifyForm.transactionId && notifyForm.merchantTradeNo && !notifyForm.merchantId) return proxy.$modal.msgWarning("使用商户订单号提醒收货时，请同时填写商户号");
    api = notifyConfirm;
    body = { ...notifyForm, receivedTime: toTimestamp(notifyForm.receivedTime) };
  }
  if (["upload", "notify"].includes(action.value)) await proxy.$modal.confirm(`确认${actionButtonText.value}吗？请先核对订单信息。`);
  submitting.value = true;
  try {
    shippingResult.value = (await api(body)).data;
    proxy.$modal.msgSuccess(`${actionButtonText.value}成功`);
  } finally { submitting.value = false; }
}
function resetCurrentAction() {
  if (action.value === "upload") {
    Object.assign(uploadForm, { orderNumberType: 2, transactionId: "", mchId: "", outTradeNo: "", logisticsType: 1, deliveryMode: 1, isAllDelivered: true, openid: "", uploadTime: formatDateTime(new Date()) });
    shippingItems.value = [{ trackingNo: "", expressCompany: "", itemDesc: "商品", consignorContact: "", receiverContact: "" }];
  } else if (action.value === "query") Object.assign(orderForm, { transactionId: "", merchantId: "", subMerchantId: "", merchantTradeNo: "" });
  else if (action.value === "list") Object.assign(listForm, { payTimeRange: [formatDateTime(new Date(Date.now() - 7 * 86400000)), formatDateTime(new Date())], orderState: undefined, openId: "", lastIndex: "", pageSize: 20 });
  else Object.assign(notifyForm, { transactionId: "", merchantId: "", subMerchantId: "", merchantTradeNo: "", receivedTime: "" });
  shippingResult.value = null;
}
function queryNextPage() { listForm.lastIndex = shippingResult.value.lastIndex; submitAction(); }

checkShippingEnabled();
</script>

<style scoped>
.status-row, .section-card { margin-top: 16px; }
.capability-status { display: flex; align-items: center; justify-content: space-between; min-height: 40px; }
.capability-title { color: var(--el-text-color-regular); }
.status-button { margin-top: 12px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.operation-form { max-width: 1180px; }
.operation-tip { margin-bottom: 16px; }
.add-item { margin: 10px 0 18px; }
.action-buttons { margin-top: 6px; }
.result-card { margin-top: 18px; }
</style>
