import request from './request'

/**
 * 获取数据面板
 */
export function getDashboardApi() {
  return request.get('/stats/dashboard')
}
