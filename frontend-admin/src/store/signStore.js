import { defineStore } from 'pinia'
import { ref } from 'vue'
import { signApi, fileApi, historyApi } from '../api'

export const useSignStore = defineStore('sign', () => {
  const uploadedFiles = ref([])
  const currentHistory = ref(null)
  const signResult = ref(null)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const historyList = ref([])
  const historyTotal = ref(0)
  const historyPage = ref(1)

  const selectedSignType = ref('RELEASE')
  const signConfig = ref({
    keyAlias: 'apk-key',
    validityYears: 25,
    storePassword: 'android',
    keyPassword: 'android'
  })

  async function uploadFile(file) {
    loading.value = true
    uploadProgress.value = 0
    try {
      const res = await signApi.upload(file, (event) => {
        uploadProgress.value = Math.round((event.loaded * 100) / event.total)
      })
      currentHistory.value = res.data
      uploadedFiles.value.push(res.data)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function executeSign(historyId) {
    loading.value = true
    try {
      const res = await signApi.execute({
        historyId,
        signType: selectedSignType.value,
        ...signConfig.value
      })
      signResult.value = res.data
      // 更新 uploadedFiles 中对应的记录
      const idx = uploadedFiles.value.findIndex(f => f.id === historyId)
      if (idx !== -1) {
        uploadedFiles.value[idx] = res.data
      }
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function batchSign() {
    loading.value = true
    try {
      const ids = uploadedFiles.value
        .filter(f => f.status === 'PENDING' || f.status === 'FAILED')
        .map(f => f.id)
      if (ids.length === 0) return []

      const res = await signApi.batchSign({
        historyIds: ids,
        signType: selectedSignType.value,
        ...signConfig.value
      })
      // 更新列表
      res.data.forEach(item => {
        const idx = uploadedFiles.value.findIndex(f => f.id === item.id)
        if (idx !== -1) uploadedFiles.value[idx] = item
      })
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function getQrCode(id) {
    const res = await fileApi.getQrCode(id)
    return res.data
  }

  async function fetchHistory(page = 1, size = 10) {
    const res = await historyApi.list(page, size)
    historyList.value = res.data.records
    historyTotal.value = res.data.total
    historyPage.value = page
  }

  async function deleteHistory(id) {
    await historyApi.delete(id)
    await fetchHistory(historyPage.value)
  }

  async function batchDeleteHistory(ids) {
    await historyApi.batchDelete(ids)
    await fetchHistory(historyPage.value)
  }

  function removeUploadedFile(id) {
    uploadedFiles.value = uploadedFiles.value.filter(f => f.id !== id)
    if (currentHistory.value?.id === id) {
      currentHistory.value = null
    }
  }

  function resetState() {
    uploadedFiles.value = []
    currentHistory.value = null
    signResult.value = null
    uploadProgress.value = 0
  }

  return {
    uploadedFiles, currentHistory, signResult, loading, uploadProgress,
    historyList, historyTotal, historyPage,
    selectedSignType, signConfig,
    uploadFile, executeSign, batchSign, getQrCode,
    fetchHistory, deleteHistory, batchDeleteHistory,
    removeUploadedFile, resetState
  }
})
