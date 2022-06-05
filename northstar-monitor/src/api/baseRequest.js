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
    config.baseURL = '/northstar'
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
    const errMsg = error.response && error.response.data ? error.response.data.message : '网络出错'
    return Promise.reject(new Error(errMsg))
  }
)

export default service
