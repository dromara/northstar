'use strict'

Object.defineProperty(exports, '__esModule', {
  value: true
})
exports.default = void 0

const colorOptions = [
  "#FF9600",  //橙色
  "#02EDED",  //青色
  "#B592D9",  //浅紫
  "#FBDAE6",  //浅粉
  "#F288AF",  //紫红
  "#01C5C4",  //蓝绿
  "#AAC370",  //墨绿
  "#C5E6FB"   //天蓝
]
let colorIndex = 0
exports.default = (name, indicatorData) => {
  return {
    name: 'VAL_' + name,
    shortName: '模组计算值',
    plots: [
      { key: 'value', title: `${name}: `, type: 'line', color: colorOptions[colorIndex++ % colorOptions.length] }
    ],
    calcTechnicalIndicator: (kLineDataList) => {
      const timeValueMap = indicatorData.values.reduce((obj, val) => {
        obj[val.timestamp] = val.value
        return obj
      }, {})
      return kLineDataList.map((data) => {
        return {
          value: timeValueMap[data.timestamp] 
        }
      })
    }
  }
}
