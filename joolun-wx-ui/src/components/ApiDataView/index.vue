<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <el-empty v-if="isEmpty" :description="emptyText" />
  <div v-else class="api-data-view">
    <el-descriptions v-if="summary.length" :column="summaryColumns" border class="summary">
      <el-descriptions-item v-for="item in summary" :key="item.key" :label="fieldLabel(item.key)">
        <el-tag v-if="typeof item.value === 'boolean'" :type="item.value ? 'success' : 'info'">
          {{ item.value ? '是' : '否' }}
        </el-tag>
        <span v-else>{{ formatValue(item.value, item.key) }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <section v-for="section in tables" :key="section.key" class="data-section">
      <div v-if="section.key !== 'root'" class="section-title">{{ fieldLabel(section.key) }}</div>
      <el-table :data="section.rows" border stripe>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column
          v-for="column in section.columns"
          :key="column"
          :prop="column"
          :label="fieldLabel(column)"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="scope">
            <el-tag v-if="typeof scope.row[column] === 'boolean'" :type="scope.row[column] ? 'success' : 'info'">
              {{ scope.row[column] ? '是' : '否' }}
            </el-tag>
            <span v-else>{{ formatValue(scope.row[column], column) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup name="ApiDataView">
import { computed } from "vue";

const props = defineProps({
  value: { default: null },
  emptyText: { type: String, default: "暂无数据，请先执行查询" },
});

const labels = {
  refDate: "日期", refHour: "小时", date: "日期", beginDate: "开始日期", endDate: "结束日期",
  sessionCnt: "打开次数", visitPv: "访问次数", visitUv: "访问人数", visitUvNew: "新访问用户",
  stayTimeUv: "人均停留时长", stayTimeSession: "次均停留时长", visitDepth: "平均访问深度",
  visitTotal: "累计用户数", sharePv: "转发次数", shareUv: "转发人数",
  pagePath: "页面路径", pageVisitPv: "页面访问次数", pageVisitUv: "页面访问人数",
  pageStayTimePv: "页面次均停留时长（秒）", entryPagePv: "入口页次数", exitPagePv: "退出页次数",
  pageSharePv: "页面转发次数", pageShareUv: "页面转发人数",
  province: "省份分布", city: "城市分布", genders: "性别分布", platforms: "终端平台分布",
  devices: "设备型号分布", ages: "年龄分布",
  access_source_session_cnt: "访问来源分布", access_staytime_info: "访问时长分布",
  access_depth_info: "访问深度分布",
  newUser: "新增用户", cancelUser: "取消关注", cumulateUser: "累计用户",
  intPageReadUser: "图文页阅读人数", intPageReadCount: "图文页阅读次数",
  oriPageReadUser: "原文页阅读人数", oriPageReadCount: "原文页阅读次数",
  shareUser: "分享人数", shareCount: "分享次数", msgUser: "消息用户数", msgCount: "消息数",
  statDate: "统计日期", targetUser: "送达人数", addToFavUser: "收藏人数", addToFavCount: "收藏次数",
  intPageFromSessionReadUser: "公众号会话阅读人数", intPageFromSessionReadCount: "公众号会话阅读次数",
  intPageFromHistMsgReadUser: "历史消息页阅读人数", intPageFromHistMsgReadCount: "历史消息页阅读次数",
  intPageFromFeedReadUser: "朋友圈阅读人数", intPageFromFeedReadCount: "朋友圈阅读次数",
  intPageFromFriendsReadUser: "好友转发阅读人数", intPageFromFriendsReadCount: "好友转发阅读次数",
  intPageFromOtherReadUser: "其他场景阅读人数", intPageFromOtherReadCount: "其他场景阅读次数",
  intPageFromKanyikanReadUser: "看一看来源阅读人数", intPageFromKanyikanReadCount: "看一看来源阅读次数",
  intPageFromSouyisouReadUser: "搜一搜来源阅读人数", intPageFromSouyisouReadCount: "搜一搜来源阅读次数",
  feedShareFromSessionUser: "会话转发朋友圈人数", feedShareFromSessionCnt: "会话转发朋友圈次数",
  feedShareFromFeedUser: "朋友圈再次转发人数", feedShareFromFeedCnt: "朋友圈再次转发次数",
  feedShareFromOtherUser: "其他场景转发朋友圈人数", feedShareFromOtherCnt: "其他场景转发朋友圈次数",
  callbackCount: "接口调用次数", failCount: "接口调用失败次数", totalTimeCost: "接口调用总耗时",
  maxTimeCost: "接口调用最大耗时", userSource: "用户来源", shareScene: "分享场景",
  msgType: "消息类型", countInterval: "消息数量区间", title: "标题", url: "链接", details: "明细",
  traceId: "检测任务 ID", statusCode: "状态码", suggest: "建议", label: "风险标签",
  receivedTime: "接收时间", errCode: "错误码", errMsg: "结果说明",
  msgId: "消息 ID", msgDataId: "图文消息 ID", status: "状态", speed: "群发速度",
  kfAccount: "客服账号", kfNick: "客服昵称", kfId: "客服 ID", kfWx: "客服微信号",
  openid: "OpenID", openId: "OpenID", createTime: "创建时间", content: "内容",
  transactionId: "微信支付单号", merchantTradeNo: "商户订单号", orderState: "订单状态",
  hasMore: "是否还有数据", lastIndex: "下一页游标", pageSize: "每页数量", count: "数量",
  list: "数据列表", items: "数据列表", records: "数据列表", data: "数据列表",
  result: "操作结果", value: "值",
};
const valueLabels = {
  suggest: { pass: "通过", review: "需要复核", risky: "存在风险" },
  status: {
    SEND_SUCCESS: "发送成功", SENDING: "发送中", SEND_FAIL: "发送失败", DELETE: "已删除",
    pending: "处理中", processing: "处理中", success: "成功", failed: "失败",
  },
  msgType: {
    text: "文本", image: "图片", voice: "语音", video: "视频", mpnews: "图文",
    miniprogrampage: "小程序卡片",
  },
  orderState: { 1: "待发货", 2: "已发货", 3: "已确认收货", 4: "交易完成", 5: "已退款" },
};

function isObject(value) { return value !== null && typeof value === "object" && !Array.isArray(value); }
function parseJsonValue(value) {
  if (typeof value !== "string" || !["{", "["].includes(value.trim().charAt(0))) return value;
  try { return JSON.parse(value); } catch { return value; }
}
function flatten(value, prefix = "", target = {}) {
  if (!isObject(value)) return target;
  Object.entries(value).forEach(([key, item]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    if (isObject(item)) flatten(item, path, target);
    else if (!Array.isArray(item)) target[path] = item;
  });
  return target;
}
function columnsFor(rows) {
  return [...new Set(rows.flatMap(row => Object.keys(row)))].slice(0, 24);
}
function normalizeRows(rows) {
  return rows.map(item => isObject(item) ? flatten(item) : { value: item });
}

const normalizedValue = computed(() => parseJsonValue(props.value));
const summary = computed(() => {
  const value = normalizedValue.value;
  if (!isObject(value)) {
    return Array.isArray(value) || value == null || value === ""
      ? []
      : [{ key: "result", value }];
  }
  return Object.entries(flatten(value)).map(([key, item]) => ({ key, value: item }));
});
const tables = computed(() => {
  const value = normalizedValue.value;
  if (Array.isArray(value)) {
    const rows = normalizeRows(value);
    return rows.length ? [{ key: "root", rows, columns: columnsFor(rows) }] : [];
  }
  if (!isObject(value)) return [];
  return Object.entries(value)
    .filter(([, item]) => Array.isArray(item) && item.length)
    .map(([key, item]) => {
      const rows = normalizeRows(item);
      return { key, rows, columns: columnsFor(rows) };
    });
});
const isEmpty = computed(() => normalizedValue.value == null || normalizedValue.value === "" ||
  (Array.isArray(normalizedValue.value) && normalizedValue.value.length === 0) ||
  (isObject(normalizedValue.value) && Object.keys(normalizedValue.value).length === 0));
const summaryColumns = computed(() => summary.value.length > 6 ? 3 : 2);

function fieldLabel(key) {
  const parts = key.split(".");
  const leaf = parts[parts.length - 1];
  if (labels[key] || labels[leaf]) return labels[key] || labels[leaf];
  if (parts.length > 1) {
    const parent = parts[parts.length - 2];
    if (labels[parent]) return `${labels[parent]}（${leaf}）`;
  }
  return "其他数据项";
}
function formatValue(value, key = "") {
  if (value == null || value === "") return "-";
  const leaf = key.split(".").pop();
  const localized = valueLabels[leaf]?.[String(value)];
  if (localized) return localized;
  const parsed = parseJsonValue(value);
  if (parsed !== value) return formatValue(parsed, key);
  if (Array.isArray(value)) return value.map(item => isObject(item) ? formatValue(item, key) : item).join("、");
  if (isObject(value)) return Object.entries(flatten(value)).map(([childKey, item]) => `${fieldLabel(childKey)}：${formatValue(item, childKey)}`).join("；");
  return value;
}
</script>

<style scoped>
.summary { margin-top: 16px; }
.data-section { margin-top: 20px; }
.section-title { margin-bottom: 10px; font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); }
</style>
