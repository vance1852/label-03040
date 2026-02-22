<template>
  <header class="app-header">
    <div class="header-content">
      <div class="logo" @click="$router.push('/')">
        <MobileOutlined class="logo-icon" />
        <span class="logo-text">APK Signer</span>
      </div>
      <nav class="nav-links">
        <router-link to="/" class="nav-item" active-class="active">
          <HomeOutlined />
          <span>签名工具</span>
        </router-link>
        <router-link to="/history" class="nav-item" active-class="active">
          <HistoryOutlined />
          <span>历史记录</span>
        </router-link>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { MobileOutlined, HomeOutlined, HistoryOutlined } from '@ant-design/icons-vue'
</script>

<style lang="scss" scoped>
.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;

  .logo-icon {
    font-size: 24px;
    color: #1677ff;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: rgba(0, 0, 0, 0.88);
  }
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 6px;
  color: rgba(0, 0, 0, 0.65);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s;

  &:hover {
    color: #1677ff;
    background: #e6f4ff;
  }

  &.active {
    color: #1677ff;
    background: #e6f4ff;
    font-weight: 500;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 12px;
  }

  .logo-text {
    font-size: 15px !important;
  }

  .logo-icon {
    font-size: 20px !important;
  }

  .nav-links {
    gap: 2px;
    flex-shrink: 0;
  }

  .nav-item {
    padding: 6px 8px;
    font-size: 12px;
    gap: 4px;
  }
}
</style>
