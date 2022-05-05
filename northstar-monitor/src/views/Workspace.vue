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
        <el-menu-item index="3">投机模组管理</el-menu-item>
        <el-menu-item index="4">套利模组管理</el-menu-item>
        <el-menu-item index="5">历史行情</el-menu-item>
        <el-menu-item index="6">手工期货交易</el-menu-item>
        <el-menu-item index="7">手工期权交易</el-menu-item>
      </el-menu>
    </div>
    <socket-connection />
    <div class="ns-body">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
import SocketConnection from '@/components/SocketConnection'
const pageOpts = {
        "1": 'mktgateway',
        "2": 'tdgateway',
        "3": 'specmodule',
        "4": 'arbitmodule',
        "5": 'mktdata',
        "6": 'manualfttd',
        "7": 'manualopttd'
      }

export default {
  components: {
    SocketConnection
  },
  data(){
    return {
      curPage: "0"
    }
  },
  mounted(){
    if(this.$route.name === 'mktgateway'){
      this.curPage = '1'
      return 
    }
    this.handleSelect("1")
  },
  methods: {
    handleSelect(index) {
      if(index === this.curPage){
        return
      }
      this.curPage = index
      this.$router.push({name: pageOpts[index]})
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
