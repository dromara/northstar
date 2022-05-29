<template>
  <div class="ns-page">
    <div ref="tradeWrap" class="ns-trade">
      <div class="ns-trade__account-profile">
        <el-select
          class="ns-trade__account"
          v-model="chosenAccount"
          placeholder="选择账户"
          @change="handleAccountChange"
        >
          <el-option
            v-for="(item, index) in accountOptions"
            :key="index"
            :label="item.gatewayId"
            :value="item"
          >
          </el-option>
        </el-select>
        <div class="ns-trade__account-description">
          权益：{{ parseInt(accountBalance) | accountingFormatter }}
        </div>
        <div class="ns-trade__account-description">
          可用：{{ parseInt(accountAvailable) | accountingFormatter }}
        </div>
        <div class="ns-trade__account-description">
          使用率：{{
            accountBalance
              ? (((accountBalance - accountAvailable) * 100) / accountBalance).toFixed(1)
              : 0
          }}
          %
        </div>
      </div>
      <div class="ns-trade__trade-section">
        <div class="ns-trade-action">
          <div class="ns-trade-action__item">
            <el-select
              v-model="dealSymbol"
              filterable
              placeholder="请选择合约"
              value-key="unifiedSymbol"
              @change="handleContractChange"
            >
              <el-option
                v-for="(item, i) in symbolList"
                :key="i"
                :label="item.name"
                :value="item.unifiedsymbol"
              >
              </el-option>
            </el-select>
          </div>
          <div class="ns-trade-action__item">
            <div class="ns-trade-action__complex-item">
              <div class="ns-trade-action__complex-item-label">手数：</div>
              <el-input-number
                v-model="dealVol"
                :min="1"
                :max="10"
                controls-position="right"
              ></el-input-number>
            </div>
          </div>
          <div class="ns-trade-action__item">
            <el-select
              v-model="dealPriceType"
              filterable
              placeholder="价格类型"
              @change="handleDealPriceTypeChange"
            >
              <el-option
                v-for="item in priceOptionList"
                :key="item.type"
                :label="item.label"
                :value="item.type"
              >
              </el-option>
            </el-select>
          </div>
          <div class="ns-trade-action__item">
            <el-input
              v-model="limitPrice"
              placeholder="委托价"
              :disabled="dealPriceType !== 'CUSTOM_PRICE'"
              type="number"
            ></el-input>
          </div>
          <div class="ns-trade-action__item">
            <el-input v-model="stopPrice" placeholder="止损价" type="number"></el-input>
          </div>
        </div>
        <div class="ns-trade-info">
          <NsPriceBoard :tick="$store.state.marketCurrentDataModule.curTick" />
        </div>
      </div>
      <div class="ns-trade__trade-btn-wrap">
        <div class="ns-trade-button">
          <NsButton
            :price="`${bkPrice || 0}`"
            :color="'rgba(196, 68, 66, 1)'"
            :label="'买开'"
            @click.native="buyOpen"
          />
        </div>
        <div class="ns-trade-button">
          <NsButton
            :price="`${skPrice || 0}`"
            :color="'rgba(64, 158, 95, 1)'"
            :label="'卖开'"
            @click.native="sellOpen"
          />
        </div>
        <div class="ns-trade-button">
          <NsButton
            :price="`${closePrice || '优先平今'}`"
            :reverseColor="true"
            :label="'平仓'"
            @click.native="closePosition"
          />
        </div>
      </div>
      <NsAccountDetail
        :tableContentHeight="flexibleTblHeight"
        :positionDescription="$store.state.accountModule.curInfo.positions"
        :orderDescription="$store.state.accountModule.curInfo.orders"
        :transactionDescription="$store.state.accountModule.curInfo.transactions"
        @chosenPosition="onPositionChosen"
        @cancelOrder="onCancelOrder"
      />
    </div>
    <div class="ns-trade__md-wrapper">
      <NsMarketData
        :marketGatewayId="marketDataGatewayId"
        :contractUnifiedSymbol="marketDataUnifiedSymbol"
        embededMode
      />
    </div>
  </div>
</template>

<script>
import NsButton from '@/components/TradeButton'
import NsPriceBoard from '@/components/PriceBoard'
import NsAccountDetail from '@/components/AccountDetail'
import NsMarketData from '@/components/MarketData'
import gatewayMgmtApi from '@/api/gatewayMgmtApi'
import tradeOprApi from '@/api/tradeOprApi'
import { ContractField } from '@/lib/xyz/redtorch/pb/core_field_pb'

let accountCheckTimer

export default {
  components: {
    NsButton,
    NsPriceBoard,
    NsAccountDetail,
    NsMarketData
  },
  data() {
    return {
      accountOptions: [],
      symbolList: [],
      priceOptionList: [
        {
          label: '对手价',
          type: 'COUNTERPARTY_PRICE'
        },
        {
          label: '排队价',
          type: 'WAITING_PRICE'
        },
        {
          label: '市价',
          type: 'FIGHTING_PRICE'
        },
        {
          label: '限价',
          type: 'CUSTOM_PRICE'
        }
      ],
      dealSymbol: '',
      dealVol: '',
      dealPrice: '',
      limitPrice: '',
      stopPrice: '',
      dealPriceType: '',
      curTab: 'position',
      symbolIndexMap: {},
      chosenAccount: '',
      currentPosition: '',
      elementHeight: 0
    }
  },
  methods: {
    handleAccountChange() {
      this.dealSymbol = ''
      if (!this.chosenAccount) {
        return
      }
      clearTimeout(accountCheckTimer)
      const timelyCheck = () => {
        accountCheckTimer = setTimeout(() => {
          if (!this.$store.getters.isAccountConnected(this.chosenAccount.gatewayId)) {
            this.$message.error(`账户【${this.chosenAccount.gatewayId}】没有连线`)
          }
          timelyCheck()
        }, 3000)
      }
      timelyCheck()

      gatewayMgmtApi
        .getSubscribedContracts(this.chosenAccount.bindedMktGatewayId)
        .then((list) => {
          this.symbolList = list
            .map((item) => ContractField.deserializeBinary(item).toObject())
            .filter((item) => item.productclass === 2)
            .sort((a, b) => a['unifiedsymbol'].localeCompare(b['unifiedsymbol']))
        })
        .catch((e) => {
          this.$message.error(e.message)
        })

      this.$store.commit('updateFocusMarketGatewayId', this.chosenAccount.bindedMktGatewayId)
      this.$store.commit('updateCurAccountId', this.chosenAccount.gatewayId)
    },
    handleContractChange() {
      this.dealPriceType = 'COUNTERPARTY_PRICE'
      this.$store.commit('updateFocusUnifiedSymbol', this.dealSymbol)
    },
    handleDealPriceTypeChange() {
      if (this.dealPriceType !== 'CUSTOM_PRICE') {
        this.limitPrice = ''
      }
    },
    onPositionChosen(pos) {
      this.dealVol = pos.position - pos.frozen
      this.dealSymbol = pos.contract.unifiedsymbol
      this.currentPosition = pos
      this.handleContractChange()
    },
    onCancelOrder(order) {
      tradeOprApi.cancelOrder(this.chosenAccount.gatewayId, order.originorderid)
    },
    buyOpen() {
      if (this.stopPrice && this.stopPrice >= this.bkPrice) {
        throw new Error('多开止损价需要少于开仓价')
      }
      return tradeOprApi.buyOpen(
        this.chosenAccount.gatewayId,
        this.dealSymbol,
        this.bkPrice,
        this.dealVol,
        this.stopPrice
      )
    },
    sellOpen() {
      if (this.stopPrice && this.stopPrice <= this.skPrice) {
        throw new Error('空开止损价需要大于开仓价')
      }
      return tradeOprApi.sellOpen(
        this.chosenAccount.gatewayId,
        this.dealSymbol,
        this.skPrice,
        this.dealVol,
        this.stopPrice
      )
    },
    closePosition() {
      if (this.currentPosition.positiondirection === 2) {
        return tradeOprApi.closeLongPosition(
          this.chosenAccount.gatewayId,
          this.dealSymbol,
          this.closePrice,
          this.dealVol
        )
      }
      if (this.currentPosition.positiondirection === 3) {
        return tradeOprApi.closeShortPosition(
          this.chosenAccount.gatewayId,
          this.dealSymbol,
          this.closePrice,
          this.dealVol
        )
      }
      throw new Error('没有持仓')
    }
  },
  beforeDestroy() {
    clearTimeout(accountCheckTimer)
    this.$store.commit('resetMarketCurrentDataModule')
  },
  async created() {
    this.accountOptions = await gatewayMgmtApi.findAll('TRADE')
    this.accountOptions = this.accountOptions.map((item) => {
      item.value = item.gatewayId
      return item
    })
    const self = this
    window.addEventListener('resize', () => {
      if (self.$refs.tradeWrap) {
        self.elementHeight = self.$refs.tradeWrap.clientHeight
      }
    })
  },
  mounted() {
    this.elementHeight = this.$refs.tradeWrap.clientHeight
  },
  computed: {
    flexibleTblHeight() {
      return this.elementHeight - 420
    },
    marketDataGatewayId() {
      return this.chosenAccount.bindedMktGatewayId
    },
    marketDataUnifiedSymbol() {
      return this.dealSymbol
    },
    accountInfo() {
      return this.$store.state.accountModule.curInfo.account
    },
    accountBalance() {
      if (this.accountInfo.balance) return this.accountInfo.balance
      return 0
    },
    accountAvailable() {
      if (this.accountInfo.available) return this.accountInfo.available
      return 0
    },
    bkPrice() {
      return {
        COUNTERPARTY_PRICE: this.$store.state.marketCurrentDataModule.curTick.askpriceList[0],
        WAITING_PRICE: this.$store.state.marketCurrentDataModule.curTick.bidpriceList[0],
        FIGHTING_PRICE: this.$store.state.marketCurrentDataModule.curTick.upperlimit,
        CUSTOM_PRICE: this.limitPrice
      }[this.dealPriceType]
    },
    skPrice() {
      return {
        COUNTERPARTY_PRICE: this.$store.state.marketCurrentDataModule.curTick.bidpriceList[0],
        WAITING_PRICE: this.$store.state.marketCurrentDataModule.curTick.askpriceList[0],
        FIGHTING_PRICE: this.$store.state.marketCurrentDataModule.curTick.lowerlimit,
        CUSTOM_PRICE: this.limitPrice
      }[this.dealPriceType]
    },
    closePrice() {
      if (this.currentPosition && this.currentPosition.positiondirection === 2) {
        return this.skPrice
      }
      if (this.currentPosition && this.currentPosition.positiondirection === 3) {
        return this.bkPrice
      }
      return ''
    }
  }
}
</script>

<style>
.ns-trade-wrapper {
  display: flex;
}
.ns-trade {
  width: 100%;
  max-width: 450px;
  margin: auto;
  height: 100%;
  display: flex;
  overflow: hidden;
  flex-direction: column;
  background-color: rgba(67, 74, 80, 1);
}
.ns-trade__account {
  width: 100%;
  margin-left: 20px;
}
.ns-trade__account-profile {
  display: flex;
  width: 100%;
  grid-template-columns: 1fr repeat(3, 1fr);
  padding: 10px 0;
  line-height: 32px;
  background-color: rgba(20, 20, 20, 0.4);
}
.ns-trade__account-description {
  width: 100%;
  text-align: center;
  font-size: 10px;
}
.ns-trade__trade-btn-wrap {
  display: flex;
}
.ns-trade__trade-section {
  height: 100%;
  max-height: 236px;
  display: flex;
}
.ns-trade-action__item {
  height: 100%;
  display: flex;
  justify-content: center;
  flex-direction: column;
}
.ns-trade-action__item-label {
  width: 100%;
}
.ns-trade-action__item-content {
  width: 100%;
}
.ns-trade-action {
  display: flex;
  flex-direction: column;
  padding: 10px 20px;
}
.ns-trade-info {
  width: 80%;
  padding: 10px 20px;
  min-height: 200px;
}
.ns-account-table {
  text-align: center;
}
.el-tabs__item {
  padding: 0;
}
.el-table th > .cell {
  padding: 0;
}
.ns-trade__md-wrapper {
  width: 100%;
  height: 100%;
}
.ns-trade-action__complex-item {
  display: flex;
}
.ns-trade-action__complex-item-label {
  width: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.ns-trade-button {
  width: 100%;
  height: 100%;
  padding: 0 20px;
}
</style>
