/**
 * 保存最近一次的tick与bar数据
 */
const marketCurrentDataModule = {
  state: () => ({
    curMarketGatewayId: '',
    curUnifiedSymbol: '',
    curRoomId: '',
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
      state.curTick = marketCurrentDataModule.state().curTick
      state.lastBar = null
      state.curBar = null
      state.curUnifiedSymbol = unifiedsymbol
      state.curRoomId = `${unifiedsymbol}@${state.curMarketGatewayId}`
      console.log('当前curUnifiedSymbol', unifiedsymbol)
      console.log('当前ws房间ID：' + state.curRoomId)
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
          volumedelta: tick.volumedelta
        }
      }
      if(state.curBar){
        state.curBar = {
          openprice: state.lastBar.closeprice,
          closeprice: tick.lastprice,
          highprice: Math.max(tick.lastprice, state.curBar.highprice),
          lowprice: Math.min(tick.lastprice, state.curBar.lowprice),
          volumedelta: tick.volumedelta + state.curBar.volumedelta,
          openinterestdelta: tick.openinterestdelta + state.curBar.openinterestdelta,
          actiontimestamp: tick.actiontimestamp - tick.actiontimestamp % 60000 + 60000
        }
      } else {
        state.curBar = {
          openprice: state.lastBar.closeprice,
          closeprice: tick.lastprice,
          highprice: tick.lastprice,
          lowprice: tick.lastprice,
          volumedelta: tick.volumedelta,
          openinterestdelta: tick.openinterestdelta,
          actiontimestamp: tick.actiontimestamp - tick.actiontimestamp % 60000 + 60000
        }
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
      state.curBar = null
    }
  },
  actions: {},
  getters: {
    curMarketGatewayId: (state) => {
      return state.curMarketGatewayId
    },
    curUnifiedSymbol: (state) => {
      return state.curUnifiedSymbol
    },
    getCurTick: (state) => {
      return state.curTick
    }
  }
}

export default marketCurrentDataModule
