import baseService from './baseRequest'

export default {
    loadWeeklyBarData(gatewayId, unifiedSymbol, refStartTimestamp, firstLoad){
        return baseService.get(`/data/bar/min?gatewayId=${gatewayId}&unifiedSymbol=${unifiedSymbol}&refStartTimestamp=${refStartTimestamp}&firstLoad=${firstLoad}`)
    }
}
