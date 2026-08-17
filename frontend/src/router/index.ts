import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', noAuth: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册', noAuth: true },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/Home.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'memories',
          name: 'Memories',
          component: () => import('@/views/Memories.vue'),
          meta: { title: '记忆列表' },
        },
        {
          path: 'upload',
          name: 'Upload',
          component: () => import('@/views/Upload.vue'),
          meta: { title: '上传记忆' },
        },
        {
          path: 'timeline',
          name: 'Timeline',
          component: () => import('@/views/Timeline.vue'),
          meta: { title: '人生时间轴' },
        },
        {
          path: 'assistant',
          name: 'Assistant',
          component: () => import('@/views/Assistant.vue'),
          meta: { title: 'AI智能助手' },
        },
        {
          path: 'story',
          name: 'Story',
          component: () => import('@/views/Story.vue'),
          meta: { title: 'AI回忆生成' },
        },
        {
          path: 'stats',
          name: 'Stats',
          component: () => import('@/views/Stats.vue'),
          meta: { title: '数据分析' },
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'LifeArchive'

  if (to.meta.noAuth) {
    return next()
  }

  if (!isLoggedIn()) {
    return next('/login')
  }

  next()
})

export default router
