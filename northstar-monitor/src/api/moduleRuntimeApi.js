import baseService from './baseRequest'

export default {
    getModuleInfo(name) {
        return baseService.get('/module/info?name=' + name)
    },
    getModuleDealRecords(name) {
        return baseService.get('/module/records?name=' + name)
    },
    createPosition(moduleName, position) {
        return baseService.post(`/module/${moduleName}/position`, position)
    },
    updatePosition(moduleName, position) {
        return baseService.put(`/module/${moduleName}/position`, position)
    },
    removePosition(moduleName, unifiedSymbol, dir) {
        return baseService.delete(
        `/module/${moduleName}/position?unifiedSymbol=${unifiedSymbol}&dir=${dir}`
        )
    }
}