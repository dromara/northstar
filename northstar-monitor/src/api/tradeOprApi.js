import baseService from './baseRequest'

export default {
  buyOpen(gatewayId, contractId, price, volume, stopPrice) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      stopPrice: stopPrice,
      volume: volume,
      tradeOpr: 'BK',
      gatewayId: gatewayId
    })
  },
  sellOpen(gatewayId, contractId, price, volume, stopPrice) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      stopPrice: stopPrice,
      volume: volume,
      tradeOpr: 'SK',
      gatewayId: gatewayId
    })
  },
  closeLongPosition(gatewayId, contractId, price, volume) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      volume: volume,
      tradeOpr: 'SP',
      gatewayId: gatewayId
    })
  },
  closeShortPosition(gatewayId, contractId, price, volume) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      volume: volume,
      tradeOpr: 'BP',
      gatewayId: gatewayId
    })
  },
  cancelOrder(gatewayId, originOrderId) {
    return baseService.post('/trade/cancel', {
      originOrderId: originOrderId,
      gatewayId: gatewayId
    })
  }
}
