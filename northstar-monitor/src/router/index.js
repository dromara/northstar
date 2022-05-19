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
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/workspace',
    name: 'workspace',
    component: () => import('../views/WorkspaceView.vue'),
    children: [
      {
        path: '/mktgateway',
        name: 'mktgateway',
        props: { gatewayUsage: 'MARKET_DATA' },
        component: () => import('../views/GatewayMgmt.vue')
      },
      {
        path: '/tdgateway',
        name: 'tdgateway',
        props: { gatewayUsage: 'TRADE' },
        component: () => import('../views/GatewayMgmt.vue')
      },
      {
        path: '/specmodule',
        name: 'specmodule',
        component: () => import('../views/ModuleMgmt.vue')
      },
      {
        path: '/arbitmodule',
        name: 'arbitmodule',
        component: () => import('../views/NotImplemented.vue')
      },
      {
        path: '/mktdata',
        name: 'mktdata',
        component: () => import('../views/NotImplemented.vue')
      },
      {
        path: '/manualfttd',
        name: 'manualfttd',
        component: () => import('../views/TradeView.vue')
      },
      {
        path: '/manualopttd',
        name: 'manualopttd',
        component: () => import('../views/NotImplemented.vue')
      },
    ]
  },
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
