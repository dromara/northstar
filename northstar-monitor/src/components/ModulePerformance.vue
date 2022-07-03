<template>
  <el-dialog title="盈亏曲线" :visible="visible" width="80%" append-to-body :before-close="close">
    <div id="performance-chart" class="chart-wrapper"></div>
  </el-dialog>
</template>

<script>
import { dispose, init } from 'klinecharts'
import KLineUtils from '@/utils/kline-utils.js'

const fields = ['open', 'high', 'close', 'low', 'volume']
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
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
      kLineChart: null
    }
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
    visible: function () {
      this.$nextTick(() => {
        if (!this.kLineChart) {
          const kLineChart = init('performance-chart')
          this.kLineChart = kLineChart
          this.kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))
          this.kLineChart.setStyleOptions(KLineUtils.getPerformanceChartOptions())
        }
        this.updateChart()
      })
    },
    moduleDealRecords: function () {
      if (this.kLineChart) {
        this.updateChart()
      }
    }
  },
  created() {
    window.addEventListener('resize', () => {
      if (this.kLineChart) {
        this.kLineChart.resize()
      }
    })
  },
  methods: {
    updateChart() {
      this.kLineChart.applyNewData(this.performanceData)
      setTimeout(() => {
        this.kLineChart.resize() // 防止偶尔渲染不成功
      }, 2000)
    },
    close() {
      this.$emit('update:visible', false)
    },
    destroyed: function () {
      dispose('performance-chart')
    }
  }
}
</script>

<style>
.chart-wrapper {
  height: 600px;
}
</style>
