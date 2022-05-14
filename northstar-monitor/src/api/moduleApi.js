import baseService from './baseRequest'

export default {
  getStrategies() {
    return baseService.get('/module/components')
  },
  componentParams(metaInfo) {
    return baseService.post(`/module/component/params`, metaInfo)
  },
  insertModule(module) {
    return baseService.post('/module', module)
  },
  updateModule(module) {
    return baseService.put('/module', module)
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
  // createPosition(moduleName, position) {
  //   return baseService.post(`/module/${moduleName}/position`, position)
  // },
  // updatePosition(moduleName, position) {
  //   return baseService.put(`/module/${moduleName}/position`, position)
  // },
  // removePosition(moduleName, unifiedSymbol, dir) {
  //   return baseService.delete(
  //   `/module/${moduleName}/position?unifiedSymbol=${unifiedSymbol}&dir=${dir}`
  //   )
  // }
}
