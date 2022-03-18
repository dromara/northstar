<template>
  <div
    class="ns-mktdata"
    v-loading.fullscreen.lock="fullscreenLoading"
    element-loading-background="rgba(0, 0, 0, 0.5)"
  >
    <div id="update-k-line" class="ns-mktdata__body">
      {{ !kLineChart ? '未有数据，请先选择合约' : '' }}
    </div>
  </div>
</template>

<script>
import { dispose, init } from 'klinecharts'
import volumePure from '@/lib/indicator/volume-pure'
import openInterestDelta from '@/lib/indicator/open-interest'

import dataSyncApi from '@/api/dataSyncApi'
import { mapGetters } from 'vuex'

import { BarField } from '@/lib/xyz/redtorch/pb/core_field_pb'
import { KLineUtils } from '@/utils.js'

export default {
  name: 'UpdateKLineChart',
  data() {
    return {
      kLineChart: null,
      fullscreenLoading: false
    }
  },
  mounted: function () {},
  computed: {
    ...mapGetters(['curMarketGatewayId', 'curUnifiedSymbol'])
  },
  watch: {
    '$store.state.marketCurrentDataModule.curBar': function (bar) {
      if (this.kLineChart && !!bar) {
        this.kLineChart.updateData(KLineUtils.createFromBar(bar))
      }
    },
    '$store.state.marketCurrentDataModule.curUnifiedSymbol': async function (val) {
      if (!this.kLineChart) {
        const kLineChart = init('update-k-line')
        kLineChart.addTechnicalIndicatorTemplate(volumePure)
        kLineChart.addTechnicalIndicatorTemplate(openInterestDelta)
        kLineChart.createTechnicalIndicator('CJL', false)
        kLineChart.createTechnicalIndicator('OpDif', false)
        this.kLineChart = kLineChart
        kLineChart.setStyleOptions(KLineUtils.getThemeOptions('dark'))

        kLineChart.loadMore(async (timestamp) => {
          console.log('加载更多数据')
          if (typeof timestamp !== 'number') {
            console.warn('忽略一个不是数值的时间戳: ' + timestamp)
            return
          }
          await new Promise((r) => setTimeout(r, 1000))
          const data = await this.loadBars(timestamp)
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
    async loadBars(timestamp) {
      this.fullscreenLoading = true
      try {
        const barDataList = await dataSyncApi.loadHistoryBars(
          this.curMarketGatewayId,
          this.curUnifiedSymbol,
          timestamp
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
