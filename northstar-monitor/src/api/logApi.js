import baseService from './baseRequest'

export default {
    tailPlatformLog(positionOffset){
        return baseService.get('/log?positionOffset=' + positionOffset)
    },
    tailModuleLog(moduleName, positionOffset){
        return baseService.get(`/log/module?name=${moduleName}&positionOffset=${positionOffset}`)
    },
    getPlatformLogLevel(){
        return baseService.get('/log/level')
    },
    setPlatformLogLevel(level){
        return baseService.put('/log/level?level=' + level)
    },
    getModuleLogLevel(moduleName){
        return baseService.get(`/log/${moduleName}/level`)
    },
    setModuleLogLevel(moduleName, level){
        return baseService.put(`/log/${moduleName}/level?level=${level}`)
    },
}