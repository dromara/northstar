import baseService from './baseRequest'

export default {
    loadWeeklyBarData(gatewayId, unifiedSymbol, refStartTimestamp){
        return baseService.get(`/data/bar/min?gatewayId=${gatewayId}&unifiedSymbol=${unifiedSymbol}&refStartTimestamp=${refStartTimestamp}`)
    }
}
