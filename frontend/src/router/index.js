import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/LoginConsole.vue'
import ProjectList from '../components/ProjectHubConsole.vue'
import ProjectWorkbench from '../components/ProjectWorkbenchConsole.vue'
import { getStoredToken } from '../api'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/projects',
    name: 'ProjectList',
    component: ProjectList
  },
  {
    path: '/project/:id',
    name: 'ProjectWorkbench',
    component: ProjectWorkbench
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from) => {
  const token = getStoredToken()

  if (to.path !== '/login' && !token) {
    return '/login'
  }

  if (to.path === '/login' && token) {
    return '/projects'
  }

  return true
})

export default router
