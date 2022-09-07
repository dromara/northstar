'use strict'

Object.defineProperty(exports, '__esModule', {
  value: true
})
exports.default = void 0

const colorOptions = [
  "#FF9600",  //橙色
  "#B3B300",  //暗黄
  "#84E6D6",  //青绿
  "#EC93D0",  //粉紫
  "#02EDED",  //青色
  "#B592D9",  //浅紫
  "#12ED71",  //绿色
  "#FBDAE6",  //浅粉
  "#B79BF8",  //紫蓝
  "#FFFF00",  //黄色
  "#FFD599",  //浅橙
  "#BEE477",  //黄绿
  "#86A2F3",  //粉蓝
  "#01C5C4",  //蓝绿
  "#EA3BF7",  //紫色
  "#AAC370",  //墨绿
  "#FB5050",  //红色
  "#C5E6FB"   //天蓝

]
exports.default = (name, colorIndex) => {
  return {
    name: 'VAL_' + name,
    shortName: '模组计算值',
    plots: [
      { key: 'value', title: `${name}: `, type: 'line', color: colorOptions[colorIndex % colorOptions.length] }
    ],
    calcTechnicalIndicator: (kLineDataList) => {
      return kLineDataList.map((data) => {
        return {
          value: data[name] 
        }
      })
    }
  }
}
