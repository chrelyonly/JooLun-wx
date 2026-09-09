<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="wx-user-select">
    <el-select
      :model-value="modelValue"
      filterable
      remote
      allow-create
      clearable
      default-first-option
      reserve-keyword
      :remote-method="searchUsers"
      :loading="loading"
      :placeholder="placeholder"
      loading-text="正在查询用户"
      no-data-text="没有匹配用户，可直接输入 OpenID"
      @update:model-value="$emit('update:modelValue', $event)"
      @visible-change="handleVisibleChange"
    >
      <el-option v-for="user in users" :key="user.openId" :label="optionLabel(user)" :value="user.openId">
        <div class="user-option">
          <el-avatar :size="28" :src="user.headimgUrl || ''">{{ avatarText(user) }}</el-avatar>
          <span class="user-name">{{ user.nickName || '未设置昵称' }}</span>
          <span class="user-openid">{{ user.openId }}</span>
        </div>
      </el-option>
    </el-select>
    <div v-if="showTip" class="select-tip">可选择已有用户；也可粘贴完整 OpenID 后按回车。</div>
  </div>
</template>

<script setup name="WxUserSelect">
import { onBeforeUnmount, ref } from "vue";
import { getUserOptions as getMaUserOptions } from "@/api/wxma/wxoperations";
import { getUserOptions as getMpUserOptions } from "@/api/wxmp/wxoperations";

const props = defineProps({
  modelValue: { type: String, default: "" },
  appType: { type: String, default: "1" },
  placeholder: { type: String, default: "选择用户或输入 OpenID" },
  showTip: { type: Boolean, default: true },
});
defineEmits(["update:modelValue"]);

const users = ref([]);
const loading = ref(false);
let searchTimer;
let requestSequence = 0;

function optionLabel(user) {
  return user.nickName ? `${user.nickName}（${user.openId}）` : user.openId;
}
function avatarText(user) { return String(user.nickName || "微").slice(0, 1); }
async function loadUsers(keyword = "") {
  const sequence = ++requestSequence;
  loading.value = true;
  try {
    const loader = props.appType === "2" ? getMpUserOptions : getMaUserOptions;
    const response = await loader(keyword.trim());
    if (sequence === requestSequence) users.value = Array.isArray(response.data) ? response.data : [];
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}
function searchUsers(keyword) {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => loadUsers(keyword || ""), 300);
}
function handleVisibleChange(visible) {
  if (visible) loadUsers();
}

onBeforeUnmount(() => clearTimeout(searchTimer));
</script>

<style scoped>
.wx-user-select, .wx-user-select :deep(.el-select) { width: 100%; }
.select-tip { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.4; }
.user-option { display: flex; align-items: center; gap: 8px; min-width: 0; }
.user-name { flex: none; max-width: 130px; overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }
.user-openid { min-width: 0; overflow: hidden; color: var(--el-text-color-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
</style>
