import { createRouter, createWebHistory } from 'vue-router'
import BusinessLayout from '../components/layout/BusinessLayout.vue'
import { pinia } from '../stores/index.js'
import { useAuthStore } from '../stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/',
    component: BusinessLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'applications/new', name: 'application-new', component: () => import('../views/LeaveApplyView.vue') },
      { path: 'applications/confirm', name: 'application-confirm', component: () => import('../views/LeaveConfirmView.vue') },
      { path: 'applications/:id/complete', name: 'application-complete', component: () => import('../views/LeaveCompleteView.vue') },
      { path: 'applications', name: 'applications', component: () => import('../views/ApplicationListView.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  await authStore.restoreSession()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
