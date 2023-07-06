<template>
  <div :id="chartId" class="chart-wrapper"></div>
</template>

<script>
import { dispose, init } from 'klinecharts'
import KLineUtils from '@/utils/kline-utils.js'

export default {
  props: {
    moduleInitBalance: {
      type: Number,
      default: 0
    },
    moduleDealRecords: {
      type: Array,
      default: () => []
    },
    largeView: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      chartId: 'perf-chart_' + Math.random(),
      kLineChart: null
    }
  },
  mounted() {
    const kLineChart = init(this.chartId)
    this.kLineChart = kLineChart
    this.kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))
    this.kLineChart.setStyleOptions(KLineUtils.getPerformanceChartOptions())
    if(this.largeView){
      this.kLineChart.addTechnicalIndicatorTemplate({
        name: 'drawback',
        shortName: `回撤统计`,
        plots: [{ 
          key: 'drawback',
          title: '回撤统计', 
          type: 'bar', 
          baseValue: 0,
          color: '#FFC107',
          isStroke: false
        }],
        precision: 2,
        styles: {
          margin: {
            top: 0.3,
            bottom: 0.1
          },
          line: {
            size: 1
          },
          bar: {
            upColor: '#EF5350',
            downColor: '#26A69A',
            noChangeColor: '#888888'
          },
        },
        calcTechnicalIndicator: (kLineDataList) => {
          return kLineDataList
        }
      })
      this.kLineChart.addTechnicalIndicatorTemplate({
        name: 'dealProfit',
        shortName: `逐笔盈亏`,
        plots: [{ 
          key: 'dealProfit',
          title: '逐笔盈亏', 
          type: 'bar', 
          baseValue: 0,
          color: function color(data, options) {
            var current = data.current;
            var value = current.technicalIndicatorData ? current.technicalIndicatorData.dealProfit : 0
            if (value > 0) {
              return options.bar.upColor;
            } else if (value < 0) {
              return options.bar.downColor;
            } else {
              return options.bar.noChangeColor;
            }
          },
          isStroke: false
        }],
        precision: 2,
        styles: {
          margin: {
            top: 0.3,
            bottom: 0.1
          },
          line: {
            size: 1
          },
          bar: {
            upColor: '#EF5350',
            downColor: '#26A69A',
            noChangeColor: '#888888'
          },
        },
        calcTechnicalIndicator: (kLineDataList) => {
          return kLineDataList
        }
      })
      this.kLineChart.createTechnicalIndicator('drawback', true, {id: 'pane1'})
      this.kLineChart.createTechnicalIndicator('dealProfit', true, {id: 'pane2'})
    }
    this.updateChart()
  },
  computed: {
    performanceData() {
      let balance = this.moduleInitBalance
      let initTime = this.moduleDealRecords.length
        ? this.moduleDealRecords[0].openTrade.tradetimestamp
        : 0
      let maxBalance = balance
      let dealArr = this.moduleDealRecords.map((rec) => {
        const closeTrade = rec.closeTrade
        const obj = { timestamp: closeTrade.tradetimestamp }
        balance += rec.dealProfit
        maxBalance = Math.max(maxBalance, balance)
        obj['close'] = balance
        obj['drawback'] = -(maxBalance - balance)
        obj['dealProfit'] = rec.dealProfit
        return obj
      })
      let startObj = { timestamp: initTime, close: this.moduleInitBalance, drawback: 0 }
      return [startObj].concat(dealArr)
    }
  },
  watch: {
    moduleDealRecords: function () {
      if (this.kLineChart) {
        this.updateChart()
      }
    }
  },
  created() {
    window.addEventListener('resize', () => {
      this.refresh()
    })
  },
  methods: {
    updateChart() {
      this.kLineChart.applyNewData(this.performanceData)
      this.refresh() // 防止偶尔渲染不成功
    },
    refresh() {
      console.log('Refreshing chart')
      if (this.kLineChart) {
        this.kLineChart.resize()
      }
    },
    destroyed: function () {
      dispose(this.chartId)
    }
  }
}
</script>

<style>
.chart-wrapper {
  height: 100%;
}
</style>
