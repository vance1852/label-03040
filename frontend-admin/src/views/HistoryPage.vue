<template>
  <div class="history-page">
    <div class="card-section">
      <div class="section-title">
        <HistoryOutlined />
        签名历史记录
        <div class="title-actions">
          <a-button
            v-if="selectedRowKeys.length > 0"
            danger
            size="small"
            @click="handleBatchDelete"
          >
            批量删除 ({{ selectedRowKeys.length }})
          </a-button>
          <a-button size="small" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>

      <!-- 桌面端：表格视图 -->
      <div class="desktop-view">
        <a-table
          :columns="columns"
          :data-source="store.historyList"
          :pagination="pagination"
          :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
          :loading="tableLoading"
          row-key="id"
          size="middle"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'originalFilename'">
              <div class="filename-cell">
                <FileOutlined class="file-icon" />
                <span class="filename-text" :title="record.originalFilename">{{ record.originalFilename }}</span>
              </div>
            </template>

            <template v-if="column.key === 'fileSize'">
              {{ formatSize(record.fileSize) }}
            </template>

            <template v-if="column.key === 'signType'">
              <a-tag color="blue">{{ record.signType }}</a-tag>
            </template>

            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">
                {{ statusText(record.status) }}
              </a-tag>
            </template>

            <template v-if="column.key === 'createdAt'">
              {{ formatDate(record.createdAt) }}
            </template>

            <template v-if="column.key === 'action'">
              <a-space :size="4">
                <a-button
                  v-if="record.status === 'SUCCESS'"
                  type="link"
                  size="small"
                  @click="handleDownload(record.id)"
                >
                  <DownloadOutlined /> 下载
                </a-button>
                <a-popconfirm
                  title="确定删除此记录？"
                  @confirm="handleDelete(record.id)"
                >
                  <a-button type="link" size="small" danger>
                    <DeleteOutlined /> 删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>

          <template #emptyText>
            <a-empty description="暂无签名记录">
              <a-button type="primary" @click="$router.push('/')">去签名</a-button>
            </a-empty>
          </template>
        </a-table>
      </div>

      <!-- 移动端：卡片视图 -->
      <div class="mobile-view">
        <a-spin :spinning="tableLoading">
          <div v-if="store.historyList.length === 0" style="padding: 32px 0">
            <a-empty description="暂无签名记录">
              <a-button type="primary" @click="$router.push('/')">去签名</a-button>
            </a-empty>
          </div>
          <div v-else class="mobile-list">
            <div
              v-for="record in store.historyList"
              :key="record.id"
              class="mobile-card"
            >
              <div class="mobile-card-header">
                <div class="mobile-filename">
                  <a-checkbox
                    :checked="selectedRowKeys.includes(record.id)"
                    @change="toggleMobileSelect(record.id)"
                  />
                  <FileOutlined class="file-icon" />
                  <span class="filename-text">{{ record.originalFilename }}</span>
                </div>
                <a-tag :color="statusColor(record.status)" :bordered="false">
                  {{ statusText(record.status) }}
                </a-tag>
              </div>
              <div class="mobile-card-body">
                <div class="mobile-meta">
                  <span><a-tag color="blue" :bordered="false">{{ record.signType }}</a-tag></span>
                  <span class="meta-text">{{ formatSize(record.fileSize) }}</span>
                  <span class="meta-text">{{ formatDate(record.createdAt) }}</span>
                </div>
              </div>
              <div class="mobile-card-footer">
                <a-button
                  v-if="record.status === 'SUCCESS'"
                  type="primary"
                  size="small"
                  ghost
                  @click="handleDownload(record.id)"
                >
                  <DownloadOutlined /> 下载
                </a-button>
                <a-popconfirm
                  title="确定删除此记录？"
                  @confirm="handleDelete(record.id)"
                >
                  <a-button size="small" danger ghost>
                    <DeleteOutlined /> 删除
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
          <div v-if="store.historyTotal > 10" class="mobile-pagination">
            <a-pagination
              :current="store.historyPage"
              :total="store.historyTotal"
              :page-size="10"
              size="small"
              simple
              @change="(page) => { store.historyPage = page; fetchData() }"
            />
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  HistoryOutlined, ReloadOutlined, FileOutlined,
  DownloadOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import { useSignStore } from '../store/signStore'
import { fileApi } from '../api'

const store = useSignStore()
const tableLoading = ref(false)
const selectedRowKeys = ref([])

const columns = [
  { title: '文件名', key: 'originalFilename', width: '30%', ellipsis: true },
  { title: '大小', key: 'fileSize', width: '10%', align: 'center' },
  { title: '类型', key: 'signType', width: '10%', align: 'center' },
  { title: '状态', key: 'status', width: '8%', align: 'center' },
  { title: '时间', key: 'createdAt', width: '15%' },
  { title: '操作', key: 'action', width: '18%', align: 'center' }
]

const pagination = computed(() => ({
  current: store.historyPage,
  total: store.historyTotal,
  pageSize: 10,
  showSizeChanger: false,
  showTotal: (total) => `共 ${total} 条`
}))

onMounted(() => fetchData())

async function fetchData() {
  tableLoading.value = true
  try {
    await store.fetchHistory(store.historyPage)
  } finally {
    tableLoading.value = false
  }
}

function handleTableChange(pag) {
  store.historyPage = pag.current
  fetchData()
}

function onSelectChange(keys) {
  selectedRowKeys.value = keys
}

function toggleMobileSelect(id) {
  const idx = selectedRowKeys.value.indexOf(id)
  if (idx >= 0) {
    selectedRowKeys.value.splice(idx, 1)
  } else {
    selectedRowKeys.value.push(id)
  }
}

function handleDownload(id) {
  window.open(fileApi.getDownloadUrl(id), '_blank')
}

async function handleDelete(id) {
  await store.deleteHistory(id)
  message.success('删除成功')
}

function handleBatchDelete() {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条记录？`,
    onOk: async () => {
      await store.batchDeleteHistory(selectedRowKeys.value)
      selectedRowKeys.value = []
      message.success('批量删除成功')
    }
  })
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
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
.section-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  .title-actions {
    margin-left: auto;
    display: flex;
    gap: 8px;
  }
}

// 桌面端表格
.desktop-view {
  margin: 0 -12px;

  :deep(.ant-table-wrapper) {
    overflow: hidden;
  }

  :deep(.ant-table) {
    font-size: 13px;
    table-layout: fixed;
    width: 100%;
  }

  :deep(.ant-btn-link) {
    padding: 0 4px;
    font-size: 13px;
  }
}

.filename-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  .file-icon {
    color: #1677ff;
    flex-shrink: 0;
  }

  .filename-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// 移动端卡片
.mobile-view {
  display: none;
}

.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.mobile-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.mobile-filename {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;

  .file-icon {
    color: #1677ff;
    flex-shrink: 0;
  }

  .filename-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.88);
  }
}

.mobile-card-body {
  margin-bottom: 10px;
}

.mobile-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  .meta-text {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.mobile-card-footer {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.mobile-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

// 响应式断点
@media (max-width: 768px) {
  .desktop-view {
    display: none;
  }
  .mobile-view {
    display: block;
  }
}
</style>
