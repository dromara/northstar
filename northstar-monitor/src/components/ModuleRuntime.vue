<template>
  <el-dialog title="模组运行状态" :visible="visible" fullscreen class="flex-col" @close="close">
    <ModulePositionForm
      v-if="module.usage !== 'PLAYBACK'"
      :visible.sync="positionFormVisible"
      :contractOptions="moduleBindedContracts"
      :moduleName="module.moduleName"
      @save="onSave"
    />
    <ModulePerformancePopup
      :visible.sync="performanceVisible"
      :moduleInitBalance="moduleInfo.initBalance"
      :moduleDealRecords="dealRecords"
    />
    <div class="module-rt-wrapper">
      <div class="side-panel">
        <div class="side-panel_content">
          <el-descriptions class="margin-top panel-header" :column="`${isMobile ? 2 : 3}`">
            <template slot="title">
              模组用途
              <el-tag
                class="ml-10"
                :type="{ PLAYBACK: 'info', UAT: 'warning', PROD: '' }[module.usage]"
                effect="dark"
                >{{ { PLAYBACK: '回测', UAT: '模拟盘', PROD: '实盘' }[module.usage] }}</el-tag
              >
            </template>
            <template slot="extra">
              <el-switch
                class="ml-10"
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
                v-if="!isMobile"
                class="compact mb-10 ml-10"
                icon="el-icon-download"
                @click="exportDealRecord"
                >导出交易</el-button
              >
            </template>
          </el-descriptions>
          <el-tabs v-model="infoTab" :stretch="true">
            <el-tab-pane name="moduleInfo" label="模组信息">
              <div class="description-wrapper">
                <el-descriptions class="margin-top" :column="`${isMobile ? 2 : 3}`">
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
                  <el-descriptions-item label="当前余额">
                    {{
                      (moduleInfo.initBalance +
                        moduleInfo.accCloseProfit -
                        moduleInfo.accCommission +
                        holdingProfit)
                        | formatter
                    }}
                  </el-descriptions-item>
                  <el-descriptions-item label="可用金额">
                    {{ moduleInfo.availableAmount | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="占用金额">
                    {{ (moduleInfo.initBalance +
                        moduleInfo.accCloseProfit -
                        moduleInfo.accCommission +
                        holdingProfit) - moduleInfo.availableAmount | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="初始金额">
                    {{ moduleInfo.initBalance | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="总盈亏">
                    {{ totalProfit | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="总平仓盈亏">
                    {{ moduleInfo.accCloseProfit | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="总手续费">
                    {{ moduleInfo.accCommission | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="总持仓盈亏">
                    {{ holdingProfit | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="交易笔数">
                    {{ dealRecords.length || 0 }}
                  </el-descriptions-item>
                  <el-descriptions-item label="胜率">
                    {{ `${(winningRatio * 100).toFixed(1)} %` }}
                  </el-descriptions-item>
                  <el-descriptions-item label="盈亏比">{{ earningPerLoss }}</el-descriptions-item>
                  <el-descriptions-item label="年化收益率">
                    {{ `${moduleInfo.annualizedRateOfReturn * 100 | formatter}%` }}
                  </el-descriptions-item>
                  <el-descriptions-item label="最大回撤">
                    {{ moduleInfo.maxDrawback | formatter }}
                  </el-descriptions-item>
                  <el-descriptions-item label="最大回撤比">
                    {{ `${Math.ceil(moduleInfo.maxDrawbackPercentage * 100 || 0)}%` }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>
            <el-tab-pane name="accountInfo" label="账户信息">
              <el-descriptions v-for="(item,i) in accountInfo" :key="i" column="2">
                <template slot="title">
                  {{item.name}}
                </template>
                <el-descriptions-item label="账户余额">
                  {{ item.balance | formatter }}
                </el-descriptions-item>
                <el-descriptions-item label="可用金额">
                  {{ item.availableAmount | formatter }}
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
            <el-tab-pane name="strategyInfo" label="策略信息">
              <div class="description-wrapper">
                <el-descriptions class="margin-top" column="2">
                  <el-descriptions-item v-for="(item, i) in strategyInfo" :label="item.name" :key="i">
                    <el-popover v-if="(item.value instanceof Array)"
                      placement="right"
                      trigger="click">
                      <el-table :data="item.value" max-height="300px">
                        <el-table-column width="100" property="name" label="描述">
                          <template slot-scope="scope">
                            {{ scope.row.name || scope.$index + 1 }}
                          </template>
                        </el-table-column>
                        <el-table-column width="100" property="value" label="数值"></el-table-column>
                      </el-table>
                      <el-button slot="reference" :disabled="!item.value.length">{{ item.value.length ? '明细' : '无数据'}}</el-button>
                    </el-popover>
                    <span v-else>{{ item.value }}</span>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>
          </el-tabs>
          
          <el-tabs v-model="moduleTab" :stretch="true">
            <el-tab-pane name="holding" label="模组持仓"></el-tab-pane>
            <el-tab-pane name="dealRecord" label="交易历史"></el-tab-pane>
          </el-tabs>
          <div style="height: 1px" />
          <div class="table-wrapper">
            <el-table
              id="modulePositionTbl"
              v-show="moduleTab === 'holding'"
              :data="holdingPositions"
              height="100%"
            >
              <el-table-column prop="unifiedSymbol" label="合约" align="center" width="100px">
                <template slot-scope="scope">{{ scope.row.contract.name }}</template>
              </el-table-column>
              <el-table-column prop="positionDir" label="方向" align="center" width="50px"
                ><template slot-scope="scope">{{
                  { 2: '多', 3: '空' }[scope.row.positiondirection] || '未知'
                }}</template>
              </el-table-column>
              <el-table-column prop="position" label="数量" align="center" min-width="46px" />
              <el-table-column v-if="!isMobile" prop="openprice" label="成本价" align="center">
                <template slot-scope="scope">
                  {{ scope.row.openprice | smartFormatter }}
                </template>
              </el-table-column>
              <el-table-column v-if="!isMobile" prop="lastprice" label="现价" align="center">
                <template slot-scope="scope">
                  {{ scope.row.lastprice | smartFormatter }}
                </template>
              </el-table-column>
              <el-table-column prop="positionprofit" label="持仓盈亏" align="center">
                <template slot-scope="scope">
                  {{ scope.row.positionprofit | formatter }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="module.usage !== 'PLAYBACK'"
                label="操作"
                align="center"
                width="50px"
              >
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
              :data="dealRecords"
              height="100%"
            >
              <el-table-column prop="contractName" label="合约" align="center" width="100px" />
              <el-table-column prop="direction" label="方向" align="center" width="46px" />
              <el-table-column prop="volume" label="数量" align="center" min-width="60px" />
              <el-table-column prop="openPrice" label="开仓价" align="center" />
              <el-table-column prop="closePrice" label="平仓价" align="center" />
              <el-table-column label="平仓盈亏" align="center" width="70px">
                <template slot-scope="scope">
                  {{ scope.row.dealProfit | formatter }}
                </template>
              </el-table-column>
              <el-table-column label="开仓时间" align="center" width="132px">
                <template slot-scope="scope">
                  {{ `${scope.row.openTrade.tradedate} ${scope.row.openTrade.tradetime}` }}
                </template>
              </el-table-column>
              <el-table-column label="平仓时间" align="center" width="132px">
                <template slot-scope="scope">
                  {{ `${scope.row.closeTrade.tradedate} ${scope.row.closeTrade.tradetime}` }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="performance-min">
            <el-button
              v-if="!isMobile"
              class="compact btn-enlarge"
              title="放大盈亏曲线"
              size="mini"
              icon="el-icon-zoom-in"
              @click="performanceVisible = true"
            ></el-button>
            <ModulePerformance
              :visible.sync="performanceVisible"
              :moduleInitBalance="moduleInfo.initBalance"
              :moduleDealRecords="dealRecords"
            />
          </div>
        </div>
      </div>
      <div v-if="!isMobile" class="kline-wrapper">
        <div class="kline-header">
          模组当前引用的K线数据（模组仅缓存最近的{{ module.moduleCacheDataSize }}根K线数据）
        </div>
        <div>
          <el-select class="ml-10 mt-5" v-model="unifiedSymbolOfChart" placeholder="请选择合约">
            <el-option
              v-for="(item, i) in bindedContracts"
              :key="i"
              :label="item"
              :value="item"
            ></el-option>
          </el-select>
          <el-select
            class="ml-10 mt-5"
            v-model="indicator.name"
            filterable
            placeholder="请选择指标"
          >
            <el-option
              v-for="(item, i) in indicatorOptions"
              :key="i"
              :label="item"
              :value="item"
            ></el-option>
          </el-select>
          <el-select
            class="ml-10 mt-5 mr-10"
            v-model="indicator.paneId"
            placeholder="请选择绘图位置"
          >
            <el-option :key="0" label="主图" value="candle_pane" />
            <el-option :key="1" label="副图1" value="pane1" />
            <el-option :key="2" label="副图2" value="pane2" />
            <el-option :key="3" label="副图3" value="pane3" />
            <el-option :key="4" label="副图4" value="pane4" />
            <el-option :key="5" label="副图5" value="pane5" />
          </el-select>
          <el-button icon="el-icon-plus" title="绘制指标" @click.native="addIndicator"></el-button>
          <el-button
            icon="el-icon-minus"
            title="移除指标"
            @click.native="removeIndicator"
          ></el-button>
          <el-popover>
            <el-form>
              <el-form-item label="线粗" size="mini">
                <el-input-number
                  style="width: 100px"
                  v-model="indicator.lineWidth"
                  :min="1"
                  :max="4"
                  @change="updateIndicator"
                />
              </el-form-item>
              <el-form-item label="线形" size="mini">
                <el-select style="width: 100px" v-model="indicator.lineStyle">
                  <el-option value="line" label="折线" key="1"></el-option>
                  <el-option value="bar" label="柱形" key="2"></el-option>
                  <el-option value="circle" label="圆点" key="3"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item style="margin-bottom: 0">
                <el-button style="float: right" type="warning" @click.native="clearIndicators"
                  >清空指标</el-button
                >
              </el-form-item>
            </el-form>
            <el-button
              class="ml-10 mr-10"
              slot="reference"
              icon="el-icon-setting"
              title="指标样式设置"
            ></el-button>
          </el-popover>
          <el-button
            :icon="`${holdingVisibleOnChart ? 'el-icon-data-board' : 'el-icon-data-line'}`"
            :title="`${holdingVisibleOnChart ? '隐藏持仓线' : '显示持仓线'}`"
            @click.native="holdingVisibleOnChart = !holdingVisibleOnChart"
          ></el-button>
          <el-button icon="el-icon-download" title="下载数据" @click.native="exportData"></el-button>
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
import ModulePerformance from './ModulePerformance.vue'
import ModulePerformancePopup from './ModulePerformancePopup.vue'
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import simpleVal from '@/lib/indicator/simple-value'
import moduleApi from '@/api/moduleApi'
import KLineUtils from '@/utils/kline-utils.js'
import { downloadData } from '@/utils/file-utils.js'
import { jStat } from 'jstat'
import { parse } from 'json2csv'
import MediaListener from '@/utils/media-utils'

import { PositionField, TradeField } from '@/lib/xyz/redtorch/pb/core_field_pb'
import moment from 'moment'

const makeHoldingSegment = (deal) => {
  return {
    name: 'segment',
    points: [
      { timestamp: deal.openTrade.tradetimestamp + 60000, value: deal.openPrice },
      { timestamp: deal.closeTrade.tradetimestamp + 60000, value: deal.closePrice }
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
    ModulePerformance,
    ModulePerformancePopup
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
      infoTab: 'moduleInfo',
      moduleTab: 'holding',
      dealRecords: [],
      barDataMap: {},
      chart: null,
      loading: false,
      moduleRuntime: '',
      unifiedSymbolOfChart: '',
      indicator: {
        name: '',
        paneId: 'candle_pane',
        lineWidth: 1,
        lineStyle: 'line'
      },
      indicatorMap: {},
      timer: '',
      isManualUpdate: true,
      isMobile: false
    }
  },
  filters: {
    formatter: function (val) {
      return typeof val === 'number' ? val.toFixed(0) : val
    }
  },
  watch: {
    visible: function (val) {
      if(!localStorage.getItem(`autoUpdate_${this.module.moduleName}`)){
        localStorage.setItem(`autoUpdate_${this.module.moduleName}`, true)
      }
      if (val) {
        this.isManualUpdate = localStorage.getItem(`autoUpdate_${this.module.moduleName}`) === 'true'
        this.unifiedSymbolOfChart = localStorage.getItem(`chartSymbol_${this.module.moduleName}`) || ''
        this.isMobile = this.listener.isMobile()
        this.moduleRuntime = this.moduleRuntimeSrc
        if(this.isMobile){
          this.loadDealRecord()
          return;
        }
        setTimeout(() => {
          this.initChart()
          this.refresh()
        }, 100)
      }
    },
    'indicator.name': function () {
      this.indicator.lineStyle = 'line'
    },
    'dealRecords.length': function () {
      if (this.holdingVisibleOnChart) {
        this.visualizeTradeRecords()
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
      if(this.visible){
        localStorage.setItem(`autoUpdate_${this.module.moduleName}`, val)
      }
    },
    moduleTab: function (val) {
      if (val === 'dealRecord') {
        setTimeout(() => {
          let table = this.$refs.dealTbl
          if (table) {
            table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
          }
        }, 50)
      }
    },
    unifiedSymbolOfChart: function (val) {
      if (val) {
        this.updateChart()
        this.loadIndicators()
        this.holdingVisibleOnChart = false
      }
      if(this.visible){
        localStorage.setItem(`chartSymbol_${this.module.moduleName}`, val)
      }
    },
    holdingVisibleOnChart: function (val) {
      if (val) {
        console.log('显示持仓线')
        this.visualizeTradeRecords()
      } else {
        if (this.chart) {
          this.chart.removeShape()
        }
      }
    }
  },
  computed: {
    bindedContracts() {
      return Object.keys(this.barDataMap) || []
    },
    strategyInfo(){
      return this.moduleRuntime.strategyInfos || []
    },
    moduleInfo(){
      return this.moduleRuntime.moduleAccountRuntime || {}
    },
    accountInfo() {
      return this.moduleRuntime.accountRuntimes || []
    },
    moduleBindedContracts() {
      if(!this.module.moduleAccountSettingsDescription) return []
      return this.module.moduleAccountSettingsDescription.reduce((contracts, mad) => contracts.concat(mad.bindedContracts), [])
    },
    holdingProfit() {
      return this.holdingPositions.map((item) => item.positionprofit).reduce((a, b) => a + b, 0)
    },
    totalProfit() {
      if (!this.moduleRuntime) return 0
      return this.holdingProfit + this.moduleInfo.accCloseProfit - this.moduleInfo.accCommission
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
      if(!this.moduleInfo.positionDescription)  return []
      const positions = this.moduleInfo.positionDescription.logicalPositions.map((data) =>
        PositionField.deserializeBinary(data).toObject()
      )
      return positions.filter((item) => item.position > 0)
    },
    indicatorOptions() {
      if (!this.moduleRuntime.indicatorMap || !this.unifiedSymbolOfChart) return []
      return this.moduleRuntime.indicatorMap[this.unifiedSymbolOfChart]
    }
  },
  created() {
    window.addEventListener('resize', () => {
      if (this.chart) {
        this.chart.resize()
      }
    })
    this.listener = new MediaListener(() => {
      this.isMobile = this.listener.isMobile()
    })
  },
  beforeDestroy() {
    this.listener.destroy()
    clearTimeout(this.timer)
  },
  methods: {
    refresh() {
      this.loadRuntime()
      this.loadDealRecord()
    },
    loadRuntime() {
      moduleApi.getModuleRuntime(this.module.moduleName).then((result) => {
        this.moduleRuntime = result
        this.barDataMap = result.dataMap
        this.updateChart()
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
          if (table) {
            table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
          }
        })
      })
    },
    exportData(){

      const fields = [
        'time',
        'open',
        'high',
        'low',
        'close',
        'volume',
        'openInterest',
        ...(this.indicatorOptions || []),
        'holding'
      ]
      Object.keys(this.barDataMap).map(symbol => {
        const dataList = this.barDataMap[symbol].map(data => {
          const timeFrameObj = fields.reduce((obj, field) => {
            obj[field] = field === 'time' ? moment(data['timestamp']).format('yyyyMMDD HH:mm') : data[field]
            return obj
          }, {})
          timeFrameObj['holding'] = 0
          this.dealRecords.forEach(deal => {
            const openTime = deal.openTrade.tradetimestamp
            const closeTime = deal.closeTrade.tradetimestamp
            if(data['timestamp'] > openTime && data['timestamp'] < closeTime){
              const factor = deal.direction === '多' ? 1 : -1
              timeFrameObj['holding'] += factor * deal.volume
            }
          })
          return timeFrameObj
        })
        const csvData = parse(dataList, {fields})
        downloadData(csvData, `${symbol}_数据.csv`, 'text/csv,charset=UTF-8')
      })
    },
    exportDealRecord() {
      const fields = [
        '合约名称',
        '持仓方向',
        '开仓价',
        '开仓时间',
        '平仓价',
        '平仓时间',
        '数量',
        '交易盈亏'
      ]
      const data = this.dealRecords.map((item) => {
        return {
          合约名称: item.contractName,
          持仓方向: item.direction,
          开仓价: item.openPrice,
          开仓时间: `${item.openTrade.tradedate} ${item.openTrade.tradetime}`,
          平仓价: item.closePrice,
          平仓时间: `${item.closeTrade.tradedate} ${item.closeTrade.tradetime}`,
          数量: item.volume,
          交易盈亏: item.dealProfit
        }
      })
      const csvData = parse(data, { fields })
      downloadData(csvData, `${this.module.moduleName}_交易历史.csv`, 'text/csv,charset=UTF-8')
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
      if (this.unifiedSymbolOfChart && this.chart) {
        this.chart.clearData()
        this.chart.applyNewData(this.barDataMap[this.unifiedSymbolOfChart])
      }
    },
    addIndicator() {
      if (!this.indicator.name) return
      this.indicatorMap[this.indicator.name] = Object.assign({}, this.indicator)
      const indicatorMap = JSON.parse(JSON.stringify(this.indicatorMap))
      this.clearIndicators()
      this.indicatorMap = indicatorMap
      this.saveIndicators()
      this.loadIndicators()
    },
    removeIndicator() {
      if (!this.indicator.name) return
      this.chart.removeTechnicalIndicator(this.indicator.paneId, 'VAL_' + this.indicator.name)
      delete this.indicatorMap[this.indicator.name]
      this.saveIndicators()
    },
    renderIndicator(indicator) {
      const colorIndex = Object.keys(this.indicatorMap).indexOf(indicator.name)
      this.chart.addTechnicalIndicatorTemplate(simpleVal(indicator, colorIndex))
      this.chart.createTechnicalIndicator('VAL_' + indicator.name, true, {
        id: indicator.paneId
      })
      this.chart.resize()
    },
    updateIndicator() {
      this.indicatorMap[this.indicator.name] = this.indicator
      const override = {
        name: 'VAL_' + this.indicator.name,
        styles: {
          margin: {
            top: 0.2,
            bottom: 0.1
          },
          line: {
            size: this.indicator.lineWidth
          }
        }
      }
      this.chart.overrideTechnicalIndicator(override, this.indicator.paneId)
      this.saveIndicators()
    },
    visualizeTradeRecords() {
      this.chart.removeShape()
      this.dealRecords
        .filter((deal) => deal.contractName === this.unifiedSymbolOfChart)
        .filter((deal) => {
          const dealTime = deal.closeTrade.tradetimestamp
          const dataHeadTime = this.barDataMap[this.unifiedSymbolOfChart].length
            ? this.barDataMap[this.unifiedSymbolOfChart][0]['timestamp']
            : dealTime
          return dealTime > dataHeadTime
        })
        .forEach((i) => {
          this.chart.createShape(makeHoldingSegment(i), 'candle_pane')
        })
    },
    clearIndicators() {
      Object.keys(this.indicatorMap).forEach((indicatorName) => {
        this.indicator = this.indicatorMap[indicatorName]
        this.removeIndicator()
      })
    },
    loadIndicators() {
      const dataStr =
        localStorage.getItem(`indicatorMap_${this.module.moduleName}_${this.unifiedSymbolOfChart}`) ||
        '{}'
      this.indicatorMap = JSON.parse(dataStr)
      for (let i = 1; i < 6; i++) {
        const paneId = 'pane' + i
        Object.keys(this.indicatorMap).forEach((indicatorName) => {
          if (
            this.moduleRuntime.indicatorMap[this.unifiedSymbolOfChart].indexOf(indicatorName) < 0
          ) {
            return
          }
          this.indicator = Object.assign({}, this.indicatorMap[indicatorName])
          if (this.indicator.paneId === paneId || this.indicator.paneId === 'candle_pane') {
            this.renderIndicator(this.indicator)
          }
        })
      }
    },
    saveIndicators() {
      localStorage.setItem(
        `indicatorMap_${this.module.moduleName}_${this.unifiedSymbolOfChart}`,
        JSON.stringify(this.indicatorMap)
      )
    },
    close() {
      this.saveIndicators()
      clearTimeout(this.timer)
      try{
        dispose('module-k-line')
      } catch (e){
        console.error(e)
      }
      this.$emit('update:visible', false)
      setTimeout(() => {
        Object.assign(this.$data, this.$options.data())
      }, 500)
    }
  }
}
</script>

<style scoped>
.side-panel {
    padding: 10px;
    flex: 1;
  }

.table-wrapper {
  flex: 1;
  min-height: 150px;
}
.performance-min {
  position: relative;
  height: 280px;
}
@media screen and (max-height: 600px) {
  .performance-min {
    height: 0px;
  }
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
  height: calc(100vh - 80px);
  display: flex;
  border-top: 1px solid;
  border-bottom: 1px solid;
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
.btn-enlarge {
  position: absolute;
  right: 0;
  margin: 10px 0;
  z-index: 999;
}
.el-tabs__header {
  margin: 0;
}
.el-dialog.is-fullscreen {
  overflow: hidden;
}
.side-panel_content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 桌面端样式 */
@media screen and (min-width: 661px) {
  .side-panel {
    min-width: 520px;
    flex: 1;
  }
  .description-wrapper {
    max-height: 250px;
    overflow: auto;
  }
}

/* 移动端样式 */
@media screen and (max-width: 660px) {
  .side-panel {
    width: 100%;
    padding: 10px 0;
    flex: 1;
  }
  .panel-header{
    height: 30px;
    min-height: 30px;
    overflow: hidden;
  }
  .description-wrapper {
    max-height: 180px;
    overflow: auto;
  }
}
</style>
