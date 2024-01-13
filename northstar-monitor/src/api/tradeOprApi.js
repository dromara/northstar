import baseService from './baseRequest'

export default {
  buyOpen(gatewayId, contractId, price, volume, stopPrice, priceType) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      stopPrice: stopPrice,
      volume: volume,
      tradeOpr: 'BK',
      gatewayId: gatewayId,
      priceType: priceType
    })
  },
  sellOpen(gatewayId, contractId, price, volume, stopPrice, priceType) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      stopPrice: stopPrice,
      volume: volume,
      tradeOpr: 'SK',
      gatewayId: gatewayId,
      priceType: priceType
    })
  },
  closeLongPosition(gatewayId, contractId, price, volume, priceType) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      volume: volume,
      tradeOpr: 'SP',
      gatewayId: gatewayId,
      priceType: priceType
    })
  },
  closeShortPosition(gatewayId, contractId, price, volume, priceType) {
    return baseService.post('/trade/submit', {
      contractId: contractId,
      price: price,
      volume: volume,
      tradeOpr: 'BP',
      gatewayId: gatewayId,
      priceType: priceType
    })
  },
  cancelOrder(gatewayId, originOrderId) {
    return baseService.post('/trade/cancel', {
      originOrderId: originOrderId,
      gatewayId: gatewayId
    })
  }
}
