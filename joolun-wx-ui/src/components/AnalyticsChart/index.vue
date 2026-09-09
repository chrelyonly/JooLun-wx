<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <el-card v-if="charts.length" shadow="never" class="chart-card">
    <template #header>
      <div class="chart-header">
        <div>
          <div class="chart-heading">{{ title }}图表</div>
          <div class="chart-subtitle">鼠标移入查看具体数值，完整数据保留在下方明细中。</div>
        </div>
        <el-radio-group v-model="displayType" size="small">
          <el-radio-button label="auto">自动</el-radio-button>
          <el-radio-button label="line">折线图</el-radio-button>
          <el-radio-button label="bar">柱状图</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <el-row :gutter="16">
      <el-col v-for="chart in charts" :key="chart.key" :xs="24" :xl="charts.length === 1 ? 24 : 12">
        <section class="chart-panel">
          <div v-if="charts.length > 1" class="panel-title">{{ chart.title }}</div>
          <div :ref="element => setChartElement(element, chart.key)" class="chart-canvas" />
        </section>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup name="AnalyticsChart">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";

const props = defineProps({
  value: { default: null },
  title: { type: String, default: "运营数据" },
  metric: { type: String, default: "" },
});

const displayType = ref("auto");
const chartElements = new Map();
const chartInstances = new Map();
const charts = computed(() => buildCharts(parseJsonValue(props.value)).slice(0, 12));

const fieldLabels = {
  refDate: "日期", refHour: "小时", statDate: "统计日期", date: "日期",
  sessionCnt: "打开次数", visitPv: "访问次数", visitUv: "访问人数", visitUvNew: "新增用户",
  stayTimeUv: "人均停留时长", stayTimeSession: "次均停留时长", visitDepth: "平均访问深度",
  visitTotal: "累计用户数", sharePv: "转发次数", shareUv: "转发人数",
  pagePath: "页面路径", pageVisitPv: "页面访问次数", pageVisitUv: "页面访问人数",
  pageStayTimePv: "页面次均停留时长", entryPagePv: "入口页次数", exitPagePv: "退出页次数",
  pageSharePv: "页面转发次数", pageShareUv: "页面转发人数",
  province: "省份分布", city: "城市分布", genders: "性别分布", platforms: "终端平台分布",
  devices: "设备型号分布", ages: "年龄分布", list: "访问分布", details: "每日明细",
  access_source_session_cnt: "访问来源", access_staytime_info: "访问时长", access_depth_info: "访问深度",
  newUser: "新增关注", cancelUser: "取消关注", cumulateUser: "累计用户",
  intPageReadUser: "图文阅读人数", intPageReadCount: "图文阅读次数",
  intPageFromSessionReadUser: "会话阅读人数", intPageFromSessionReadCount: "会话阅读次数",
  intPageFromHistMsgReadUser: "历史消息阅读人数", intPageFromHistMsgReadCount: "历史消息阅读次数",
  intPageFromFeedReadUser: "朋友圈阅读人数", intPageFromFeedReadCount: "朋友圈阅读次数",
  intPageFromFriendsReadUser: "好友转发阅读人数", intPageFromFriendsReadCount: "好友转发阅读次数",
  intPageFromOtherReadUser: "其他场景阅读人数", intPageFromOtherReadCount: "其他场景阅读次数",
  intPageFromKanyikanReadUser: "看一看阅读人数", intPageFromKanyikanReadCount: "看一看阅读次数",
  intPageFromSouyisouReadUser: "搜一搜阅读人数", intPageFromSouyisouReadCount: "搜一搜阅读次数",
  oriPageReadUser: "原文阅读人数", oriPageReadCount: "原文阅读次数",
  shareUser: "分享人数", shareCount: "分享次数", addToFavUser: "收藏人数", addToFavCount: "收藏次数",
  feedShareFromSessionUser: "会话分享人数", feedShareFromSessionCnt: "会话分享次数",
  feedShareFromFeedUser: "朋友圈分享人数", feedShareFromFeedCnt: "朋友圈分享次数",
  feedShareFromOtherUser: "其他场景分享人数", feedShareFromOtherCnt: "其他场景分享次数",
  msgUser: "消息用户数", msgCount: "消息数", targetUser: "送达人数",
  callbackCount: "接口调用次数", failCount: "接口失败次数", totalTimeCost: "接口总耗时", maxTimeCost: "接口最大耗时",
  countInterval: "消息数量区间", msgType: "消息类型", userSource: "用户来源", shareScene: "分享场景",
  title: "文章标题", value: "数量",
};
const categoryKeys = ["refHour", "pagePath", "title", "statDate", "refDate", "date", "countInterval", "msgType", "userSource", "shareScene", "name", "key", "label"];
const identifierKeys = new Set(["msgId", "msgDataId", "id", "index", "userSource", "shareScene", "msgType", "countInterval"]);

function isObject(value) { return value !== null && typeof value === "object" && !Array.isArray(value); }
function parseJsonValue(value) {
  if (typeof value !== "string" || !["{", "["].includes(value.trim().charAt(0))) return value;
  try { return JSON.parse(value); } catch { return value; }
}
function isNumeric(value) { return value !== "" && value != null && Number.isFinite(Number(value)); }
function isNumericMap(value) {
  const entries = isObject(value) ? Object.entries(value) : [];
  return entries.length > 0 && entries.every(([, item]) => isNumeric(item));
}
function flatten(value, target = {}) {
  if (!isObject(value)) return target;
  Object.entries(value).forEach(([key, item]) => {
    if (isObject(item) && !isNumericMap(item)) flatten(item, target);
    else if (!Array.isArray(item) && !isObject(item)) target[key] = item;
  });
  return target;
}
function labelFor(key) { return fieldLabels[key] || "其他指标"; }
function pathTitle(path, contextTitle) {
  const labels = path.filter(part => typeof part === "string" && !/^\d+$/.test(part) && part !== "root").map(labelFor);
  const unique = [...new Set(labels.filter(label => label !== "其他指标"))];
  return [contextTitle, ...unique].filter(Boolean).join(" · ") || props.title;
}
function formatDateLabel(value) {
  const text = String(value ?? "");
  if (/^\d{8}$/.test(text)) return `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6)}`;
  return text;
}
function formatCategory(key, value) {
  const text = String(value ?? "-");
  if (["refDate", "statDate", "date"].includes(key)) return formatDateLabel(text);
  if (key === "refHour") return `${String(text).padStart(4, "0").slice(0, 2)}:00`;
  if (["visitUvNew", "visitUv"].includes(key) && /^\d+$/.test(text)) {
    const unit = props.metric.includes("weekly") ? "周" : props.metric.includes("monthly") ? "月" : "天";
    const current = { 天: "当天", 周: "当周", 月: "当月" }[unit];
    return text === "0" ? current : `${text}${unit}后`;
  }
  if (key === "access_source_session_cnt") return ({
    1: "历史列表", 2: "搜索", 3: "会话", 4: "二维码", 5: "公众号主页", 6: "聊天顶部",
    7: "系统桌面", 8: "小程序主页", 9: "附近小程序", 10: "其他", 11: "模板消息",
    12: "客服消息", 13: "公众号菜单", 14: "App 分享", 15: "支付完成页", 16: "长按识别二维码",
    17: "相册二维码", 18: "公众号文章", 19: "钱包", 20: "卡包", 21: "小程序卡券",
    22: "其他小程序", 23: "其他小程序返回", 24: "卡券门店列表", 25: "搜索快捷入口",
    26: "小程序客服", 27: "公众号下发",
  })[text] || `来源 ${text}`;
  if (key === "access_staytime_info") return ({
    1: "0–2 秒", 2: "3–5 秒", 3: "6–10 秒", 4: "11–20 秒",
    5: "20–30 秒", 6: "30–50 秒", 7: "50–100 秒", 8: "100 秒以上",
  })[text] || text;
  if (key === "access_depth_info") return ({
    1: "1 页", 2: "2 页", 3: "3 页", 4: "4 页", 5: "5 页", 6: "6–10 页", 7: "10 页以上",
  })[text] || text;
  if (key === "countInterval") return ({ 0: "0 条", 1: "1–5 条", 2: "6–10 条", 3: "10 条以上" })[text] || text;
  if (key === "msgType") return ({ 1: "文字", 2: "图片", 3: "语音", 4: "视频", 6: "链接" })[text] || text;
  if (key === "userSource") return ({ 0: "全部用户", 1: "公众号搜索", 17: "名片分享", 30: "扫描二维码", 43: "图文页右上角菜单", 51: "支付后关注", 57: "图文页内公众号名称", 75: "其他" })[text] || `来源 ${text}`;
  if (key === "shareScene") return ({ 1: "好友", 2: "朋友圈", 3: "腾讯微博", 255: "其他" })[text] || text;
  return text;
}
function chooseDimensions(rows) {
  const available = categoryKeys.filter(key => rows.some(row => row[key] != null && row[key] !== ""));
  if (!available.length) return [];
  const varying = available.filter(key => new Set(rows.map(row => String(row[key] ?? ""))).size > 1);
  const dimensions = [];
  if (varying.includes("refDate") || varying.includes("statDate") || varying.includes("date")) {
    dimensions.push(varying.find(key => ["refDate", "statDate", "date"].includes(key)));
  }
  if (varying.includes("refHour")) dimensions.push("refHour");
  const categorical = varying.find(key => !["refDate", "statDate", "date", "refHour"].includes(key));
  if (categorical && (dimensions.length === 0 || rows.length !== new Set(rows.map(row => dimensions.map(key => row[key]).join("|"))).size)) dimensions.push(categorical);
  return dimensions.length ? dimensions : [available[0]];
}
function buildRowsModel(value, path, contextTitle) {
  const rows = value.filter(isObject).map(item => flatten(item));
  if (!rows.length) return null;
  const dimensions = chooseDimensions(rows);
  const numericKeys = [...new Set(rows.flatMap(row => Object.keys(row)))]
    .filter(key => !dimensions.includes(key) && !identifierKeys.has(key) && rows.some(row => isNumeric(row[key])))
    .slice(0, 8);
  if (!numericKeys.length) return null;
  const categories = rows.map((row, index) => dimensions.length
    ? dimensions.map(key => formatCategory(key, row[key])).join(" / ")
    : `第 ${index + 1} 项`);
  const timeSeries = dimensions.some(key => ["refDate", "statDate", "date", "refHour"].includes(key)) && categories.length > 1;
  return {
    key: `rows-${path.join("-") || "root"}`,
    title: pathTitle(path, contextTitle),
    categories,
    series: numericKeys.map(key => ({ name: labelFor(key), data: rows.map(row => isNumeric(row[key]) ? Number(row[key]) : null) })),
    preferredType: timeSeries ? "line" : "bar",
  };
}
function buildMapModel(value, path, contextTitle) {
  return {
    key: `map-${path.join("-")}`,
    title: pathTitle(path, contextTitle),
    categories: Object.keys(value).map(key => formatCategory(path[path.length - 1], key)),
    series: [{ name: pathTitle(path, contextTitle), data: Object.values(value).map(Number) }],
    preferredType: "bar",
  };
}
function buildScalarModel(value, path, contextTitle) {
  const entries = Object.entries(value).filter(([key, item]) => !identifierKeys.has(key) && isNumeric(item));
  if (!entries.length) return null;
  return {
    key: `scalar-${path.join("-") || "root"}`,
    title: pathTitle(path, contextTitle),
    categories: entries.map(([key]) => labelFor(key)),
    series: [{ name: "数值", data: entries.map(([, item]) => Number(item)) }],
    preferredType: "bar",
  };
}
function buildCharts(value) {
  const result = [];
  function walk(item, path = ["root"], contextTitle = "", allowScalar = true) {
    if (Array.isArray(item)) {
      const model = buildRowsModel(item, path, contextTitle);
      if (model) result.push(model);
      item.forEach((row, index) => {
        if (!isObject(row)) return;
        Object.entries(row).forEach(([key, child]) => {
          if (Array.isArray(child) || isObject(child)) walk(child, [...path, String(index), key], row.title || contextTitle, false);
        });
      });
      return;
    }
    if (!isObject(item)) return;
    if (isNumericMap(item)) {
      result.push(buildMapModel(item, path, contextTitle));
      return;
    }
    if (allowScalar) {
      const scalar = buildScalarModel(item, path, contextTitle);
      if (scalar && scalar.categories.length > 1) result.push(scalar);
    }
    Object.entries(item).forEach(([key, child]) => {
      if (Array.isArray(child) || isObject(child)) walk(child, [...path, key], contextTitle, true);
    });
  }
  walk(value);
  return result.filter((chart, index, list) => list.findIndex(item => item.key === chart.key) === index);
}
function setChartElement(element, key) {
  if (element) chartElements.set(key, element);
}
function optionFor(model) {
  const type = displayType.value === "auto" ? model.preferredType : displayType.value;
  const values = model.series.flatMap(series => series.data).filter(value => Number.isFinite(value));
  const allIntegers = values.length > 0 && values.every(Number.isInteger);
  return {
    color: ["#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#9B7EDE", "#36CFC9", "#FF85C0"],
    animationDuration: 500,
    tooltip: { trigger: "axis", confine: true },
    legend: { type: "scroll", top: 4 },
    grid: { top: model.series.length > 1 ? 58 : 38, left: 24, right: 24, bottom: model.categories.length > 8 ? 70 : 42, containLabel: true },
    toolbox: { right: 8, feature: { saveAsImage: { title: "保存图片", name: model.title } } },
    xAxis: { type: "category", data: model.categories, axisLabel: { interval: 0, rotate: model.categories.length > 6 ? 28 : 0, hideOverlap: true } },
    yAxis: { type: "value", minInterval: allIntegers ? 1 : undefined, splitLine: { lineStyle: { type: "dashed" } } },
    dataZoom: model.categories.length > 12 ? [{ type: "inside" }, { type: "slider", height: 18, bottom: 8 }] : [],
    series: model.series.map(series => ({
      ...series,
      type,
      smooth: type === "line",
      symbolSize: 7,
      barMaxWidth: 42,
      emphasis: { focus: "series" },
    })),
  };
}
async function renderCharts() {
  await nextTick();
  const currentKeys = new Set(charts.value.map(chart => chart.key));
  chartInstances.forEach((instance, key) => {
    if (!currentKeys.has(key)) {
      instance.dispose();
      chartInstances.delete(key);
      chartElements.delete(key);
    }
  });
  charts.value.forEach(model => {
    const element = chartElements.get(model.key);
    if (!element) return;
    const instance = echarts.getInstanceByDom(element) || echarts.init(element);
    chartInstances.set(model.key, instance);
    instance.setOption(optionFor(model), true);
  });
}
function resizeCharts() { chartInstances.forEach(instance => instance.resize()); }

watch([charts, displayType], renderCharts, { flush: "post", immediate: true });
onMounted(() => window.addEventListener("resize", resizeCharts));
onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  chartInstances.forEach(instance => instance.dispose());
  chartInstances.clear();
});
</script>

<style scoped>
.chart-card { margin-top: 16px; }
.chart-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.chart-heading { font-size: 16px; font-weight: 600; }
.chart-subtitle { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.chart-panel { min-width: 0; padding: 4px 0 12px; }
.panel-title { padding: 8px 12px 0; color: var(--el-text-color-primary); font-weight: 600; }
.chart-canvas { width: 100%; height: 360px; }
@media (max-width: 768px) {
  .chart-header { align-items: flex-start; flex-direction: column; }
  .chart-canvas { height: 320px; }
}
</style>
