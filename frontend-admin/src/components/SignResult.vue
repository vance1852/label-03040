<template>
  <div v-if="completedFiles.length > 0" class="card-section">
    <div class="section-title">
      <CheckCircleOutlined />
      签名结果
    </div>

    <div v-for="file in completedFiles" :key="file.id" class="result-item">
      <div class="result-header">
        <a-result
          v-if="file.status === 'SUCCESS'"
          status="success"
          :title="`${file.originalFilename} 签名成功`"
          :sub-title="`签名类型: ${file.signType} | 文件大小: ${formatSize(file.fileSize)}`"
          style="padding: 16px 0"
        >
          <template #extra>
            <a-space wrap>
              <a-button type="primary" @click="handleDownload(file.id)">
                <template #icon><DownloadOutlined /></template>
                直接下载
              </a-button>
              <a-button @click="showQrCode(file)">
                <template #icon><QrcodeOutlined /></template>
                扫码下载
              </a-button>
            </a-space>
          </template>
        </a-result>

        <a-result
          v-else-if="file.status === 'FAILED'"
          status="error"
          :title="`${file.originalFilename} 签名失败`"
          :sub-title="file.errorMessage || '未知错误'"
          style="padding: 16px 0"
        >
          <template #extra>
            <a-button type="primary" @click="retrySign(file.id)">
              <template #icon><RedoOutlined /></template>
              重新签名
            </a-button>
          </template>
        </a-result>
      </div>
    </div>

    <!-- 二维码弹窗 -->
    <a-modal
      v-model:open="qrVisible"
      title="扫码下载"
      :footer="null"
      :width="360"
      centered
    >
      <div class="qr-content">
        <img v-if="qrCodeData" :src="qrCodeData" alt="下载二维码" class="qr-image" />
        <a-spin v-else />
        <p class="qr-hint">使用手机扫描二维码下载签名后的 APK</p>
        <p class="qr-filename">{{ qrFilename }}</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined, DownloadOutlined, QrcodeOutlined, RedoOutlined
} from '@ant-design/icons-vue'
import { useSignStore } from '../store/signStore'
import { fileApi } from '../api'

const store = useSignStore()
const qrVisible = ref(false)
const qrCodeData = ref('')
const qrFilename = ref('')

const completedFiles = computed(() => {
  return store.uploadedFiles.filter(f => f.status === 'SUCCESS' || f.status === 'FAILED')
})

function handleDownload(id) {
  window.open(fileApi.getDownloadUrl(id), '_blank')
}

async function showQrCode(file) {
  qrVisible.value = true
  qrFilename.value = file.signedFilename || file.originalFilename
  qrCodeData.value = ''
  try {
    qrCodeData.value = await store.getQrCode(file.id)
  } catch {
    message.error('二维码生成失败')
  }
}

async function retrySign(id) {
  await store.executeSign(id)
  message.success('重新签名完成')
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
</script>

<style lang="scss" scoped>
.result-item {
  & + .result-item {
    border-top: 1px solid #f0f0f0;
    margin-top: 16px;
    padding-top: 16px;
  }
}

.qr-content {
  text-align: center;
  padding: 16px 0;

  .qr-image {
    width: 200px;
    height: 200px;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    padding: 8px;
  }

  .qr-hint {
    margin-top: 12px;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.65);
  }

  .qr-filename {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    word-break: break-all;
  }
}
</style>
