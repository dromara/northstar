<template>
  <div :id="chartId" class="chart-wrapper"></div>
</template>

<script>
import { dispose, init } from 'klinecharts'
import KLineUtils from '@/utils/kline-utils.js'

const fields = ['open', 'high', 'close', 'low', 'volume']
export default {
  props: {
    moduleInitBalance: {
      type: Number,
      default: 0
    },
    moduleDealRecords: {
      type: Array,
      default: () => []
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
    this.updateChart()
  },
  computed: {
    performanceData() {
      let balance = this.moduleInitBalance
      let initTime = this.moduleDealRecords.length
        ? this.moduleDealRecords[0].openTrade.tradetimestamp
        : 0
      let dealArr = this.moduleDealRecords.map((rec) => {
        const closeTrade = rec.closeTrade
        const obj = { timestamp: closeTrade.tradetimestamp }
        balance += rec.dealProfit
        fields.forEach((field) => (obj[field] = balance))
        return obj
      })
      let initState = () => {
        const obj = { timestamp: initTime }
        fields.forEach((field) => (obj[field] = this.moduleInitBalance))
        return obj
      }
      return [initState()].concat(dealArr)
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
