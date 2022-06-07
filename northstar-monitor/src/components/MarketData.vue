<template>
  <div
    class="ns-mktdata"
    v-loading.fullscreen.lock="fullscreenLoading"
    element-loading-background="rgba(0, 0, 0, 0.5)"
  >
    <div class="flex-row mt-10">
      <el-form label-width="100px" :inline="true">
        <el-form-item label="网关列表">
          <el-select v-model="gateway" filterable :disabled="embededMode">
            <el-option v-for="gw in gatewayList" :label="gw" :value="gw" :key="gw"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="合约列表">
          <el-select v-model="unifiedSymbol" filterable :disabled="embededMode">
            <el-option
              v-for="(c, i) in gwContractList"
              :label="c.name"
              :value="c.unifiedSymbol"
              :value-key="c.unifiedSymbol"
              :key="i"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <div id="update-k-line" class="ns-mktdata__body">
      {{ !kLineChart ? '未有数据，请先选择合约' : '' }}
    </div>
  </div>
</template>

<script>
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import gatewayDataApi from '@/api/gatewayDataApi'

import { mapGetters } from 'vuex'

import { BarField } from '@/lib/xyz/redtorch/pb/core_field_pb'
import { KLineUtils } from '@/utils.js'

export default {
  name: 'UpdateKLineChart',
  props: {
    marketGatewayId: {
      type: String,
      default: ''
    },
    contractUnifiedSymbol: {
      type: String,
      default: ''
    },
    embededMode: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      kLineChart: null,
      fullscreenLoading: false,
      contractList: [],
      gateway: '',
      unifiedSymbol: ''
    }
  },
  created() {
    window.addEventListener('resize', () => {
      if (this.kLineChart) {
        this.kLineChart.resize()
      }
    })
  },
  computed: {
    ...mapGetters(['curMarketGatewayId', 'curUnifiedSymbol']),
    gatewayList() {
      const gatewayMap = {}
      this.contractList.forEach((i) => (gatewayMap[i.gatewayId] = true))
      return Object.keys(gatewayMap)
    },
    gwContractList() {
      return this.contractList.filter((i) => i.gatewayId === this.gateway)
    }
  },
  watch: {
    marketGatewayId: function (val) {
      this.gateway = val
    },
    contractUnifiedSymbol: function (val) {
      this.unifiedSymbol = val
    },
    gateway: function (gatewayId) {
      this.$store.commit('updateFocusMarketGatewayId', gatewayId)
    },
    unifiedSymbol: function (symbol) {
      this.$store.commit('updateFocusUnifiedSymbol', symbol)
    },
    '$store.state.marketCurrentDataModule.curBar': function (bar) {
      if (this.kLineChart && !!bar) {
        this.kLineChart.updateData(KLineUtils.createFromBar(bar))
      }
    },
    '$store.state.marketCurrentDataModule.curUnifiedSymbol': async function (val) {
      if (!this.kLineChart) {
        const kLineChart = init('update-k-line')
        kLineChart.addTechnicalIndicatorTemplate(volumePure)
        kLineChart.createTechnicalIndicator('CJL', false)
        this.kLineChart = kLineChart
        kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))

        kLineChart.loadMore(async (timestamp) => {
          console.log('加载更多数据')
          if (typeof timestamp !== 'number') {
            console.warn('忽略一个不是数值的时间戳: ' + timestamp)
            return
          }
          await new Promise((r) => setTimeout(r, 1000))
          const data = await this.loadBars(timestamp, true)
          kLineChart.applyMoreData(data || [], !!data)
        })
      }
      if (val) {
        this.kLineChart.clearData()
        this.kLineChart.applyNewData((await this.loadBars(new Date().getTime())) || [])
      }
    }
  },
  methods: {
    async loadBars(timestamp, loadMore) {
      this.fullscreenLoading = true
      try {
        const barDataList = await gatewayDataApi.loadWeeklyBarData(
          this.curMarketGatewayId,
          this.curUnifiedSymbol,
          timestamp,
          !loadMore
        )
        return barDataList
          .map((data) => BarField.deserializeBinary(data).toObject())
          .map((bar) => KLineUtils.createFromBar(bar))
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.fullscreenLoading = false
      }
    }
  },
  destroyed: function () {
    dispose('update-k-line')
  }
}
</script>

<style>
.ns-mktdata {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}
.ns-mktdata__head {
  height: 30px;
  width: 100%;
  display: flex;
}
.ns-mktdata__body {
  display: flex;
  flex: 1;
  justify-content: center;
  align-items: center;
}
</style>
