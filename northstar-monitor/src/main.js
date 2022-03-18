/* eslint-disable */
// import Vue from 'vue'
// import ELEMENT from 'element-ui'
import App from './App.vue'
import router from './router'
import store from './store'

import NumberFilter from '@/filter/number-filter'
import '@/assets/style/index.css'
import './utils'

console.log(NumberFilter)

Vue.use(ELEMENT, { size: 'mini' })

Vue.config.productionTip = false

Vue.filter('accountingFormatter', NumberFilter.accountingFormatter)

// 统一异常处理
Vue.config.errorHandler = function (e, v) {
  console.warn('统一异常处理', e)
  ELEMENT.Message({
    message: e.message || '遇到未知异常',
    type: 'error',
    duration: 5 * 1000
  })
  if (v) {
    v.loading = false
  }
  console.error(e)
}

new Vue({
  router,
  store,
  render: (h) => h(App)
}).$mount('#app')
