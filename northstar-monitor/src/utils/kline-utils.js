export default (()=>{
    const textColorDark = '#929AA5'
    const gridColorDark = '#292929'
    const axisLineColorDark = '#333333'
    const crossTextBackgroundColorDark = '#373a40'
  
    const textColorLight = '#76808F'
    const gridColorLight = '#ededed'
    const axisLineColorLight = '#DDDDDD'
    const crossTextBackgroundColorLight = '#686d76'
    const upColor = '#EF5350'
    const downColor = '#26A69A'
  
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
          bar: {
            upColor: upColor,
            downColor: downColor,
          },
          priceMark: {
            high: {
              color: textColor
            },
            low: {
              color: textColor
            },
            last: {
              upColor: upColor,
              downColor: downColor,
            }
          },
          tooltip: {
            text: {
              color: textColor
            }
          }
        },
        technicalIndicator: {
          bar: {
            upColor: upColor,
            downColor: downColor,
          },
          circle: {
            upColor: upColor,
            downColor: downColor,
          },
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
  

    const getPerformanceChartOptions = () => {

      return {
        candle: {
          type: 'area',
          tooltip: {
            showRule: 'none'
          }
        },
        yAxis: {
          position: 'left'
        }
      }
    }

    const createFromBar = (bar) => {
      return {
        open: bar.openprice,
        low: bar.lowprice,
        high: bar.highprice,
        close: bar.closeprice,
        volume: bar.volume,
        openInterestDelta: bar.openinterestdelta,
        openInterest: bar.openinterest,
        timestamp: bar.actiontimestamp
      }
    }
  
    return {
      getThemeOptions,
      getPerformanceChartOptions,
      createFromBar
    }
  })()