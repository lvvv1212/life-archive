import request from './request'

/**
 * 获取用户时间轴（全部年份）
 */
export function getTimelineApi() {
  return request.get('/timeline/list')
}

/**
 * 获取指定年份时间轴
 */
export function getTimelineByYearApi(year: number) {
  return request.get(`/timeline/year/${year}`)
}
