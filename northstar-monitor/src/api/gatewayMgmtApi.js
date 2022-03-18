import baseService from './baseRequest'

export default {
  create(gateway) {
    return baseService.post('/mgt/gateway', gateway)
  },
  remove(gatewayId) {
    return baseService.delete('/mgt/gateway?gatewayId=' + gatewayId)
  },
  update(gateway) {
    return baseService.put('/mgt/gateway', gateway)
  },
  findAll(gatewayUsage) {
    return baseService.get('/mgt/gateway?usage=' + gatewayUsage)
  },
  connect(gatewayId) {
    return baseService.get('/mgt/connection?gatewayId=' + gatewayId)
  },
  disconnect(gatewayId) {
    return baseService.delete('/mgt/connection?gatewayId=' + gatewayId)
  },
  moneyIO(gatewayId, money) {
    return baseService.post(`/mgt/moneyio?gatewayId=${gatewayId}&money=${money}`)
  },
  isActive(gatewayId) {
    return baseService.get('/mgt/gateway/active?gatewayId=' + gatewayId)
  }
}
