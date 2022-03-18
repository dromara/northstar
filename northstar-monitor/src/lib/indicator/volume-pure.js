'use strict'

Object.defineProperty(exports, '__esModule', {
  value: true
})
exports.default = void 0

var _default = {
  name: 'CJL',
  series: 'volume',
  shouldCheckParamCount: false,
  shouldFormatBigNumber: false,
  plots: [
    {
      key: 'volume',
      title: 'VOLUME: ',
      type: 'bar',
      baseValue: 0,
      minValue: 0,
      color: function color(data, options) {
        var kLineData = data.current.kLineData || {}

        if (kLineData.close > kLineData.open) {
          return options.bar.upColor
        } else if (kLineData.close < kLineData.open) {
          return options.bar.downColor
        }
        return options.bar.noChangeColor
      }
    }
  ],
  calcTechnicalIndicator: (dataList) => {
    return dataList.map((kLineData) => {
      const volume = kLineData.volume || 0
      return {
        volume
      }
    })
  }
}
exports.default = _default
