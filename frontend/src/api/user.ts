import request from './request'

/**
 * 用户注册
 */
export function registerApi(data: { username: string; password: string; email?: string }) {
  return request.post('/user/register', data)
}

/**
 * 用户登录
 */
export function loginApi(data: { username: string; password: string }) {
  return request.post('/user/login', data)
}

/**
 * 获取当前用户信息
 */
export function getUserInfoApi() {
  return request.get('/user/info')
}
