// [SDD-TASK: Task001]
// [SDD-SPEC: 02-功能规范.md §7.1] 路由 + 守卫(无 token 跳 /login)
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/http'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/workitems' },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/workitems', name: 'WorkitemList', component: () => import('@/views/WorkitemListView.vue') },
  { path: '/workitems/new', name: 'WorkitemCreate', component: () => import('@/views/WorkitemCreateView.vue') },
  { path: '/workitems/:id', name: 'WorkitemDetail', component: () => import('@/views/WorkitemDetailView.vue'), props: true },
  { path: '/workitems/:id/edit', name: 'WorkitemEdit', component: () => import('@/views/WorkitemEditView.vue'), props: true },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  // [SDD-SPEC: §4 BR-15 前端守卫]
  if (to.meta.public) {
    next()
    return
  }
  if (getToken()) {
    next()
  } else {
    next({ name: 'Login' })
  }
})

export default router
