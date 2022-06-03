<template>
  <el-dialog title="模组运行状态" :visible="visible" fullscreen class="flex-col" @close="close">
    <ModulePositionForm
      :visible.sync="positionFormVisible"
      :moduleAccount="accountSettings"
      :moduleName="module.moduleName"
      @save="onSave"
    />
    <div class="module-rt-wrapper">
      <div class="side-panel">
        <div class="description-wrapper">
          <el-descriptions class="margin-top" title="模组信息" :column="3">
            <template slot="extra">
              <el-button class="compact mb-10" icon="el-icon-refresh" @click="refresh"
                >刷新数据</el-button
              >
              <!-- <el-button class="compact mb-10" icon="el-icon-info" @click="refresh"
                >更多统计</el-button
              > -->
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
            <el-descriptions-item label="总盈亏">{{ totalProfit }}</el-descriptions-item>
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
                {{ accountInfo.initBalance }}
              </el-descriptions-item>
              <el-descriptions-item label="当前余额">
                {{ accountInfo.preBalance + holdingProfit }}
              </el-descriptions-item>
              <el-descriptions-item label="持仓盈亏">
                {{ holdingProfit }}
              </el-descriptions-item>
              <el-descriptions-item label="累计平仓盈亏">
                {{ accountInfo.accCloseProfit }}
              </el-descriptions-item>
              <el-descriptions-item label="累计手续费">
                {{ accountInfo.accCommission }}
              </el-descriptions-item>
              <el-descriptions-item label="合计盈亏">
                {{ accountInfo.accCloseProfit - accountInfo.accCommission + holdingProfit }}
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
        <div class="kline-header">模组当前引用的K线数据</div>

        <el-tabs v-model="activeTab" stretch @tab-click="updateChart">
          <el-tab-pane :label="symbol" :name="symbol" v-for="(symbol, i) in symbolOptions" :key="i">
          </el-tab-pane>
        </el-tabs>
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
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import moduleApi from '@/api/moduleApi'
import { KLineUtils } from '@/utils.js'
import { jStat } from 'jstat'

import { BarField, PositionField, TradeField } from '@/lib/xyz/redtorch/pb/core_field_pb'

const convertDataRef = (dataRefSrcMap) => {
  const resultMap = {}
  Object.keys(dataRefSrcMap).forEach((k) => {
    if (!(dataRefSrcMap[k] instanceof Array)) {
      return
    }
    resultMap[k] = dataRefSrcMap[k]
      .map((byteData) => BarField.deserializeBinary(byteData).toObject())
      .map(KLineUtils.createFromBar)
  })
  return resultMap
}

const makeShape = (deal) => {
  return {
    name: 'segment',
    points: [
      { timestamp: deal.openTimestamp, value: deal.openPrice },
      { timestamp: deal.closeTimestamp, value: deal.closePrice }
    ],
    lock: true,
    styles: {
      line: {
        color:
          deal.direction === 'PD_Long' ? '#f00' : deal.direction === 'PD_Short' ? '#0f0' : '#00f',
        size: 2
      }
    }
  }
}

export default {
  components: {
    ModulePositionForm
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
      moduleTab: 'holding',
      activeTab: '',
      activeAccount: '',
      dealRecords: [],
      barDataMap: {},
      chart: null,
      loading: false,
      moduleRuntime: ''
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.moduleRuntime = this.moduleRuntimeSrc
        this.activeAccount = this.accountOptions[0]
        this.refresh()
        console.log(this.holdingPositions)
      }
    },
    moduleTab: function (val) {
      if (val === 'dealRecord') {
        setTimeout(() => {
          let table = this.$refs.dealTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        }, 50)
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
      const avgProfit = jStat.sum(winningDeals.map((item) => item.dealProfit))
      const avgLoss = jStat.sum(lossDeals.map((item) => item.dealProfit))
      return `${avgProfit.toFixed(1)} : ${Math.abs(avgLoss).toFixed(1)}`
    },
    holdingPositions() {
      if (!this.activeAccount) return []
      const positions = this.accountInfo.positionDescription.logicalPositions.map((data) =>
        PositionField.deserializeBinary(data).toObject()
      )
      return positions.filter((item) => item.position > 0)
    }
  },
  methods: {
    refresh() {
      this.loadRuntime()
      this.loadDealRecord()
    },
    loadRuntime() {
      moduleApi.getModuleRuntime(this.module.moduleName).then((result) => {
        this.moduleRuntime = result
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
    async loadRefData() {
      const kLineChart = init(`module-k-line`)
      kLineChart.addTechnicalIndicatorTemplate(volumePure)
      kLineChart.createTechnicalIndicator('CJL', false)
      kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))
      this.chart = kLineChart
      this.barDataMap = await this.loadData(this.moduleName, new Date().getTime())
      this.activeTab = this.symbolOptions.length ? this.symbolOptions[0] : ''
      this.updateChart()
    },
    async onSave() {
      setTimeout(this.refresh, 500)
    },
    updateChart() {
      this.chart.clearData()
      if (!this.barDataMap[this.activeTab] || !this.barDataMap[this.activeTab].length) {
        this.$message.warning('数据为空')
        return
      }

      this.chart.applyNewData(this.barDataMap[this.activeTab])
      this.chart.loadMore(async (timestamp) => {
        console.log('加载更多数据')
        if (typeof timestamp !== 'number') {
          console.warn('忽略一个不是数值的时间戳: ' + timestamp)
          return
        }
        await new Promise((r) => setTimeout(r, 1000))
        const dataSet = await this.loadData(this.moduleName, timestamp)
        const deltaData = dataSet[this.activeTab]
        if (this.chart) {
          this.chart.applyMoreData(deltaData || [], !!deltaData)
          Object.keys(dataSet)
            .filter((i) => !!deltaData[i])
            .forEach((i) => {
              this.barDataMap[i] = deltaData[i].concat(this.barDataMap[i] || [])
            })
          this.tradeVisualize()
        }
      })
      this.tradeVisualize()
    },
    async loadData(moduleName, timestamp) {
      this.loading = true
      try {
        const result = await moduleApi.getModuleDataRef(moduleName, timestamp)
        return convertDataRef(result)
      } catch (e) {
        this.$message.error(e.message)
        throw new Error(e)
      } finally {
        this.loading = false
      }
    },
    tradeVisualize() {
      this.chart.removeShape()
      this.dealRecords.forEach((i, idx) => {
        this.chart.createShape(makeShape(i), 'panel' + idx)
      })
    },
    close() {
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
  height: 100%;
  width: 100%;
  border-left: 1px solid;
}
.module-rt-wrapper {
  display: flex;
  height: 100%;
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
.kline-body {
  height: calc(100% - 56px);
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