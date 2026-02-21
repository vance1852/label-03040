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

      <a-table
        :columns="columns"
        :data-source="store.historyList"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
        :loading="tableLoading"
        row-key="id"
        :scroll="{ x: 800 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'originalFilename'">
            <div class="filename-cell">
              <FileOutlined class="file-icon" />
              <span class="filename-text">{{ record.originalFilename }}</span>
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
            <a-space>
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
            <a-button type="primary" @click="$router.push('/')">
              去签名
            </a-button>
          </a-empty>
        </template>
      </a-table>
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
  { title: '文件名', key: 'originalFilename', ellipsis: true },
  { title: '大小', key: 'fileSize', width: 100 },
  { title: '签名类型', key: 'signType', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const pagination = computed(() => ({
  current: store.historyPage,
  total: store.historyTotal,
  pageSize: 10,
  showSizeChanger: false,
  showTotal: (total) => `共 ${total} 条记录`
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
  return new Date(dateStr).toLocaleString('zh-CN')
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

.filename-cell {
  display: flex;
  align-items: center;
  gap: 6px;

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
</style>
