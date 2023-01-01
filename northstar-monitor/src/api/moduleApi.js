import baseService from './baseRequest'

export default {
  getStrategies() {
    return baseService.get('/module/strategies')
  },
  componentParams(metaInfo) {
    return baseService.post(`/module/strategy/params`, metaInfo)
  },
  insertModule(module) {
    return baseService.post('/module', module)
  },
  updateModule(module, reset) {
    return baseService.put(`/module?reset=${!!reset}`, module)
  },
  getAllModules() {
    return baseService.get('/module')
  },
  toggleModuleState(name) {
    return baseService.get('/module/toggle?name=' + name)
  },
  removeModule(name) {
    return baseService.delete('/module?name=' + name)
  },
  getModuleRuntime(name) {
    return baseService.get('/module/rt/info?name=' + name)
  },
  getModuleDealRecords(name) {
    return baseService.get('/module/deal/records?name=' + name)
  },
  mockTradeAdjustment(moduleName, mockTrade) {
    return baseService.post(`/module/${moduleName}/mockTrade`, mockTrade)
  },
}
