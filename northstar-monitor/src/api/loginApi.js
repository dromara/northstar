import baseService from './baseRequest'

export default {
  login(username, password) {
    return baseService.post('/auth/login', {
      userName: username,
      password: password
    })
  },

  test() {
    return baseService.get('/auth/test')
  }
}
