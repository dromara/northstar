<template>
  <div class="ns-workspace">
    <MailConfigForm :visible.sync="mailSettingFormVisible" />
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
        <el-menu-item index="1">行情管理</el-menu-item>
        <el-menu-item index="2">账户管理</el-menu-item>
        <el-menu-item index="3">模组管理</el-menu-item>
        <!-- <el-menu-item index="5">手工期权交易</el-menu-item> -->
        <el-menu-item index="6">手工期货交易</el-menu-item>
        <el-menu-item index="9">日志跟踪</el-menu-item>
      </el-menu>
      <div class="ns-tools">
        <el-button
          @click="mailSettingFormVisible = true"
          icon="el-icon-share"
          title="邮件通知设置"
          circle
        ></el-button>
        <el-button
          class="mr-10"
          icon="el-icon-switch-button"
          title="退出登陆"
          circle
          @click="logout"
        ></el-button>
        <span>版本号：v{{ version }}</span>
      </div>
    </div>
    <div class="ns-body">
      <router-view></router-view>
    </div>
    <socket-connection />
  </div>
</template>

<script>
import SocketConnection from '@/components/SocketConnection'
import MailConfigForm from '@/components/MailConfigForm'
import packageJson from '@/../package.json'

import loginApi from '@/api/loginApi'

const pageOpts = {
  1: 'mktgateway',
  2: 'tdgateway',
  3: 'module',
  5: 'manualopttd',
  6: 'manualfttd',
  9: 'logTailer'
}

const pageOptsRevert = {}
Object.keys(pageOpts).forEach((key) => (pageOptsRevert[pageOpts[key]] = key))

export default {
  components: {
    SocketConnection,
    MailConfigForm
  },
  data() {
    return {
      curPage: '0',
      mailSettingFormVisible: false,
      version: packageJson.version
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
      this.$router.push({ name: pageOpts[index], query: { auth: this.$route.query.auth } })
    },
    logout() {
      loginApi.logout()
      this.$message.success('退出登陆')
      this.$router.push({ name: 'login' })
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
  display: flex;
  justify-content: space-between;
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
.ns-tools {
  align-items: center;
  line-height: 60px;
  padding-right: 10px;
}
.mail-button-wrapper {
}
</style>
