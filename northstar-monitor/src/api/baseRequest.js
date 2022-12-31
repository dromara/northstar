import router from '@/router'
/* eslint-disable */
// import axios from 'axios'

// create an axios instance
const service = axios.create({
  withCredentials: true, // send cookies when cross-domain requests
  timeout: 0 // 请求不设超时时间
})

service.interceptors.request.use(
  (config) => {
    //根据vuex store内容动态设置baseurl
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if(response.status === 200 && !res.status){
      return
    }
    if (res.status === 555) {
      throw new Error(res.message)
    }
    // 统一请求的异常处理
    if (res.status !== 200) {
      throw new Error(res.message)
    } else {
      return res.data
    }
  },
  (error) => {
    console.log('error', error.response.status)
    let errMsg = error.response && error.response.data ? error.response.data.message : '网络出错'
    if(error.response.status === 401){
      router.push({name: 'login'})
      errMsg = '会话过期，请重新登陆'
    }
    return Promise.reject(new Error(errMsg))
  }
)

const mergeConfig = (config) => {
  return Object.assign({baseURL: `${window.baseURL || ''}/northstar`, withCredentials: true}, config)
}

export default {
  get(url, config){
    return service.get(url, mergeConfig(config))
  },
  post(url, data, config){
    return service.post(url, data, mergeConfig(config))
  },
  put(url, data, config){
    return service.put(url, data, mergeConfig(config))
  },
  delete(url, config){
    return service.delete(url, mergeConfig(config))
  },
  head(url, config){
    return service.head(url, mergeConfig(config))
  }
}
