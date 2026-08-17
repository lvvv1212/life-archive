import request from './request'

/**
 * AI 智能问答（含对话历史）
 */
export function chatApi(question: string, history: Array<{role:string;content:string}>) {
  return request.post('/assistant/chat', { question, history })
}

export function rebuildIndexApi() {
  return request.post('/assistant/rebuild-index')
}
