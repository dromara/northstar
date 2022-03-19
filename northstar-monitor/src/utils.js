// 对Date的扩展，将 Date 转化为指定格式的String   
// 月(M)、日(d)、小时(H)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，   
// 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)   
// 例子：   
// (new Date()).Format("yyyy-MM-dd HH:mm:ss.S") ==> 2006-07-02 08:09:04.423   
// (new Date()).Format("yyyy-M-d H:m:s.S")      ==> 2006-7-2 8:9:4.18   
Date.prototype.format = function(fmt)   
{ //author: meizz   
  var o = {   
    "M+" : this.getMonth()+1,                 //月份   
    "d+" : this.getDate(),                    //日   
    "h+" : this.getHours(),                   //小时   
    "m+" : this.getMinutes(),                 //分   
    "s+" : this.getSeconds(),                 //秒   
    "q+" : Math.floor((this.getMonth()+3)/3), //季度   
    "S"  : this.getMilliseconds()             //毫秒   
  };   
  if(/(y+)/.test(fmt))   
    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));   
  for(var k in o)   
    if(new RegExp("("+ k +")").test(fmt))   
  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
  return fmt;   
}


export var KLineUtils = (()=>{
  const textColorDark = '#929AA5'
  const gridColorDark = '#292929'
  const axisLineColorDark = '#333333'
  const crossTextBackgroundColorDark = '#373a40'

  const textColorLight = '#76808F'
  const gridColorLight = '#ededed'
  const axisLineColorLight = '#DDDDDD'
  const crossTextBackgroundColorLight = '#686d76'

  const getThemeOptions = (theme) => {
    const textColor = theme === 'dark' ? textColorDark : textColorLight
    const gridColor = theme === 'dark' ? gridColorDark : gridColorLight
    const axisLineColor = theme === 'dark' ? axisLineColorDark : axisLineColorLight
    const crossLineColor = theme === 'dark' ? axisLineColorDark : axisLineColorLight
    const crossTextBackgroundColor =
      theme === 'dark' ? crossTextBackgroundColorDark : crossTextBackgroundColorLight
    return {
      grid: {
        horizontal: {
          color: gridColor
        },
        vertical: {
          color: gridColor
        }
      },
      candle: {
        priceMark: {
          high: {
            color: textColor
          },
          low: {
            color: textColor
          }
        },
        tooltip: {
          text: {
            color: textColor
          }
        }
      },
      technicalIndicator: {
        tooltip: {
          text: {
            color: textColor
          }
        }
      },
      xAxis: {
        axisLine: {
          color: axisLineColor
        },
        tickLine: {
          color: axisLineColor
        },
        tickText: {
          color: textColor
        }
      },
      yAxis: {
        axisLine: {
          color: axisLineColor
        },
        tickLine: {
          color: axisLineColor
        },
        tickText: {
          color: textColor
        }
      },
      separator: {
        color: axisLineColor
      },
      crosshair: {
        horizontal: {
          line: {
            color: crossLineColor
          },
          text: {
            backgroundColor: crossTextBackgroundColor
          }
        },
        vertical: {
          line: {
            color: crossLineColor
          },
          text: {
            backgroundColor: crossTextBackgroundColor
          }
        }
      }
    }
  }

  const createFromBar = (bar) => {
    return {
      open: bar.openprice,
      low: bar.lowprice,
      high: bar.highprice,
      close: bar.closeprice,
      volume: bar.volumedelta,
      openInterestDelta: bar.openinterestdelta,
      timestamp: bar.actiontimestamp
    }
  }

  return {
    getThemeOptions,
    createFromBar
  }
})()