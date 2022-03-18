import baseService from './baseRequest'

export default {
  dataSync() {
    return baseService.get('/data/sync')
  },

  loadHistoryBars(gatewayId, unifiedSymbol, startRefTime) {
    console.log(
      `查询网关${gatewayId} 合约${unifiedSymbol} ${new Date(startRefTime).toLocaleString()}之前的数据`
    )
    return baseService.get(
      `/data/his/bar?gatewayId=${gatewayId}&unifiedSymbol=${unifiedSymbol}&startRefTime=${startRefTime}`
    )
  },

  getAvailableContracts() {
    return baseService.get('/data/contracts')
  }
}
