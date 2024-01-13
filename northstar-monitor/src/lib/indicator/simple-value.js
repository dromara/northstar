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

const createStyle = (indicator, colorIndex) => {
  const styleMap = {
    'line': { key: 'value', title: `${indicator.name}: `, type: 'line', color: colorOptions[colorIndex % colorOptions.length] },
    'bar': { 
      key: 'value',
      title: `${indicator.name}: `, 
      type: 'bar', 
      baseValue: 0,
      color: function color(data, options) {
        var current = data.current;
        var value = current.technicalIndicatorData ? current.technicalIndicatorData.value : 0
        if (value > 0) {
          return options.bar.upColor;
        } else if (value < 0) {
          return options.bar.downColor;
        } else {
          return options.bar.noChangeColor;
        }
      },
      isStroke: function isStroke(data) {
        var prev = data.prev,
            current = data.current;
        var value = (current.technicalIndicatorData || {}).value;
        var preVal = (prev.technicalIndicatorData || {}).value;
        return preVal > value;
      }
    },
    'circle': {
      key: 'value',
      title: `${indicator.name}: `, 
      type: 'circle',
      isStroke: false,
      color: function color(data, options) {
        return options.circle.color;
      }
    }
  }
  return styleMap[indicator.lineStyle]
}

exports.default = (indicator, colorIndex, precision) => {
  const name = indicator.name
  const plot = createStyle(indicator, colorIndex)
  const prefix = indicator.paneId === 'candle_pane' ? '主图_' : `副图${indicator.paneId.replace('pane','')}_`
  return {
    name: 'VAL_' + name,
    shortName: `${prefix}模组计算值`,
    plots: [plot],
    precision: Math.max(precision, 2),
    styles: {
      margin: {
        top: 0.2,
        bottom: 0.1
      },
      line: {
        size: indicator.lineWidth
      },
      bar: {
        upColor: '#EF5350',
        downColor: '#26A69A',
        noChangeColor: '#888888'
      },
      circle: {
        color: '#FFFA70'
      },
    },
    calcTechnicalIndicator: (kLineDataList) => {
      return kLineDataList.map((data) => {
        return {
          value: data[name] 
        }
      })
    }
  }
}
