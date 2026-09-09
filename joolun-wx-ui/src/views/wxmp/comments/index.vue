<!--
  Copyright (C) 2026
  All rights reserved, Designed By www.joolun.com
-->
<template>
  <div class="app-container">
    <el-alert title="查询公众号图文评论，并直接执行开启、精选、回复和删除等管理操作。" type="info" show-icon :closable="false" />

    <el-card shadow="never" class="section-card">
      <el-form :inline="true" :model="commentForm">
        <el-form-item label="图文消息编号" required><el-input v-model="commentForm.msgDataId" placeholder="群发成功后返回的图文消息编号" style="width: 280px" /></el-form-item>
        <el-form-item label="文章序号"><el-input-number v-model="commentForm.index" :min="0" /><el-tooltip content="多图文中的位置，第一篇为 0"><el-icon class="help-icon"><QuestionFilled /></el-icon></el-tooltip></el-form-item>
        <el-form-item label="评论类型">
          <el-select v-model="commentForm.type" style="width: 130px"><el-option label="全部" :value="0" /><el-option label="精选" :value="1" /></el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" v-hasPermi="['wxmp:operations:query']" @click="searchComments">查询评论</el-button>
          <el-button type="success" :loading="actionLoading" v-hasPermi="['wxmp:operations:edit']" @click="operateArticle('open')">开启评论</el-button>
          <el-button :loading="actionLoading" v-hasPermi="['wxmp:operations:edit']" @click="operateArticle('close')">关闭评论</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading || actionLoading" :data="comments" border stripe empty-text="没有查询到评论">
        <el-table-column prop="userCommentId" label="评论 ID" width="110" />
        <el-table-column prop="openid" label="用户 OpenID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="精选" width="90" align="center">
          <template #default="scope"><el-tag :type="scope.row.commentType === 1 ? 'success' : 'info'">{{ scope.row.commentType === 1 ? '是' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="作者回复" min-width="220" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.reply?.content || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180" />
        <el-table-column label="操作" width="285" fixed="right">
          <template #default="scope">
            <el-button link type="primary" v-hasPermi="['wxmp:operations:edit']" @click="toggleFeatured(scope.row)">{{ scope.row.commentType === 1 ? '取消精选' : '设为精选' }}</el-button>
            <el-button link type="primary" v-hasPermi="['wxmp:operations:edit']" @click="openReply(scope.row)">回复</el-button>
            <el-button v-if="scope.row.reply" link type="warning" v-hasPermi="['wxmp:operations:edit']" @click="deleteReply(scope.row)">删除回复</el-button>
            <el-button link type="danger" v-hasPermi="['wxmp:operations:edit']" @click="deleteComment(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > commentForm.count"
        v-model:current-page="currentPage"
        v-model:page-size="commentForm.count"
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        @current-change="changePage"
      />
    </el-card>

    <el-dialog v-model="replyDialogVisible" title="回复评论" width="520px">
      <el-form label-width="90px">
        <el-form-item label="原评论"><el-input :model-value="replyTarget?.content" type="textarea" :rows="3" disabled /></el-form-item>
        <el-form-item label="回复内容" required><el-input v-model="replyContent" type="textarea" :rows="4" maxlength="600" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">确认回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WxMpComments">
import { getCurrentInstance, reactive, ref } from "vue";
import { getComments, operateComment } from "@/api/wxmp/wxoperations";

const { proxy } = getCurrentInstance();
const commentForm = reactive({ msgDataId: "", index: 0, begin: 0, count: 20, type: 0 });
const comments = ref([]);
const total = ref(0);
const currentPage = ref(1);
const loading = ref(false);
const actionLoading = ref(false);
const submitting = ref(false);
const replyDialogVisible = ref(false);
const replyTarget = ref(null);
const replyContent = ref("");

function requireArticle() {
  if (commentForm.msgDataId) return true;
  proxy.$modal.msgWarning("请填写图文消息编号");
  return false;
}
function actionBody(row, content) {
  return {
    msgDataId: commentForm.msgDataId,
    index: commentForm.index,
    userCommentId: row?.userCommentId ?? null,
    content,
  };
}
async function loadComments() {
  if (!requireArticle()) return;
  loading.value = true;
  try {
    const response = await getComments(commentForm);
    comments.value = response.data?.comment || [];
    total.value = response.data?.total || 0;
  } finally { loading.value = false; }
}
function searchComments() {
  currentPage.value = 1;
  commentForm.begin = 0;
  loadComments();
}
function changePage(page) {
  commentForm.begin = (page - 1) * commentForm.count;
  loadComments();
}
async function operateArticle(action) {
  if (!requireArticle()) return;
  actionLoading.value = true;
  try {
    await operateComment(action, actionBody());
    proxy.$modal.msgSuccess(action === "open" ? "评论功能已开启" : "评论功能已关闭");
  } finally { actionLoading.value = false; }
}
async function toggleFeatured(row) {
  actionLoading.value = true;
  try {
    await operateComment(row.commentType === 1 ? "unmark" : "mark", actionBody(row));
    proxy.$modal.msgSuccess(row.commentType === 1 ? "已取消精选" : "已设为精选");
    loadComments();
  } finally { actionLoading.value = false; }
}
function openReply(row) {
  replyTarget.value = row;
  replyContent.value = row.reply?.content || "";
  replyDialogVisible.value = true;
}
async function submitReply() {
  if (!replyContent.value.trim()) return proxy.$modal.msgWarning("请填写回复内容");
  submitting.value = true;
  try {
    await operateComment("reply", actionBody(replyTarget.value, replyContent.value));
    proxy.$modal.msgSuccess("回复成功");
    replyDialogVisible.value = false;
    loadComments();
  } finally { submitting.value = false; }
}
async function deleteReply(row) {
  await proxy.$modal.confirm("确认删除这条作者回复吗？");
  await operateComment("delete-reply", actionBody(row));
  proxy.$modal.msgSuccess("回复已删除");
  loadComments();
}
async function deleteComment(row) {
  await proxy.$modal.confirm("确认删除这条评论吗？删除后无法恢复。");
  await operateComment("delete", actionBody(row));
  proxy.$modal.msgSuccess("评论已删除");
  loadComments();
}
</script>

<style scoped>
.section-card { margin-top: 16px; }
.pagination { margin-top: 18px; justify-content: flex-end; }
.help-icon { margin-left: 6px; color: var(--el-text-color-secondary); cursor: help; }
</style>
