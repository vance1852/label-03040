<template>
  <div class="home-page">
    <!-- Hero Banner -->
    <div class="hero-banner card-section">
      <div class="hero-content">
        <h1 class="hero-title">
          <SafetyCertificateOutlined class="hero-icon" />
          Android APK 签名平台
        </h1>
        <p class="hero-desc">
          安全、快速、便捷的在线 APK 签名服务。支持 Debug、Release、V1/V2/V3 多种签名方案，
          提供批量签名和扫码下载功能。
        </p>
      </div>
    </div>

    <!-- 步骤指引 -->
    <div class="card-section">
      <a-steps :current="currentStep" size="small">
        <a-step title="上传文件" description="选择或拖拽APK文件" />
        <a-step title="选择签名类型" description="选择合适的签名方案" />
        <a-step title="配置参数" description="自定义签名参数" />
        <a-step title="下载结果" description="下载签名后的APK" />
      </a-steps>
    </div>

    <!-- 功能区域 -->
    <FileUploader />
    <SignTypeSelector />
    <SignConfigPanel />
    <SignResult />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { SafetyCertificateOutlined } from '@ant-design/icons-vue'
import FileUploader from '../components/FileUploader.vue'
import SignTypeSelector from '../components/SignTypeSelector.vue'
import SignConfigPanel from '../components/SignConfigPanel.vue'
import SignResult from '../components/SignResult.vue'
import { useSignStore } from '../store/signStore'

const store = useSignStore()

const currentStep = computed(() => {
  if (store.uploadedFiles.some(f => f.status === 'SUCCESS')) return 3
  if (store.uploadedFiles.length > 0) return 1
  return 0
})
</script>

<style lang="scss" scoped>
.hero-banner {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: #fff;
  border-radius: 8px;

  .hero-content {
    padding: 8px 0;
  }

  .hero-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 8px 0;
    display: flex;
    align-items: center;
    gap: 10px;
    color: #fff;

    .hero-icon {
      font-size: 28px;
    }
  }

  .hero-desc {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.85);
    margin: 0;
    line-height: 1.6;
  }

  .section-title {
    display: none;
  }

  &:hover {
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 20px !important;
  }
}
</style>
