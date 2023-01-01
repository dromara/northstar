import baseService from './baseRequest'

export default {
    loadWeeklyBarData(contractId, refStartTimestamp, firstLoad){
        return baseService.get(`/data/bar/min?contractId=${contractId}&refStartTimestamp=${refStartTimestamp}&firstLoad=${firstLoad}`)
    }
}
