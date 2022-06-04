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
        <el-menu-item index="3">模组管理</el-menu-item>
        <!-- <el-menu-item index="5">历史行情</el-menu-item> -->
        <el-menu-item index="6">手工期货交易</el-menu-item>
        <!-- <el-menu-item index="7">手工期权交易</el-menu-item> -->
      </el-menu>
    </div>
    <div class="ns-body">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
const pageOpts = {
  1: 'mktgateway',
  2: 'tdgateway',
  3: 'specmodule',
  5: 'mktdata',
  6: 'manualfttd',
  7: 'manualopttd'
}

const pageOptsRevert = {}
Object.keys(pageOpts).forEach((key) => (pageOptsRevert[pageOpts[key]] = key))

export default {
  data() {
    return {
      curPage: '0'
    }
  },
  mounted() {
    this.curPage = pageOptsRevert[this.$route.name]
  },
  methods: {
    handleSelect(index) {
      if (index === this.curPage) {
        return
      }
      this.curPage = index
      this.$router.push({ name: pageOpts[index] })
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
