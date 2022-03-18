<template>
  <div />
</template>

<script>
import {
  TickField,
  BarField,
  ContractField,
  AccountField,
  PositionField,
  TradeField,
  OrderField,
  NoticeField
} from '@/lib/xyz/redtorch/pb/core_field_pb'
import SocketIO from 'socket.io-client'
const TYPE = {
  0: 'success',
  1: 'info',
  2: 'warning',
  3: 'error'
}
export default {
  data() {
    return {
      socket: null
    }
  },
  watch: {
    '$store.state.marketCurrentDataModule.curUnifiedSymbol': function (val, oldVal) {
      if (oldVal) {
        this.socket.emit('logout', oldVal)
      }
      if (val) {
        this.socket.emit('login', val)
      }
    }
  },
  mounted() {
    this.socket = SocketIO()
    this.socket.on('TICK', (data) => {
      let tick = TickField.deserializeBinary(data).toObject()
      this.$store.commit('updateTick', tick)
    })
    this.socket.on('IDX_TICK', (data) => {
      let tick = TickField.deserializeBinary(data).toObject()
      this.$store.commit('updateTick', tick)
    })
    this.socket.on('BAR', (data) => {
      let bar = BarField.deserializeBinary(data).toObject()
      this.$store.commit('updateBar', bar)
    })
    this.socket.on('HIS_BAR', (data) => {
      let bar = BarField.deserializeBinary(data).toObject()
      this.$store.commit('updateHisBar', bar)
    })
    this.socket.on('ACCOUNT', (data) => {
      let account = AccountField.deserializeBinary(data).toObject()
      this.$store.commit('updateAccount', account)
    })
    this.socket.on('POSITION', (data) => {
      let position = PositionField.deserializeBinary(data).toObject()
      this.$store.commit('updatePosition', position)
    })
    this.socket.on('TRADE', (data) => {
      let trade = TradeField.deserializeBinary(data).toObject()
      this.$store.commit('updateTrade', trade)
    })
    this.socket.on('ORDER', (data) => {
      let order = OrderField.deserializeBinary(data).toObject()
      this.$store.commit('updateOrder', order)
    })
    this.socket.on('CONTRACT', (data) => {
      let contract = ContractField.deserializeBinary(data).toObject()
      this.$store.commit('updateContract', contract)
    })
    this.socket.on('NOTICE', (data) => {
      let notice = NoticeField.deserializeBinary(data).toObject()
      console.log(notice)
      this.$message[TYPE[notice.status]](notice.content)
    })
    this.socket.on('error', (e) => {
      console.log('SocketIO连接异常', e)
    })
    this.socket.on('connect', () => {
      console.log('SocketIO连接成功')
    })
  }
}
</script>

<style></style>
