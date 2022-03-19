import Vue from 'vue'
import VueRouter from 'vue-router'
import loginApi from '../api/loginApi'
import { Message } from 'element-ui'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/workspace',
    name: 'workspace',
    component: () => import('../views/Workspace.vue')
  },
  {
    path: '/mktdata',
    name: 'marketData',
    component: () => import('../views/MarketData.vue')
  },
  {
    path: '/trade',
    name: 'trade',
    component: () => import('../views/Trade.vue')
  },
  {
    path: '/gateway',
    name: 'gateway',
    component: () => import('../views/GatewayMgmt.vue')
  }
]

const router = new VueRouter({
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.name === 'login') {
    next()
    return
  }
  try {
    console.log('test auth')
    await loginApi.test()
    next()
  } catch (e) {
    console.log('test fail')
    Message({
      type: 'error',
      message: '认证失败！请登陆！',
      duration: 3000
    })
    next('/')
  }
})

export default router
