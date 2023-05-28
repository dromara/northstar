import baseService from './baseRequest'
import md5 from 'js-md5'

export default {
  login(username, password) {
    const salt = new Date().getTime()
    return baseService.post(`/auth/login?timestamp=${salt}`, {
      userName: username,
      password: md5(password + salt)
    })
  },
  logout(){
    return baseService.get('/auth/logout')
  },
  healthyCheck(){
    return baseService.head(`/auth/login`)
  },
  test() {
    return baseService.get('/auth/test')
  },
  socketioPort(){
    return baseService.get('/auth/socketio')
  }
}
