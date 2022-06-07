'use strict'

Object.defineProperty(exports, '__esModule', {
  value: true
})
exports.default = void 0

var _default = {
  name: 'OPI',
  series: 'volume',
  shouldCheckParamCount: false,
  shouldFormatBigNumber: false,
  plots: [
    {
      key: 'openInterest',
      title: '持仓量：',
      type: 'bar',
      baseValue: 0,
      color: function color(data, options) {
        var currentData = data.current
        if (currentData.technicalIndicatorData.openInterestDelta > 0) {
          return options.bar.downColor
        } else if (currentData.technicalIndicatorData.openInterestDelta < 0) {
          return options.bar.upColor
        } else {
          return options.bar.noChangeColor
        }
      }
    }
  ],

  calcTechnicalIndicator: (dataList) => {
    return dataList.map((kLineData) => {
      const openInterest = kLineData.openInterest || 0
      return {
        openInterest
      }
    })
  }
}
exports.default = _default
