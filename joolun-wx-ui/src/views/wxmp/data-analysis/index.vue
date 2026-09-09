<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="查询微信官方公众号用户、图文、消息和接口分析数据，系统会按指标限制日期范围。" type="info" show-icon :closable="false" />
    <el-form :inline="true" :model="analyticsForm" class="query-form">
      <el-form-item label="统计指标" class="metric-form-item">
        <div class="metric-tags">
          <el-check-tag
            v-for="item in analyticsMetrics"
            :key="item.value"
            :checked="analyticsForm.metric === item.value"
            class="metric-tag"
            @change="selectMetric(item.value)"
          >{{ item.label }}</el-check-tag>
        </div>
      </el-form-item>
      <el-form-item label="统计日期">
        <el-date-picker
          v-if="maxDays === 1"
          v-model="analyticsDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          :disabled-date="disableUnavailableDate"
        />
        <el-date-picker
          v-else
          v-model="analyticsDates"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :disabled-date="disableUnavailableDate"
        />
      </el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" v-hasPermi="['wxmp:operations:query']" @click="loadAnalytics">查询数据</el-button></el-form-item>
    </el-form>
    <el-alert class="range-tip" :title="rangeHint" type="success" :closable="false" show-icon />
    <div v-loading="loading" class="result-area">
      <analytics-chart
        :value="analyticsResult"
        :title="selectedMetricLabel"
        :metric="analyticsForm.metric"
      />
      <el-card shadow="never" class="detail-card">
        <template #header><span>数据明细</span></template>
        <api-data-view :value="analyticsResult" empty-text="选择指标和日期后查询" />
      </el-card>
    </div>
  </div>
</template>

<script setup name="WxMpDataAnalysis">
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import ApiDataView from "@/components/ApiDataView/index.vue";
import AnalyticsChart from "@/components/AnalyticsChart/index.vue";
import { getAnalytics } from "@/api/wxmp/wxoperations";

const { proxy } = getCurrentInstance();
const analyticsDate = ref(formatDate(addDays(new Date(), -1)));
const analyticsDates = ref([]);
const analyticsForm = reactive({ metric: "userSummary" });
const analyticsResult = ref(null);
const loading = ref(false);
let analyticsRequestId = 0;
const analyticsMetrics = [
  ["userSummary", "用户增减"], ["userCumulate", "累计用户"], ["articleSummary", "图文群发每日"],
  ["articleTotal", "图文群发总览"], ["userRead", "图文阅读"], ["userReadHour", "图文阅读分时"],
  ["userShare", "图文分享"], ["userShareHour", "图文分享分时"], ["upstreamMsg", "用户消息发送"],
  ["upstreamMsgHour", "用户消息发送分时"], ["upstreamMsgWeek", "用户消息周数据"],
  ["upstreamMsgMonth", "用户消息月数据"], ["upstreamMsgDist", "用户消息分布"],
  ["interfaceSummary", "接口调用分析"], ["interfaceSummaryHour", "接口调用分时"],
].map(([value, label]) => ({ value, label }));

const metricMaxDays = {
  articleSummary: 1, articleTotal: 1, userReadHour: 1, userShareHour: 1,
  upstreamMsgHour: 1, interfaceSummaryHour: 1,
  userRead: 3,
  userSummary: 7, userCumulate: 7, userShare: 7, upstreamMsg: 7,
  upstreamMsgWeek: 30, upstreamMsgMonth: 30, upstreamMsgDist: 15, interfaceSummary: 30,
};
const maxDays = computed(() => metricMaxDays[analyticsForm.metric] || 1);
const selectedMetricLabel = computed(() => analyticsMetrics.find(item => item.value === analyticsForm.metric)?.label || "运营数据");
const rangeHint = computed(() => maxDays.value === 1
  ? "该指标每次只能查询一天，最晚可查询到昨天。"
  : `该指标最多查询连续 ${maxDays.value} 天，最晚可查询到昨天；已为你预选最近 ${maxDays.value} 天。`);

function formatDate(value) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
function addDays(value, days) {
  const result = new Date(value);
  result.setDate(result.getDate() + days);
  return result;
}
function parseDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}
function disableUnavailableDate(value) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return value >= today;
}
function handleMetricChange() {
  const yesterday = addDays(new Date(), -1);
  analyticsDate.value = formatDate(yesterday);
  analyticsDates.value = maxDays.value === 1 ? [] : [formatDate(addDays(yesterday, 1 - maxDays.value)), formatDate(yesterday)];
  analyticsResult.value = null;
}
function selectMetric(metric) {
  if (analyticsForm.metric === metric) return;
  analyticsForm.metric = metric;
  handleMetricChange();
  loadAnalytics();
}
async function loadAnalytics() {
  let startDate;
  let endDate;
  if (maxDays.value === 1) {
    if (!analyticsDate.value) return proxy.$modal.msgWarning("请选择统计日期");
    startDate = analyticsDate.value;
    endDate = analyticsDate.value;
  } else {
    if (analyticsDates.value.length !== 2) return proxy.$modal.msgWarning("请选择统计日期范围");
    [startDate, endDate] = analyticsDates.value;
    const days = Math.round((parseDate(endDate) - parseDate(startDate)) / 86400000) + 1;
    if (days > maxDays.value) return proxy.$modal.msgWarning(`当前指标最多查询连续 ${maxDays.value} 天`);
  }
  const requestId = ++analyticsRequestId;
  loading.value = true;
  try {
    const result = (await getAnalytics({ metric: analyticsForm.metric, startDate, endDate })).data;
    if (requestId === analyticsRequestId) analyticsResult.value = result;
  } finally {
    if (requestId === analyticsRequestId) loading.value = false;
  }
}

handleMetricChange();
onMounted(loadAnalytics);
</script>

<style scoped>
.query-form { margin-top: 20px; }
.metric-form-item { display: flex; margin-right: 0; width: 100%; }
:deep(.metric-form-item .el-form-item__content) { flex: 1; }
.metric-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.metric-tag { cursor: pointer; user-select: none; }
.range-tip { margin-top: -6px; }
.result-area { min-height: 180px; }
.detail-card { margin-top: 16px; }
</style>
