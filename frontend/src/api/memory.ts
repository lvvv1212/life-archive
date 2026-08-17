import request from './request'

/**
 * 上传记忆（图片/视频）
 */
export function uploadMemoryApi(formData: FormData) {
  return request.post('/memory/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 创建文本记忆
 */
export function createTextMemoryApi(data: { title: string; content: string; memoryType?: string }) {
  return request.post('/memory/text', data)
}

/**
 * 获取记忆列表
 */
export function getMemoryListApi(params: { memoryType?: string; page?: number; size?: number }) {
  return request.get('/memory/list', { params })
}

/**
 * 获取记忆详情
 */
export function getMemoryDetailApi(id: number) {
  return request.get(`/memory/${id}`)
}

/**
 * 删除记忆
 */
export function deleteMemoryApi(id: number) {
  return request.delete(`/memory/${id}`)
}

/**
 * 更新记忆
 */
export function updateMemoryApi(id: number, data: Record<string, any>) {
  return request.put(`/memory/${id}`, data)
}

/**
 * 获取记忆统计
 */
export function getMemoryStatsApi() {
  return request.get('/memory/stats')
}
