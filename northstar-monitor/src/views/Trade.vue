<template>
  <div class="ns-trade">
    <div class="ns-trade__account-profile">
      <el-select
        class="ns-trade__account"
        v-model="currentAccountId"
        placeholder="选择账户"
        @change="handleAccountChange"
      >
        <el-option
          v-for="(item, index) in accountOptions"
          :key="index"
          :label="item.gatewayId"
          :value="item.gatewayId"
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
              :value="item.unifiedSymbol"
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
</template>

<script>
import NsButton from '@/components/TradeButton'
import NsPriceBoard from '@/components/PriceBoard'
import NsAccountDetail from '@/components/AccountDetail'
import gatewayMgmtApi from '../api/gatewayMgmtApi'
import tradeOprApi from '../api/tradeOprApi'
import dataSyncApi from '../api/dataSyncApi'

let accountCheckTimer

export default {
  name: 'Trade',
  components: {
    NsButton,
    NsPriceBoard,
    NsAccountDetail
  },
  data() {
    return {
      accountOptions: [],
      accountMap: {},
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
      currentAccountId: '',
      currentPosition: ''
    }
  },
  methods: {
    handleAccountChange() {
      if (!this.currentAccountId) {
        return
      }
      clearTimeout(accountCheckTimer)
      const timelyCheck = () => {
        accountCheckTimer = setTimeout(() => {
          if (!this.$store.getters.isAccountConnected(this.currentAccountId)) {
            this.$message.error(`账户${this.currentAccountId}没有连线`)
          }
          timelyCheck()
        }, 3000)
      }
      timelyCheck()

      // this.symbolList = this.$store.getters.findContractsByType(
      //   this.currentAccount.bindedMktGatewayId,
      //   'FUTURES'
      // )
      const sortFunc = (a, b) => {
        return a['unifiedSymbol'].localeCompare(b['unifiedSymbol'])
      }

      dataSyncApi.getAvailableContracts().then((list) => {
        console.log('合约总数', list.length)
        this.symbolList = list
          .filter((i) => i.gatewayId === this.currentAccount.bindedMktGatewayId)
          .sort(sortFunc)
      })

      this.$store.commit('updateFocusMarketGatewayId', this.currentAccount.bindedMktGatewayId)
      this.$store.commit('updateCurAccountId', this.currentAccountId)
    },
    handleContractChange() {
      this.dealPriceType = 'COUNTERPARTY_PRICE'
      this.$store.commit('updateFocusUnifiedSymbol', this.dealSymbol)
      console.log(this.dealSymbol)
    },
    handleDealPriceTypeChange() {
      if (this.dealPriceType !== 'CUSTOM_PRICE') {
        this.limitPrice = ''
      }
    },
    onPositionChosen(pos) {
      console.log(pos)
      this.dealVol = pos.position - pos.frozen
      this.dealSymbol = pos.contract.unifiedsymbol
      this.currentPosition = pos
      this.handleContractChange()
      console.log(this.closePrice)
    },
    onCancelOrder(order) {
      console.log(order)
      tradeOprApi.cancelOrder(this.currentAccountId, order.originorderid)
    },
    buyOpen() {
      if (this.stopPrice && this.stopPrice >= this.bkPrice) {
        throw new Error('多开止损价需要少于开仓价')
      }
      return tradeOprApi.buyOpen(
        this.currentAccountId,
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
        this.currentAccountId,
        this.dealSymbol,
        this.skPrice,
        this.dealVol,
        this.stopPrice
      )
    },
    closePosition() {
      if (this.currentPosition.positiondirection === 2) {
        return tradeOprApi.closeLongPosition(
          this.currentAccountId,
          this.dealSymbol,
          this.closePrice,
          this.dealVol
        )
      }
      if (this.currentPosition.positiondirection === 3) {
        return tradeOprApi.closeShortPosition(
          this.currentAccountId,
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
    this.currentAccountId = this.$store.state.accountModule.curAccountId
    this.accountOptions = await gatewayMgmtApi.findAll('TRADE')
    this.accountOptions.forEach((i) => {
      this.accountMap[i.gatewayId] = i
    })
    this.handleAccountChange()
  },
  computed: {
    flexibleTblHeight() {
      return document.body.clientHeight - 460
    },
    currentAccount() {
      return this.accountMap[this.currentAccountId]
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
.el-input-number--small {
  width: 100%;
}
.el-input-number {
  width: 100%;
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
