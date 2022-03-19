<template>
  <div class="ns-workspace">
    <div class="ns-header">
      <el-menu
        class="el-menu-demo"
        mode="horizontal"
        @select="handleSelect"
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
        :default-active="curPage"
      >
        <el-menu-item index="1">网关管理</el-menu-item>
        <el-menu-item index="2">账户管理</el-menu-item>
        <el-menu-item index="3">策略模组管理</el-menu-item>
        <el-menu-item index="4">手工交易</el-menu-item>
      </el-menu>
    </div>
    <div class="ns-body">
      <GatewayManagement
        v-if="curPage === '1'"
        :gatewayUsage="'MARKET_DATA'"
        :key="1"
      />
      <GatewayManagement
        v-if="curPage === '2'"
        :gatewayUsage="'TRADE'"
        :key="2"
      />
      <ModuleManagement v-if="curPage === '3'" :key="3" />
      <div class="ns-trmkt-wrapper" v-if="curPage === '4'" :key="4">
        <Trade />
        <MarketData />
      </div>
    </div>
    <socket-connection />
  </div>
</template>

<script>
import GatewayManagement from './GatewayMgmt'
import ModuleManagement from './ModuleMgmt'
import SocketConnection from '../components/SocketConnection'
import MarketData from './MarketData'
import Trade from './Trade'
import dataSyncApi from '../api/dataSyncApi'

export default {
  components: {
    GatewayManagement,
    ModuleManagement,
    SocketConnection,
    MarketData,
    Trade
  },
  data() {
    return {
      curPage: '1'
    }
  },
  created() {
    dataSyncApi.dataSync()
  },
  methods: {
    handleSelect(index) {
      this.curPage = index
    }
  }
}
</script>

<style scoped>
.ns-workspace {
  width: 100%;
  display: flex;
  flex-direction: column;
}
.ns-header {
  border-bottom: solid 1px #e6e6e6;
  width: 100%;
}
.ns-body {
  height: 100%;
  width: 100%;
  display: flex;
  overflow: hidden;
}

.el-menu.el-menu--horizontal {
  border-bottom: none;
}
.ns-trmkt-wrapper {
  display: flex;
  width: 100%;
}
</style>
