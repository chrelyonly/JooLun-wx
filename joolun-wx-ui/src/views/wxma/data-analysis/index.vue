<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="查询微信官方小程序数据分析接口，日期范围限制以具体指标接口为准。" type="info" show-icon :closable="false" />
    <el-form :inline="true" :model="analyticsForm" class="query-form">
      <el-form-item label="指标" class="metric-form-item">
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
      <el-form-item :label="dateLabel">
        <el-date-picker
          v-model="analyticsDate"
          :type="pickerType"
          value-format="YYYY-MM-DD"
          :placeholder="datePlaceholder"
          :disabled-date="disableUnavailableDate"
        />
      </el-form-item>
      <el-form-item v-if="metricPeriod === 'portrait'" label="画像周期">
        <el-select v-model="portraitDays" style="width: 120px" @change="analyticsResult = null">
          <el-option label="单日" :value="1" />
          <el-option label="最近 7 天" :value="7" />
          <el-option label="最近 30 天" :value="30" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" v-hasPermi="['wxma:operations:query']" @click="loadAnalytics">查询数据</el-button></el-form-item>
    </el-form>
    <el-alert class="date-hint" :title="`${dateHint} 本次查询范围：${resolvedRangeText}`" type="success" :closable="false" show-icon />
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

<script setup name="WxMaDataAnalysis">
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import ApiDataView from "@/components/ApiDataView/index.vue";
import AnalyticsChart from "@/components/AnalyticsChart/index.vue";
import { getAnalytics } from "@/api/wxma/wxoperations";

const { proxy } = getCurrentInstance();
const analyticsDate = ref(formatDate(addDays(new Date(), -1)));
const portraitDays = ref(1);
const analyticsForm = reactive({ metric: "dailySummary" });
const analyticsResult = ref(null);
const loading = ref(false);
let analyticsRequestId = 0;
const analyticsMetrics = [
  ["dailySummary", "每日概况"], ["dailyVisit", "每日访问趋势"], ["weeklyVisit", "每周访问趋势"],
  ["monthlyVisit", "每月访问趋势"], ["visitDistribution", "访问分布"], ["dailyRetain", "每日留存"],
  ["weeklyRetain", "每周留存"], ["monthlyRetain", "每月留存"], ["visitPage", "访问页面"], ["userPortrait", "用户画像"],
].map(([value, label]) => ({ value, label }));

const metricPeriods = {
  dailySummary: "day", dailyVisit: "day", visitDistribution: "day", dailyRetain: "day", visitPage: "day",
  weeklyVisit: "week", weeklyRetain: "week",
  monthlyVisit: "month", monthlyRetain: "month",
  userPortrait: "portrait",
};
const metricPeriod = computed(() => metricPeriods[analyticsForm.metric] || "day");
const selectedMetricLabel = computed(() => analyticsMetrics.find(item => item.value === analyticsForm.metric)?.label || "运营数据");
const pickerType = computed(() => metricPeriod.value === "month" ? "month" : "date");
const dateLabel = computed(() => ({ day: "统计日期", week: "所在自然周", month: "统计月份", portrait: "截止日期" }[metricPeriod.value]));
const datePlaceholder = computed(() => ({ day: "选择日期", week: "选择该周任意一天", month: "选择月份", portrait: "选择截止日期" }[metricPeriod.value]));
const dateHint = computed(() => ({
  day: "该指标仅支持查询单日数据，最晚可选择昨天。",
  week: "选择自然周中的任意一天，系统会自动按周一至周日查询；当前未结束的自然周不可查询。",
  month: "系统会自动按自然月第一天至最后一天查询；当前未结束的月份不可查询。",
  portrait: "用户画像只支持单日、最近 7 天或最近 30 天，最晚截止到昨天。",
}[metricPeriod.value]));
const resolvedRangeText = computed(() => {
  if (!analyticsDate.value) return "尚未选择";
  const [start, end] = resolveDateRange();
  return start.getTime() === end.getTime() ? formatDate(start) : `${formatDate(start)} 至 ${formatDate(end)}`;
});

function parseDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}
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
function disableUnavailableDate(value) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return value >= today;
}
function resolveDateRange() {
  const selected = parseDate(analyticsDate.value);
  if (metricPeriod.value === "week") {
    const weekday = selected.getDay() || 7;
    const start = addDays(selected, 1 - weekday);
    return [start, addDays(start, 6)];
  }
  if (metricPeriod.value === "month") {
    const start = new Date(selected.getFullYear(), selected.getMonth(), 1);
    return [start, new Date(selected.getFullYear(), selected.getMonth() + 1, 0)];
  }
  if (metricPeriod.value === "portrait") return [addDays(selected, 1 - portraitDays.value), selected];
  return [selected, selected];
}
function handleMetricChange() {
  const today = new Date();
  if (metricPeriod.value === "week") {
    const weekday = today.getDay() || 7;
    analyticsDate.value = formatDate(addDays(today, -weekday));
  } else if (metricPeriod.value === "month") {
    analyticsDate.value = formatDate(new Date(today.getFullYear(), today.getMonth(), 0));
  } else {
    analyticsDate.value = formatDate(addDays(today, -1));
  }
  analyticsResult.value = null;
}
function selectMetric(metric) {
  if (analyticsForm.metric === metric) return;
  analyticsForm.metric = metric;
  handleMetricChange();
  loadAnalytics();
}

async function loadAnalytics() {
  if (!analyticsDate.value) return proxy.$modal.msgWarning("请选择统计日期");
  const [startDate, endDate] = resolveDateRange();
  const yesterday = addDays(new Date(), -1);
  yesterday.setHours(0, 0, 0, 0);
  if (endDate > yesterday) return proxy.$modal.msgWarning(metricPeriod.value === "week" ? "请选择已经结束的自然周" : "请选择已经结束的自然月");
  const requestId = ++analyticsRequestId;
  loading.value = true;
  try {
    const result = (await getAnalytics({
      metric: analyticsForm.metric,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
    })).data;
    if (requestId === analyticsRequestId) analyticsResult.value = result;
  } finally {
    if (requestId === analyticsRequestId) loading.value = false;
  }
}

onMounted(loadAnalytics);
</script>

<style scoped>
.query-form { margin-top: 20px; }
.metric-form-item { display: flex; margin-right: 0; width: 100%; }
:deep(.metric-form-item .el-form-item__content) { flex: 1; }
.metric-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.metric-tag { cursor: pointer; user-select: none; }
.date-hint { margin-top: -6px; }
.result-area { min-height: 180px; }
.detail-card { margin-top: 16px; }
</style>
