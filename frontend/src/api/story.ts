import request from './request'

/**
 * 生成回忆文章
 */
export function generateStoryApi(theme: string) {
  return request.post('/story/generate', { theme })
}
