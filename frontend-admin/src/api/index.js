import axios from 'axios'
import { message } from 'ant-design-vue'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000 // 5分钟超时，签名可能耗时较长
})

api.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code && data.code !== 200) {
      message.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const msg = error.response.data?.message || `请求失败 (${error.response.status})`
      message.error(msg)
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后重试')
    } else {
      message.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

// 签名相关 API
export const signApi = {
  upload: (file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/sign/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },
  execute: (data) => api.post('/sign/execute', data),
  batchSign: (data) => api.post('/sign/batch', data),
  getStatus: (id) => api.get(`/sign/status/${id}`)
}

// 文件相关 API
export const fileApi = {
  getDownloadUrl: (id) => `/api/file/download/${id}`,
  getQrCode: (id) => api.get(`/file/qrcode/${id}`)
}

// 历史记录 API
export const historyApi = {
  list: (page = 1, size = 10) => api.get('/history/list', { params: { page, size } }),
  delete: (id) => api.delete(`/history/${id}`),
  batchDelete: (ids) => api.delete('/history/batch', { data: ids })
}

export default api
