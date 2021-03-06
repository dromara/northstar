<template>
  <el-dialog title="模组运行状态" :visible="visible" fullscreen class="flex-col" @close="close">
    <ModulePositionForm
      :visible.sync="positionFormVisible"
      :moduleAccount="accountSettings"
      :moduleName="module.moduleName"
      @save="onSave"
    />
    <ModulePerformanceForm
      :visible.sync="performanceVisible"
      :moduleInitBalance="accountInfo.initBalance"
      :moduleDealRecords="dealRecords"
    />
    <div class="module-rt-wrapper">
      <div class="side-panel">
        <div class="description-wrapper">
          <el-descriptions class="margin-top" title="模组信息" :column="3">
            <template slot="extra">
              <el-switch
                v-model="isManualUpdate"
                inactive-text="自动刷新"
                active-color="#D8DBE1"
                inactive-color="#f7c139"
              >
              </el-switch>
              <el-button class="compact mb-10 ml-10" icon="el-icon-refresh" @click="refresh"
                >刷新数据</el-button
              >
              <el-button
                class="compact mb-10"
                icon="el-icon-info"
                @click="performanceVisible = true"
                >模组绩效</el-button
              >
            </template>
            <el-descriptions-item label="名称">{{ moduleRuntime.moduleName }}</el-descriptions-item>
            <el-descriptions-item label="启停状态"
              ><el-tag size="small" :type="`${moduleRuntime.enabled ? 'success' : 'danger'}`">{{
                moduleRuntime.enabled ? '启用' : '停用'
              }}</el-tag></el-descriptions-item
            >
            <el-descriptions-item label="盘口状态">
              <el-tag size="small">{{
                {
                  HOLDING_LONG: '持多单',
                  HOLDING_SHORT: '持空单',
                  EMPTY: '无持仓',
                  EMPTY_HEDGE: '对冲锁仓',
                  HOLDING_HEDGE: '对冲持仓',
                  PENDING_ORDER: '等待成交'
                }[moduleRuntime.moduleState]
              }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总盈亏">{{
              totalProfit | formatter
            }}</el-descriptions-item>
            <el-descriptions-item label="胜率">
              {{ `${(winningRatio * 100).toFixed(1)} %` }}
            </el-descriptions-item>
            <el-descriptions-item label="盈亏比">{{ earningPerLoss }}</el-descriptions-item>
          </el-descriptions>
          <el-tabs v-model="activeAccount">
            <el-tab-pane
              v-for="item in accountOptions"
              :key="item"
              :name="item"
              :label="item"
            ></el-tab-pane>
          </el-tabs>
          <div class="pt-10">
            <el-descriptions class="margin-top" :column="3">
              <el-descriptions-item label="账户ID">
                {{ accountInfo.accountId }}
              </el-descriptions-item>
              <el-descriptions-item label="初始余额">
                {{ accountInfo.initBalance | formatter }}
              </el-descriptions-item>
              <el-descriptions-item label="当前余额">
                {{ (accountInfo.preBalance + holdingProfit) | formatter }}
              </el-descriptions-item>
              <el-descriptions-item label="持仓盈亏">
                {{ holdingProfit | formatter }}
              </el-descriptions-item>
              <el-descriptions-item label="累计平仓盈亏">
                {{ accountInfo.accCloseProfit | formatter }}
              </el-descriptions-item>
              <el-descriptions-item label="累计手续费">
                {{ accountInfo.accCommission | formatter }}
              </el-descriptions-item>
              <el-descriptions-item label="合计盈亏">
                {{
                  (accountInfo.accCloseProfit - accountInfo.accCommission + holdingProfit)
                    | formatter
                }}
              </el-descriptions-item>
            </el-descriptions>
            <el-tabs v-model="moduleTab" :stretch="true">
              <el-tab-pane name="holding" label="模组持仓"></el-tab-pane>
              <el-tab-pane name="dealRecord" label="交易历史"></el-tab-pane>
            </el-tabs>
            <div class="table-wrapper">
              <el-table v-show="moduleTab === 'holding'" :data="holdingPositions" height="100%">
                <el-table-column prop="unifiedSymbol" label="合约" align="center" width="100px">
                  <template slot-scope="scope">{{ scope.row.contract.name }}</template>
                </el-table-column>
                <el-table-column prop="positionDir" label="方向" align="center" width="40px"
                  ><template slot-scope="scope">{{
                    { 2: '多', 3: '空' }[scope.row.positiondirection] || '未知'
                  }}</template>
                </el-table-column>
                <el-table-column prop="position" label="手数" align="center" width="46px" />
                <el-table-column prop="openprice" label="成本价" align="center"></el-table-column>
                <el-table-column prop="lastprice" label="现价" align="center"></el-table-column>
                <el-table-column prop="positionprofit" label="持仓盈亏" align="center" />
                <el-table-column label="操作" align="center" width="50px">
                  <template slot="header">
                    <el-button
                      id="editPosition"
                      class="compact"
                      title="调整持仓"
                      size="mini"
                      icon="el-icon-edit"
                      @click="positionFormVisible = true"
                    ></el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-table
                ref="dealTbl"
                v-show="moduleTab === 'dealRecord'"
                :data="accountDealRecords"
                height="100%"
              >
                <el-table-column prop="contractName" label="合约" align="center" width="100px" />
                <el-table-column prop="direction" label="方向" align="center" width="40px" />
                <el-table-column prop="volume" label="手数" align="center" width="46px" />
                <el-table-column prop="openPrice" label="开仓价" align="center" />
                <el-table-column prop="closePrice" label="平仓价" align="center" />
                <el-table-column prop="dealProfit" label="平仓盈亏" align="center" width="70px" />
                <el-table-column prop="tradingDay" label="交易日" align="center" width="100px" />
              </el-table>
            </div>
          </div>
        </div>
      </div>
      <div class="kline-wrapper">
        <div class="kline-header">模组当前引用的K线数据（模组仅缓存最近的500根K线数据）</div>
        <div>
          <el-select class="ml-10 mt-5" v-model="unifiedSymbolOfChart" placeholder="请选择合约">
            <el-option
              v-for="(item, i) in contractOptions"
              :key="i"
              :label="item"
              :value="item"
            ></el-option>
          </el-select>
          <el-select class="ml-10 mt-5" v-model="indicator" placeholder="请选择指标">
            <el-option
              v-for="(item, i) in indicatorOptions"
              :key="i"
              :label="item"
              :value="item"
            ></el-option>
          </el-select>
          <el-select class="ml-10 mt-5 mr-10" v-model="paneId" placeholder="请选择绘图位置">
            <el-option :key="1" label="主图" value="candle_pane" />
            <el-option :key="2" label="副图1" value="pane1" />
            <el-option :key="3" label="副图2" value="pane2" />
            <el-option :key="4" label="副图3" value="pane3" />
          </el-select>
          <el-button icon="el-icon-plus" title="绘制指标" @click.native="addIndicator"></el-button>
          <el-button
            icon="el-icon-minus"
            title="移除指标"
            @click.native="removeIndicator"
          ></el-button>
          <el-button
            :icon="`${holdingVisibleOnChart ? 'el-icon-data-board' : 'el-icon-data-line'}`"
            :title="`${holdingVisibleOnChart ? '隐藏持仓线' : '显示持仓线'}`"
            @click.native="holdingVisibleOnChart = !holdingVisibleOnChart"
          ></el-button>
        </div>
        <div
          id="module-k-line"
          class="kline-body"
          v-loading.lock="loading"
          element-loading-background="rgba(0, 0, 0, 0.5)"
        ></div>
      </div>
    </div>
  </el-dialog>
</template>
<script>
import ModulePositionForm from './ModulePositionForm.vue'
import ModulePerformanceForm from './ModulePerformance.vue'
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import simpleVal from '@/lib/indicator/simple-value'
import moduleApi from '@/api/moduleApi'
import KLineUtils from '@/utils/kline-utils.js'
import { jStat } from 'jstat'

import { BarField, PositionField, TradeField } from '@/lib/xyz/redtorch/pb/core_field_pb'

const makeHoldingSegment = (deal) => {
  return {
    name: 'segment',
    points: [
      { timestamp: deal.openTrade.tradetimestamp, value: deal.openPrice },
      { timestamp: deal.closeTrade.tradetimestamp, value: deal.closePrice }
    ],
    lock: true,
    styles: {
      line: {
        color:
          deal.direction === '多' ? '#ff0000' : deal.direction === '空' ? '#00ff00' : '#0000ff',
        size: 2
      }
    }
  }
}

export default {
  components: {
    ModulePositionForm,
    ModulePerformanceForm
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    module: {
      type: Object,
      default: () => {}
    },
    moduleRuntimeSrc: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      positionFormVisible: false,
      performanceVisible: false,
      holdingVisibleOnChart: false,
      moduleTab: 'holding',
      activeAccount: '',
      dealRecords: [],
      barDataMap: {},
      chart: null,
      loading: false,
      moduleRuntime: '',
      unifiedSymbolOfChart: '',
      paneId: '',
      indicator: '',
      indicatorMap: {},
      timer: '',
      isManualUpdate: true
    }
  },
  filters: {
    formatter: function (val) {
      return typeof val === 'number' ? val.toFixed(0) : val
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.moduleRuntime = this.moduleRuntimeSrc
        this.activeAccount = this.accountOptions[0]
        setTimeout(() => {
          this.initChart()
          this.refresh()
        }, 100)
      }
    },
    isManualUpdate(val) {
      if (val) {
        clearTimeout(this.timer)
      } else {
        const autoUpdate = () => {
          this.refresh()
          this.timer = setTimeout(autoUpdate, 5000)
        }
        this.timer = setTimeout(autoUpdate, 5000)
      }
    },
    moduleTab: function (val) {
      if (val === 'dealRecord') {
        setTimeout(() => {
          let table = this.$refs.dealTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        }, 50)
      }
    },
    unifiedSymbolOfChart: function (val) {
      if (val) {
        this.updateChart()
      }
    },
    holdingVisibleOnChart: function (val) {
      if (val) {
        this.visualizeTradeRecords()
      } else {
        if (this.chart) {
          this.chart.removeShape()
        }
      }
    }
  },
  computed: {
    symbolOptions() {
      return Object.keys(this.barDataMap) || []
    },
    accountOptions() {
      if (!this.moduleRuntime) return []
      return Object.keys(this.moduleRuntime.accountRuntimeDescriptionMap)
    },
    accountInfo() {
      if (!this.activeAccount) return {}
      return this.moduleRuntime.accountRuntimeDescriptionMap[this.activeAccount]
    },
    accountSettings() {
      if (!this.activeAccount) return {}
      return this.module.moduleAccountSettingsDescription.find(
        (item) => item.accountGatewayId === this.activeAccount
      )
    },
    accountDealRecords() {
      if (!this.activeAccount) return []
      return this.dealRecords.filter((item) => item.moduleAccountId === this.activeAccount)
    },
    holdingProfit() {
      if (!this.activeAccount) return 0
      return this.holdingPositions.map((item) => item.positionprofit).reduce((a, b) => a + b, 0)
    },
    totalProfit() {
      if (!this.moduleRuntime) return 0
      return Object.values(this.moduleRuntime.accountRuntimeDescriptionMap)
        .map((accountInfo) => {
          const holdingProfit = accountInfo.positionDescription.logicalPositions
            .map((data) => PositionField.deserializeBinary(data).toObject())
            .filter((item) => item.position > 0)
            .map((item) => item.positionprofit)
            .reduce((a, b) => a + b, 0)
          return accountInfo.accCloseProfit - accountInfo.accCommission + holdingProfit
        })
        .reduce((a, b) => a + b, 0)
    },
    winningRatio() {
      if (!this.dealRecords.length) return 0
      const numberOfWinning = this.dealRecords.filter((item) => item.dealProfit > 0).length || 0
      return (numberOfWinning / this.dealRecords.length).toFixed(3)
    },
    earningPerLoss() {
      if (!this.dealRecords.length) return 'N/A'
      const winningDeals = this.dealRecords.filter((item) => item.dealProfit > 0)
      const lossDeals = this.dealRecords.filter((item) => item.dealProfit <= 0)
      const avgProfit = winningDeals.length
        ? (jStat.sum(winningDeals.map((item) => item.dealProfit)) / winningDeals.length).toFixed(0)
        : '0'
      const avgLoss = lossDeals.length
        ? Math.abs(jStat.sum(lossDeals.map((item) => item.dealProfit)) / lossDeals.length).toFixed(
            0
          )
        : '0'
      return `${avgProfit} : ${avgLoss}`
    },
    holdingPositions() {
      if (!this.activeAccount) return []
      const positions = this.accountInfo.positionDescription.logicalPositions.map((data) =>
        PositionField.deserializeBinary(data).toObject()
      )
      return positions.filter((item) => item.position > 0)
    },
    contractOptions() {
      return Object.keys(this.barDataMap)
    },
    indicatorOptions() {
      if (!this.moduleRuntime.indicatorMap) return []
      return Object.keys(this.moduleRuntime.indicatorMap).filter(
        (key) => this.moduleRuntime.indicatorMap[key].unifiedSymbol === this.unifiedSymbolOfChart
      )
    }
  },
  created() {
    window.addEventListener('resize', () => {
      if (this.chart) {
        this.chart.resize()
      }
    })
  },
  methods: {
    refresh() {
      this.loadRuntime()
      this.loadDealRecord()
    },
    loadRuntime() {
      moduleApi.getModuleRuntime(this.module.moduleName).then((result) => {
        this.moduleRuntime = result
        this.barDataMap = {}
        Object.keys(result.barDataMap).forEach((key) => {
          this.barDataMap[key] = result.barDataMap[key]
            .map((data) => BarField.deserializeBinary(data).toObject())
            .map(KLineUtils.createFromBar)
        })
        this.updateChart()
        this.updateIndicator()
      })
    },
    loadDealRecord() {
      moduleApi.getModuleDealRecords(this.module.moduleName).then((result) => {
        this.dealRecords = result.map((item) => {
          item.openTrade = TradeField.deserializeBinary(item.openTrade).toObject()
          item.closeTrade = TradeField.deserializeBinary(item.closeTrade).toObject()
          item.volume = item.closeTrade.volume
          item.direction = { 1: '多', 2: '空' }[item.openTrade.direction]
          item.openPrice = item.openTrade.price
          item.closePrice = item.closeTrade.price
          item.tradingDay = item.closeTrade.tradingday
          return item
        })

        this.$nextTick(() => {
          let table = this.$refs.dealTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        })
      })
    },
    initChart() {
      const kLineChart = init(`module-k-line`)
      kLineChart.addTechnicalIndicatorTemplate(volumePure)
      kLineChart.createTechnicalIndicator('CJL', false, { id: 'pane1' })
      kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))
      this.chart = kLineChart
    },
    async onSave() {
      setTimeout(this.refresh, 500)
    },
    updateChart() {
      if (this.unifiedSymbolOfChart) {
        this.chart.removeTechnicalIndicator('candle_pane')
        this.chart.applyNewData(this.barDataMap[this.unifiedSymbolOfChart])
      }
    },
    addIndicator() {
      if (!this.indicator) return
      if (!this.paneId) return
      const indicatorData = this.moduleRuntime.indicatorMap[this.indicator]
      const colorIndex = Object.keys(this.moduleRuntime.indicatorMap).indexOf(this.indicator)
      this.indicatorMap[this.indicator] = this.paneId
      this.renderIndicator(this.indicator, this.paneId, indicatorData, colorIndex)
    },
    removeIndicator() {
      if (!this.indicator) return
      if (!this.paneId) return
      this.chart.removeTechnicalIndicator(
        this.indicatorMap[this.indicator],
        'VAL_' + this.indicator
      )
      delete this.indicatorMap[this.indicator]
      console.log('移除指标', this.indicator)
    },
    updateIndicator() {
      Object.values(this.indicatorMap).forEach((pane) => this.chart.removeTechnicalIndicator(pane))
      Object.keys(this.indicatorMap).forEach((indicatorName) => {
        const indicatorData = this.moduleRuntime.indicatorMap[indicatorName]
        const colorIndex = Object.keys(this.moduleRuntime.indicatorMap).indexOf(indicatorName)
        this.renderIndicator(
          indicatorName,
          this.indicatorMap[indicatorName],
          indicatorData,
          colorIndex
        )
      })
    },
    renderIndicator(name, paneId, indicatorData, colorIndex) {
      this.chart.addTechnicalIndicatorTemplate(simpleVal(name, indicatorData, colorIndex))
      this.chart.createTechnicalIndicator('VAL_' + name, true, {
        id: paneId
      })
    },
    visualizeTradeRecords() {
      this.chart.removeShape()
      this.dealRecords
        .filter((deal) => deal.closeTrade.contract.unifiedsymbol === this.unifiedSymbolOfChart)
        .forEach((i) => {
          this.chart.createShape(makeHoldingSegment(i), 'candle_pane')
        })
    },
    close() {
      clearTimeout(this.timer)
      Object.assign(this.$data, this.$options.data())
      dispose('module-k-line')
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.table-wrapper {
  height: calc(100vh - 382px);
}
.kline-wrapper {
  width: 100%;
  border-left: 1px solid;
  display: flex;
  flex-direction: column;
}
#module-k-line {
  flex: 1;
}
.module-rt-wrapper {
  display: flex;
  border-top: 1px solid;
  border-bottom: 1px solid;
}
.side-panel {
  min-width: 520px;
  flex: 1;
}
.cell-content {
  position: absolute;
  bottom: 0;
}
.kline-header {
  margin-left: 8px;
}
.text-wrapper {
  box-sizing: content-box;
  width: 100%;
}
</style>
<style>
.el-tabs__header {
  margin: 0;
}
.el-dialog.is-fullscreen {
  overflow: hidden;
}
.description-wrapper {
  padding: 10px;
  height: calc(100% - 20px);
}
</style>
