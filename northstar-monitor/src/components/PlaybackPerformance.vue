<template>
  <el-dialog title="回测详情" :visible.sync="dialogVisible" append-to-body width="600px">
    <div class="warning-text compact" v-if="record.exceptionMessage">
      <i class="el-icon-warning" /> {{ record.exceptionMessage }}<br />
    </div>
    <div class="module-perf-wrapper">
      <div class="side-panel">
        <div class="basic-info">
          <el-form inline>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue label="模组名称" label-width="70px" :value="moduleName" />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue label="总盈亏" label-width="70px" :value="record.sumOfProfit" />
              </el-col>
              <el-col span="8">
                <el-tooltip
                  class="item"
                  effect="dark"
                  content="365 *（总盈亏-总手续费）/（平均占用资金 x 回测时长）"
                  placement="left"
                >
                  <ReadonlyFieldValue
                    label="年化收益率"
                    label-width="84px"
                    :value="parseInt((record.yearlyEarningRate || 0) * 100) + ' %'"
                  />
                </el-tooltip>
              </el-col>
            </el-row>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="交易次数"
                  label-width="70px"
                  :value="record.timesOfTransaction"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="总手续费"
                  label-width="70px"
                  :value="record.sumOfCommission"
                />
              </el-col>
              <el-col span="8">
                <el-tooltip
                  class="item"
                  effect="dark"
                  content="每笔交易占用资金 = 1.5倍的持仓保证金（可能有误差）"
                  placement="left"
                >
                  <ReadonlyFieldValue
                    label="平均占用资金"
                    label-width="84px"
                    :value="parseInt(record.meanOfOccupiedMoney || 0)"
                  />
                </el-tooltip>
              </el-col>
            </el-row>
            <el-row>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="回测时长"
                  label-width="70px"
                  :value="(record.duration || 0) + ' 天'"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="盈亏标准差"
                  label-width="70px"
                  :value="parseInt(record.stdOfProfit || 0)"
                />
              </el-col>
              <el-col span="8">
                <ReadonlyFieldValue
                  label="最大回撤金额"
                  label-width="84px"
                  :value="record.maxFallback"
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每5笔样本期望盈亏均值"
                  label-width="160px"
                  :value="(record.meanOf5TransactionsAvgProfit || 0).toFixed(3)"
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每5笔样本期望盈亏标准差"
                  label-width="180px"
                  :value="(record.stdOf5TransactionsAvgProfit || 0).toFixed(3)"
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每5笔样本胜率均值"
                  label-width="160px"
                  :value="((record.meanOf5TransactionsAvgWinningRate || 0) * 100).toFixed(2) + ' %'"
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每5笔样本胜率标准差"
                  label-width="180px"
                  :value="((record.stdOf5TransactionsAvgWinningRate || 0) * 100).toFixed(2) + ' %'"
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每10笔样本期望盈亏均值"
                  label-width="160px"
                  :value="(record.meanOf10TransactionsAvgProfit || 0).toFixed(3)"
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每10笔样本期望盈亏标准差"
                  label-width="180px"
                  :value="(record.stdOf10TransactionsAvgProfit || 0).toFixed(3)"
                />
              </el-col>
            </el-row>
            <el-row>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每10笔样本胜率均值"
                  label-width="160px"
                  :value="
                    ((record.meanOf10TransactionsAvgWinningRate || 0) * 100).toFixed(2) + ' %'
                  "
                />
              </el-col>
              <el-col span="12">
                <ReadonlyFieldValue
                  label="每10笔样本胜率标准差"
                  label-width="180px"
                  :value="((record.stdOf10TransactionsAvgWinningRate || 0) * 100).toFixed(2) + ' %'"
                />
              </el-col>
            </el-row>
          </el-form>
        </div>
        <div>
          <el-tabs v-model="moduleTab" :stretch="true">
            <el-tab-pane name="dealRecord" label="交易历史"></el-tab-pane>
            <el-tab-pane name="tradeRecord" label="原始成交"></el-tab-pane>
          </el-tabs>
        </div>
        <div class="table-wrapper pb-20">
          <el-table
            ref="dealTbl"
            v-show="moduleTab === 'dealRecord'"
            :data="dealRecords"
            height="350px"
          >
            <el-table-column
              prop="contractName"
              label="合约"
              align="center"
              width="80px"
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
            height="350px"
          >
            <el-table-column prop="contractName" label="合约" align="center"></el-table-column>
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
    </div>
  </el-dialog>
</template>

<script>
import ReadonlyFieldValue from './ReadonlyFieldValue.vue'
import playbackApi from '@/api/playbackApi'
export default {
  components: {
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
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
        this.$nextTick(this.init)
        this.updatePage()
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
        this.clearPage()
      }
    }
  },
  data() {
    return {
      dialogVisible: false,
      moduleTab: 'dealRecord',
      record: {},
      dealRecords: [],
      tradeRecords: []
    }
  },
  methods: {
    updatePage() {
      playbackApi.getPlaybackStatRecord(this.moduleName).then((record) => (this.record = record))
      playbackApi
        .getPlaybackDealRecord(this.moduleName)
        .then((dealRecords) => (this.dealRecords = dealRecords))
      playbackApi
        .getPlaybackTradeRecord(this.moduleName)
        .then((tradeRecords) => (this.tradeRecords = tradeRecords))
    },
    clearPage() {
      this.record = {}
      this.dealRecords = []
      this.tradeRecords = []
    }
  }
}
</script>

<style>
.warning-text.compact {
  margin-bottom: 0;
}
</style>
