/**
 * 保存最近一次的tick与bar数据
 */
const marketCurrentDataModule = {
  state: () => ({
    curMarketGatewayId: '',
    curUnifiedSymbol: '',
    curTick: {
      askpriceList: [0, 0, 0, 0, 0],
      askvolumeList: [0, 0, 0, 0, 0],
      bidpriceList: [0, 0, 0, 0, 0],
      bidvolumeList: [0, 0, 0, 0, 0],
      lastprice: 0,
      volumedelta: 0
    },
    lastBar: null,
    curBar: null,
  }),
  mutations: {
    resetMarketCurrentDataModule(state) {
      Object.assign(state, marketCurrentDataModule.state())
      console.log('重置marketCurrentDataModule', state)
    },
    updateFocusMarketGatewayId(state, gatewayId) {
      state.curMarketGatewayId = gatewayId
      state.curUnifiedSymbol = ''
      state.lastBar = null
      state.curBar = null
      console.log('当前curMarketGatewayId', gatewayId)
    },
    updateFocusUnifiedSymbol(state, unifiedsymbol) {
      state.lastBar = null
      state.curBar = null
      state.curUnifiedSymbol = unifiedsymbol
      console.log('当前curUnifiedSymbol', unifiedsymbol)
    },
    updateTick(state, tick) {
      if (
        state.curMarketGatewayId !== tick.gatewayid ||
        state.curUnifiedSymbol !== tick.unifiedsymbol
      ) {
        return
      }
      state.curTick = tick
      if(!state.lastBar){
        state.lastBar = {
          openprice: tick.lastprice,
          closeprice: tick.lastprice,
          openinterest: tick.openinterest,
          volume: tick.volume
        }
      }
      state.curBar = {
        openprice: state.lastBar.closeprice,
        closeprice: tick.lastprice,
        highprice: state.curBar ? Math.max(tick.lastprice, state.curBar.highprice) : tick.lastprice,
        lowprice: state.curBar ? Math.min(tick.lastprice, state.curBar.lowprice) : tick.lastprice,
        volumedelta: tick.volume - state.lastBar.volume,
        openinterestdelta: tick.openinterest - state.lastBar.openinterest,
        actiontimestamp: tick.actiontimestamp - tick.actiontimestamp % 60000
      }
    },
    updateBar(state, bar) {
      if (
        state.curMarketGatewayId !== bar.gatewayid ||
        state.curUnifiedSymbol !== bar.unifiedsymbol
      ) {
        return
      }
      state.lastBar = bar
    }
  },
  actions: {},
  getters: {
    curMarketGatewayId: (state) => {
      return state.curMarketGatewayId
    },
    curUnifiedSymbol: (state) => {
      return state.curUnifiedSymbol
    }
  }
}

export default marketCurrentDataModule
