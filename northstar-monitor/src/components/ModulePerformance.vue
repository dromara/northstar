<template>
  <el-dialog title="模组透视" :visible.sync="dialogVisible" fullscreen>
    <ModulePositionForm :visible.sync="positionFormVisible" :data="curPosition" @save="onSave" />
    <div class="module-perf-wrapper">
      <div class="side-panel">
        <div class="basic-info">
          <el-form inline>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue label="模组名称" label-width="60px" :value="moduleName" />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue label="账户ID" label-width="60px" :value="accountId" />
              </el-col>
              <el-col span="8"
                ><div class="cell-content">
                  <el-button class="compact" icon="el-icon-refresh" @click="init"
                    >刷新数据</el-button
                  >
                </div></el-col
              >
            </el-row>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="账户总额"
                  label-width="60px"
                  :value="parseInt(accountBalance)"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="占用均额"
                  label-width="60px"
                  title="采用交易所保证金的1.5倍估算"
                  :value="parseInt(avgOccupiedAmount)"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue label="模组状态" label-width="60px" :value="positionState" />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="平仓盈亏"
                  label-width="60px"
                  :value="parseInt(totalCloseProfit) || '0'"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="持仓盈亏"
                  label-width="60px"
                  :value="parseInt(totalPositionProfit) || '0'"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="总盈亏"
                  label-width="50px"
                  title="注意：手续费不算在总盈亏内"
                  :value="parseInt(totalCloseProfit + totalPositionProfit) || '0'"
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="近五次交易平均盈亏"
                  label-width="140px"
                  title="注意：手续费不算在盈亏内"
                  :value="
                    meanProfitOf5Transactions ? parseInt(meanProfitOf5Transactions) : '数据不足'
                  "
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="近五次交易胜率"
                  label-width="120px"
                  :value="
                    winningRateOf5Transactions < 0
                      ? '数据不足'
                      : parseInt(winningRateOf5Transactions * 100) + ' %'
                  "
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="近十次交易平均盈亏"
                  label-width="140px"
                  title="注意：手续费不算在盈亏内"
                  :value="
                    meanProfitOf10Transactions ? parseInt(meanProfitOf10Transactions) : '数据不足'
                  "
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="近十次交易胜率"
                  label-width="120px"
                  :value="
                    winningRateOf10Transactions < 0
                      ? '数据不足'
                      : parseInt(winningRateOf10Transactions * 100) + ' %'
                  "
                />
              </el-col>
            </el-row>
          </el-form>
        </div>
        <div>
          <el-tabs v-model="moduleTab" :stretch="true">
            <el-tab-pane name="holding" label="模组持仓"></el-tab-pane>
            <el-tab-pane name="dealRecord" label="交易历史"></el-tab-pane>
            <el-tab-pane name="tradeRecord" label="原始成交"></el-tab-pane>
          </el-tabs>
        </div>
        <div class="table-wrapper">
          <el-table v-show="moduleTab === 'holding'" :data="holdingPositions" height="100%">
            <el-table-column prop="unifiedSymbol" label="合约" align="center" width="100px">
              <template slot-scope="scope">{{ scope.row.unifiedSymbol.split('@')[0] }}</template>
            </el-table-column>
            <el-table-column prop="positionDir" label="方向" align="center" width="40px"
              ><template slot-scope="scope">{{
                { PD_Long: '多', PD_Short: '空' }[scope.row.positionDir] || '未知'
              }}</template></el-table-column
            >
            <el-table-column
              prop="volume"
              label="手数"
              align="center"
              width="30px"
            ></el-table-column>
            <el-table-column prop="openPrice" label="成本价" align="center"></el-table-column>
            <el-table-column prop="stopLossPrice" label="止损价" align="center"></el-table-column>
            <el-table-column label="操作" align="center">
              <template slot="header">
                <el-button
                  class="compact"
                  title="新建持仓"
                  size="mini"
                  icon="el-icon-plus"
                  @click="createPosition"
                ></el-button>
              </template>
              <template slot-scope="scope">
                <el-button
                  class="compact"
                  title="修改持仓"
                  size="mini"
                  icon="el-icon-edit"
                  @click="editPosition(scope.row)"
                ></el-button>
                <el-popconfirm class="ml-5" title="确定移除吗？" @confirm="delPosition(scope.row)">
                  <el-button
                    class="compact"
                    title="移除持仓"
                    size="mini"
                    slot="reference"
                    icon="el-icon-delete"
                  ></el-button>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-table
            ref="dealTbl"
            v-show="moduleTab === 'dealRecord'"
            :data="dealRecords"
            height="100%"
          >
            <el-table-column
              prop="contractName"
              label="合约"
              align="center"
              width="100px"
            ></el-table-column>
            <el-table-column prop="direction" label="方向" align="center" width="40px">
              <template slot-scope="scope">{{
                { PD_Long: '多', PD_Short: '空' }[scope.row.direction] || '未知'
              }}</template>
            </el-table-column>
            <el-table-column
              prop="volume"
              label="手数"
              align="center"
              width="30px"
            ></el-table-column>
            <el-table-column prop="openPrice" label="开仓价" align="center"></el-table-column>
            <el-table-column prop="closePrice" label="平仓价" align="center"></el-table-column>
            <el-table-column prop="closeProfit" label="平仓盈亏" align="center"></el-table-column>
            <el-table-column prop="tradingDay" label="交易日" align="center"></el-table-column>
          </el-table>
          <el-table
            ref="tradeTbl"
            v-show="moduleTab === 'tradeRecord'"
            :data="tradeRecords"
            height="100%"
            max-height="100%"
          >
            <el-table-column
              prop="contractName"
              label="合约"
              align="center"
              width="100px"
            ></el-table-column>
            <el-table-column prop="operation" label="操作" align="center"> </el-table-column>
            <el-table-column prop="volume" label="手数" align="center"></el-table-column>
            <el-table-column prop="price" label="成交价" align="center"></el-table-column>
            <el-table-column prop="tradingDay" label="成交时间" align="center" width="140px">
              <template slot-scope="scope">
                {{ new Date(scope.row.actionTime).toLocaleString('zh', { hour12: false }) }}
              </template>
            </el-table-column>
          </el-table>
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
import ReadonlyFieldValue from './ReadonlyFieldValue.vue'
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import moduleApi from '@/api/moduleApi'
import { mapGetters } from 'vuex'
import { KLineUtils } from '@/utils.js'

import { BarField } from '@/lib/xyz/redtorch/pb/core_field_pb'

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
    ModulePositionForm,
    ReadonlyFieldValue
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    moduleName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      positionFormVisible: false,
      curPosition: null,
      moduleTab: 'holding',
      activeTab: '',
      dialogVisible: false,
      dealRecords: [],
      tradeRecords: [],
      holdingPositions: [],
      totalCloseProfit: 0,
      totalPositionProfit: 0,
      moduleState: '',
      accountId: '',
      avgOccupiedAmount: 0,
      meanProfitOf5Transactions: 0,
      meanProfitOf10Transactions: 0,
      winningRateOf5Transactions: 0,
      winningRateOf10Transactions: 0,
      barDataMap: {},
      chart: null,
      loading: false
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
        this.$nextTick(this.init)
        this.$nextTick(this.loadRefData)
      }
    },
    moduleTab: function (val) {
      console.log(val)
      if (val === 'dealRecord') {
        setTimeout(() => {
          let table = this.$refs.dealTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        }, 50)
      }
      if (val === 'tradeRecord') {
        setTimeout(() => {
          let table = this.$refs.tradeTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        }, 50)
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
        this.$nextTick(this.close)
      }
    }
  },
  computed: {
    ...mapGetters(['getAccountById']),
    symbolOptions() {
      return Object.keys(this.barDataMap) || []
    },
    positionState() {
      return (
        { HOLDING_LONG: '持多单', HOLDING_SHORT: '持空单', EMPTY: '无持仓' }[this.moduleState] ||
        '等待成交'
      )
    },
    accountBalance() {
      if (!this.accountId || !this.getAccountById(this.accountId).account) {
        return 0
      }
      return this.getAccountById(this.accountId).account.balance
    }
  },
  methods: {
    async init() {
      this.loadBasicInfo()
      this.loadDealRecord()
      this.loadTradeRecord()
    },
    loadBasicInfo() {
      moduleApi.getModuleInfo(this.moduleName).then((result) => {
        this.totalPositionProfit = result.totalPositionProfit
        this.moduleState = result.moduleState
        this.accountId = result.accountId
        this.avgOccupiedAmount = result.avgOccupiedAmount
        let longPositions = Object.values(result.longPositions)
        let shortPositions = Object.values(result.shortPositions)
        this.holdingPositions = [...longPositions, ...shortPositions]
        this.meanProfitOf5Transactions = result.meanProfitOf5Transactions
        this.meanProfitOf10Transactions = result.meanProfitOf10Transactions
        this.winningRateOf5Transactions = result.winningRateOf5Transactions
        this.winningRateOf10Transactions = result.winningRateOf10Transactions
      })
    },
    loadDealRecord() {
      moduleApi.getModuleDealRecords(this.moduleName).then((result) => {
        this.dealRecords = result
        this.totalCloseProfit = result.length
          ? result.map((i) => i.closeProfit).reduce((a, b) => a + b)
          : 0

        this.$nextTick(() => {
          let table = this.$refs.dealTbl
          table.bodyWrapper.scrollTop = table.bodyWrapper.scrollHeight
        })
      })
    },
    loadTradeRecord() {
      moduleApi.getModuleTradeRecords(this.moduleName).then((result) => {
        this.tradeRecords = result
        this.$nextTick(() => {
          let table = this.$refs.tradeTbl
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
    createPosition() {
      this.curPosition = null
      this.positionFormVisible = true
    },
    editPosition(position) {
      console.log(position)
      this.curPosition = position
      this.positionFormVisible = true
    },
    async delPosition(position) {
      await moduleApi.removePosition(this.moduleName, position.unifiedSymbol, position.positionDir)
      this.init()
    },
    async onSave(position) {
      if (this.curPosition) {
        await moduleApi.updatePosition(this.moduleName, position)
      } else {
        await moduleApi.createPosition(this.moduleName, position)
      }
      this.init()
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
    }
  }
}
</script>

<style scoped>
.table-wrapper {
  height: calc(100vh - 250px);
}
.kline-wrapper {
  height: 100%;
  width: 100%;
  border-top: 1px solid;
  border-left: 1px solid;
}
.module-perf-wrapper {
  height: calc(100vh - 64px);
  display: flex;
}
.side-panel {
  min-width: 500px;
  border-top: 1px solid;
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
</style>
