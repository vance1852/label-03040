<template>
  <div class="card-section">
    <div class="section-title">
      <SettingOutlined />
      签名参数配置
    </div>

    <a-form layout="vertical" :model="store.signConfig">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="密钥别名 (Key Alias)">
            <a-input
              v-model:value="store.signConfig.keyAlias"
              placeholder="请输入密钥别名"
            >
              <template #prefix><KeyOutlined /></template>
            </a-input>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="有效期 (年)">
            <a-input-number
              v-model:value="store.signConfig.validityYears"
              :min="1"
              :max="50"
              style="width: 100%"
              placeholder="证书有效期"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="KeyStore 密码">
            <a-input-password
              v-model:value="store.signConfig.storePassword"
              placeholder="请输入 KeyStore 密码"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="Key 密码">
            <a-input-password
              v-model:value="store.signConfig.keyPassword"
              placeholder="请输入 Key 密码"
            >
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <div class="action-bar">
      <a-space>
        <a-button
          type="primary"
          size="large"
          :loading="store.loading"
          :disabled="!canSign"
          @click="handleSign"
        >
          <template #icon><SafetyCertificateOutlined /></template>
          {{ store.uploadedFiles.length > 1 ? '批量签名' : '开始签名' }}
        </a-button>
        <a-button
          v-if="store.uploadedFiles.length > 1"
          size="large"
          :loading="store.loading"
          :disabled="!canSign"
          @click="handleSingleSign"
        >
          仅签名选中文件
        </a-button>
      </a-space>
      <span v-if="!canSign" class="hint-text">请先上传 APK 文件</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  SettingOutlined, KeyOutlined, LockOutlined, SafetyCertificateOutlined
} from '@ant-design/icons-vue'
import { useSignStore } from '../store/signStore'

const store = useSignStore()

const canSign = computed(() => {
  return store.uploadedFiles.length > 0 &&
    store.uploadedFiles.some(f => f.status === 'PENDING' || f.status === 'FAILED')
})

async function handleSign() {
  if (store.uploadedFiles.length > 1) {
    await store.batchSign()
    message.success('批量签名完成')
  } else if (store.uploadedFiles.length === 1) {
    await store.executeSign(store.uploadedFiles[0].id)
    message.success('签名完成')
  }
}

async function handleSingleSign() {
  const pending = store.uploadedFiles.find(f => f.status === 'PENDING' || f.status === 'FAILED')
  if (pending) {
    await store.executeSign(pending.id)
    message.success('签名完成')
  }
}
</script>

<style lang="scss" scoped>
.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;

  .hint-text {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.45);
  }
}
</style>
