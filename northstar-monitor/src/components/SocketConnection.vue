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
import io from 'socket.io-client'
const TYPE = {
  0: 'success',
  1: 'info',
  2: 'warning',
  3: 'error'
}
export default {
  props: {
    username: {
      type: String,
      default: 'admin'
    },
    password: {
      type: String,
      default: '123456'
    }
  },
  data() {
    return {
      socket: null,
      wsHost: '',
      wsPort: 51688
    }
  },
  watch: {
    '$store.state.marketCurrentDataModule.curRoomId': function (val, oldVal) {
      if (oldVal) {
        this.socket.emit('logout', oldVal)
      }
      if (val) {
        this.socket.emit('login', val)
      }
    }
  },
  mounted() {
    this.wsHost = window.remoteHost || location.hostname
    this.wsHost = this.wsHost.split(":")[0]
    this.wsPort = window.socketioPort || localStorage.getItem('socketioPort') || 51688
    this.initSocket()
  },
  methods: {
    initSocket() {
      const wsProtocal = process.env.NODE_ENV === 'development' ? 'ws' : 'wss'
      const wsEndpoint = `${wsProtocal}://${this.wsHost}:${this.wsPort}`
      const token = this.$route.query.auth || window.btoa(`${this.username}:${this.password}`)
      console.log('准备连接websocket：' + wsEndpoint, ' token:' + token)
      this.socket = io(wsEndpoint, {
        transports: ['websocket'],
        query: { auth: token },
        rejectUnauthorized : false 
      })
      this.socket.on('TICK', (data) => {
        this.$nextTick(() => {
          let tick = TickField.deserializeBinary(data).toObject()
          this.$store.commit('updateTick', tick)
        })
      })
      this.socket.on('BAR', (data) => {
        this.$nextTick(() => {
          let bar = BarField.deserializeBinary(data).toObject()
          this.$store.commit('updateBar', bar)
        })
      })
      this.socket.on('ACCOUNT', (data) => {
        this.$nextTick(() => {
          let account = AccountField.deserializeBinary(data).toObject()
          this.$store.commit('updateAccount', account)
        })
      })
      this.socket.on('POSITION', (data) => {
        this.$nextTick(() => {
          let position = PositionField.deserializeBinary(data).toObject()
          this.$store.commit('updatePosition', position)
        })
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
        this.$message[TYPE[notice.status]](notice.content)
      })
      this.socket.on('error', (e) => {
        console.log('SocketIO连接异常', e)
        this.$message.error('服务端连接异常')
      })
      this.socket.on('connect_error', (e) => {
        console.log('SocketIO连接失败', e)
        this.$message.error('服务端连接失败')
      })
      this.socket.on('connect', () => {
        console.log('SocketIO连接成功')
        this.$message.success('服务端连接成功')
      })
    }
  }
}
</script>

<style></style>
