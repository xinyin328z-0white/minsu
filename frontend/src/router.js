import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import AdminView from './views/AdminView.vue'
import CustomerView from './views/CustomerView.vue'

const routes = [
  {
    path: '/login',
    component: LoginView
  },
  {
    path: '/admin',
    component: AdminView,
    meta: { requiresAuth: true }
  },
  {
    path: '/customer',
    component: CustomerView,
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    redirect: () => {
      const token = localStorage.getItem('token')
      return token ? '/customer' : '/login'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
