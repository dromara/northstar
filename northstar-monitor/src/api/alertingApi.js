import baseService from './baseRequest'

export default {
    subEvents(){
        return baseService.get(`/alerting/events`)
    },
    saveEvents(events){
        return baseService.post(`/alerting/events`, events)
    },
    getSettings(){
        return baseService.get('/alerting/settings')
    },
    saveSettings(settings){
        return baseService.post('/alerting/settings', settings)
    },
    testSettings(settings){
        return baseService.post('/alerting/test', settings)
    }
}