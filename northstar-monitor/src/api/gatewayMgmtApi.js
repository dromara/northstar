import baseService from './baseRequest'

export default {
  create(gateway) {
    return baseService.post('/gateway', gateway)
  },
  remove(gatewayId) {
    return baseService.delete('/gateway?gatewayId=' + gatewayId)
  },
  update(gateway) {
    return baseService.put('/gateway', gateway)
  },
  findAll(gatewayUsage) {
    return baseService.get('/gateway?usage=' + gatewayUsage)
  },
  find(gatewayId){
    return baseService.get('/gateway/specific?gatewayId=' + gatewayId)
  },
  connect(gatewayId) {
    return baseService.get('/gateway/connection?gatewayId=' + gatewayId)
  },
  disconnect(gatewayId) {
    return baseService.delete('/gateway/connection?gatewayId=' + gatewayId)
  },
  moneyIO(gatewayId, money) {
    return baseService.post(`/gateway/moneyio?gatewayId=${gatewayId}&money=${money}`)
  },
  isActive(gatewayId) {
    return baseService.get('/gateway/active?gatewayId=' + gatewayId)
  },
  getContractDef(gatewayType){
    return baseService.get('/gateway/contractDefs?gatewayType=' + gatewayType)
  },
  getSubscribedContracts(gatewayType){
    return baseService.get('/gateway/subContracts?gatewayType=' + gatewayType)
  }
}
