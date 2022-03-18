import baseService from './baseRequest'

export default {
  startPlay(startDate, endDate, moduleNames, playbackAccountInitialBalance, fee=0) {
    return baseService.post(`/pb/play`, {
      startDate,
      endDate,
      moduleNames,
      playbackAccountInitialBalance,
      fee,
      precision: 'TICK'
    })
  },
  getProcess() {
    return baseService.get(`/pb/play/process`)
  },
  getBalance(moduleName){
    return baseService.get(`/pb/balance?moduleName=${moduleName}`)
  },
  getPlaybackReadiness() {
    return baseService.get(`/pb/readiness`)
  },
  getPlaybackDealRecord(moduleName){
    return baseService.get(`/pb/records/deal?moduleName=${moduleName}`)
  },
  getPlaybackTradeRecord(moduleName){
    return baseService.get(`/pb/records/trade?moduleName=${moduleName}`)
  },
  getPlaybackStatRecord(moduleName){
    return baseService.get(`/pb/records/stat?moduleName=${moduleName}`)
  }
}
