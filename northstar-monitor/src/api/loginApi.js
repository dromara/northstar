import baseService from './baseRequest'

export default {
  login(username, password) {
    return baseService.post('/auth/login', {
      userName: username,
      password: password
    })
  },

  logout(){
    return baseService.get('/auth/logout')
  },

  test() {
    return baseService.get('/auth/test')
  }
}
