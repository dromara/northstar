import baseService from './baseRequest'

export default {
    saveConfig(emailConfig){
        return baseService.post('/email', emailConfig)
    },

    getConfig(){
        return baseService.get('/email')
    }
}