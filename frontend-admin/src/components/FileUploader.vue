<template>
  <div class="card-section">
    <div class="section-title">
      <CloudUploadOutlined />
      文件上传
    </div>

    <a-upload-dragger
      :before-upload="handleBeforeUpload"
      :show-upload-list="false"
      accept=".apk"
      :multiple="true"
      :disabled="store.loading"
      class="upload-area"
    >
      <div class="upload-content">
        <p class="upload-icon">
          <InboxOutlined />
        </p>
        <p class="upload-text">点击或拖拽 APK 文件到此区域上传</p>
        <p class="upload-hint">支持 .apk 格式，单个文件最大 200MB，支持批量上传</p>
      </div>
    </a-upload-dragger>

    <a-progress
      v-if="store.uploadProgress > 0 && store.uploadProgress < 100"
      :percent="store.uploadProgress"
      :stroke-color="{ '0%': '#1677ff', '100%': '#52c41a' }"
      style="margin-top: 16px"
    />

    <!-- 已上传文件列表 -->
    <div v-if="store.uploadedFiles.length > 0" class="uploaded-list">
      <div class="list-header">
        <span>已上传文件 ({{ store.uploadedFiles.length }})</span>
        <a-button
          v-if="store.uploadedFiles.length > 1"
          type="link"
          size="small"
          @click="store.resetState()"
        >
          清空列表
        </a-button>
      </div>
      <div
        v-for="file in store.uploadedFiles"
        :key="file.id"
        class="file-item"
      >
        <div class="file-info">
          <FileOutlined class="file-icon" />
          <div class="file-detail">
            <span class="file-name">{{ file.originalFilename }}</span>
            <span class="file-size">{{ formatSize(file.fileSize) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <a-tag :color="statusColor(file.status)">{{ statusText(file.status) }}</a-tag>
          <a-button
            type="text"
            size="small"
            danger
            @click="store.removeUploadedFile(file.id)"
          >
            <DeleteOutlined />
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined, InboxOutlined, FileOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import { useSignStore } from '../store/signStore'

const store = useSignStore()

function handleBeforeUpload(file) {
  if (!file.name.toLowerCase().endsWith('.apk')) {
    message.error('仅支持 .apk 格式文件')
    return false
  }
  if (file.size > 200 * 1024 * 1024) {
    message.error('文件大小不能超过 200MB')
    return false
  }
  store.uploadFile(file).then(() => {
    message.success(`${file.name} 上传成功`)
  }).catch(() => {})
  return false
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

function statusColor(status) {
  const map = { PENDING: 'default', PROCESSING: 'processing', SUCCESS: 'success', FAILED: 'error' }
  return map[status] || 'default'
}

function statusText(status) {
  const map = { PENDING: '待签名', PROCESSING: '签名中', SUCCESS: '已完成', FAILED: '失败' }
  return map[status] || status
}
</script>

<style lang="scss" scoped>
.upload-area {
  :deep(.ant-upload-drag) {
    border: 2px dashed #d9d9d9;
    border-radius: 8px;
    background: #fafafa;
    transition: all 0.3s;
    padding: 32px 0;

    &:hover {
      border-color: #1677ff;
      background: #e6f4ff;
    }
  }
}

.upload-content {
  .upload-icon {
    font-size: 48px;
    color: #1677ff;
    margin-bottom: 8px;
  }
  .upload-text {
    font-size: 16px;
    color: rgba(0, 0, 0, 0.88);
    margin-bottom: 4px;
  }
  .upload-hint {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.uploaded-list {
  margin-top: 16px;

  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.65);
  }
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: background 0.2s;

  &:hover {
    background: #f0f0f0;
  }
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;

  .file-icon {
    font-size: 20px;
    color: #1677ff;
  }
}

.file-detail {
  display: flex;
  flex-direction: column;
  min-width: 0;

  .file-name {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.88);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .file-size {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>
