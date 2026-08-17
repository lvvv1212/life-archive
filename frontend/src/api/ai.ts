import request from './request'

/**
 * AI 自动分析（根据类型自动选择）
 */
export function analyzeMemoryApi(memoryId: number) {
  return request.post('/ai/analyze', { memoryId })
}

/**
 * AI 分析图片
 */
export function analyzeImageApi(memoryId: number) {
  return request.post('/ai/analyze/image', { memoryId })
}

/**
 * AI 分析文本
 */
export function analyzeTextApi(memoryId: number) {
  return request.post('/ai/analyze/text', { memoryId })
}
