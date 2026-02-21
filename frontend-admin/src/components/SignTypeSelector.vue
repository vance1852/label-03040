<template>
  <div class="card-section">
    <div class="section-title">
      <SafetyCertificateOutlined />
      签名类型选择
    </div>

    <a-row :gutter="[16, 16]">
      <a-col
        v-for="type in signTypes"
        :key="type.value"
        :xs="24" :sm="12" :md="8"
      >
        <div
          class="sign-type-card"
          :class="{ active: store.selectedSignType === type.value }"
          @click="store.selectedSignType = type.value"
        >
          <div class="type-header">
            <component :is="type.icon" class="type-icon" />
            <span class="type-name">{{ type.label }}</span>
          </div>
          <p class="type-desc">{{ type.description }}</p>
          <div class="type-tags">
            <a-tag v-for="tag in type.tags" :key="tag" :color="tag === '推荐' ? 'blue' : 'default'" size="small">
              {{ tag }}
            </a-tag>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import {
  SafetyCertificateOutlined, BugOutlined, RocketOutlined,
  LockOutlined, SecurityScanOutlined, SafetyOutlined
} from '@ant-design/icons-vue'
import { useSignStore } from '../store/signStore'

const store = useSignStore()

const signTypes = [
  {
    value: 'DEBUG',
    label: 'Debug 签名',
    icon: BugOutlined,
    description: '使用调试密钥签名，适用于开发测试阶段。签名后的APK仅用于调试，不可发布到应用商店。',
    tags: ['开发测试', '快速签名']
  },
  {
    value: 'RELEASE',
    label: 'Release 签名',
    icon: RocketOutlined,
    description: '使用自定义密钥进行正式签名，适用于应用发布。包含V1+V2签名方案，兼容性好。',
    tags: ['正式发布', '推荐']
  },
  {
    value: 'V1',
    label: 'V1 签名 (JAR)',
    icon: LockOutlined,
    description: '基于JAR签名的传统方案，兼容所有Android版本。签名覆盖ZIP条目，不保护APK其他部分。',
    tags: ['全版本兼容', '传统方案']
  },
  {
    value: 'V2',
    label: 'V2 签名',
    icon: SecurityScanOutlined,
    description: 'Android 7.0+ 引入的全文件签名方案，验证速度更快，安全性更高，能检测APK任何修改。',
    tags: ['Android 7.0+', '高安全性']
  },
  {
    value: 'V3',
    label: 'V3 签名',
    icon: SafetyOutlined,
    description: 'Android 9.0+ 引入，支持密钥轮换。在V2基础上增加了签名密钥历史记录，便于密钥更新。',
    tags: ['Android 9.0+', '密钥轮换']
  }
]
</script>

<style lang="scss" scoped>
.sign-type-card {
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
  height: 100%;

  &:hover {
    border-color: #1677ff;
    background: #e6f4ff;
  }

  &.active {
    border-color: #1677ff;
    background: #e6f4ff;
    box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
  }

  .type-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .type-icon {
      font-size: 20px;
      color: #1677ff;
    }

    .type-name {
      font-size: 15px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.88);
    }
  }

  .type-desc {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.65);
    line-height: 1.6;
    margin-bottom: 8px;
  }

  .type-tags {
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }
}
</style>
