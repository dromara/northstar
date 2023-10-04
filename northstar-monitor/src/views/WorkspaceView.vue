<template>
  <div class="ns-workspace">
    <MessageConfigForm :visible.sync="mailSettingFormVisible" />
    <div class="ns-header">
      <el-menu
        class="ns-menu"
        mode="horizontal"
        @select="handleSelect"
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
        :default-active="curPage"
      >
        <el-menu-item id="tabMarketData" index="1">行情管理</el-menu-item>
        <el-menu-item id="tabAccount" index="2">账户管理</el-menu-item>
        <el-menu-item id="tabModule" index="3">模组管理</el-menu-item>
        <el-menu-item id="tabTrade" index="6">手工交易</el-menu-item>
        <el-menu-item id="tabLog" index="9" @click="systemLogger">日志跟踪</el-menu-item>
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
          title="退出登录"
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
import MessageConfigForm from '@/components/MessageConfigForm'
import packageJson from '@/../package.json'

import loginApi from '@/api/loginApi'
import MediaListener from '@/utils/media-utils'

const pageOpts = {
  1: 'mktgateway',
  2: 'tdgateway',
  3: 'module',
  5: 'manualopttd',
  6: 'manualfttd',
  9: 'logger'
}

const pageOptsRevert = {}
Object.keys(pageOpts).forEach((key) => (pageOptsRevert[pageOpts[key]] = key))

export default {
  components: {
    SocketConnection,
    MessageConfigForm
  },
  data() {
    return {
      curPage: '0',
      mailSettingFormVisible: false,
      version: packageJson.version
    }
  },
  beforeRouteEnter(to, from, next){
    const listener = new MediaListener(() => {})
    if(listener.isMobile() && to.name !== 'module'){
      const newTo = {
        path: '/module',
        query: to.query,
        params: to.params
      }
      next(newTo)
      return
    }
    next()
  },
  mounted() {
    this.curPage = pageOptsRevert[this.$route.name]
    const resizeHandler = () => {
      if(this.listener.isMobile() && this.$route.name !== 'module' && this.$route.name !== 'manualfttd'){
        this.handleSelect('3', ['3'])
      }
    }
    this.listener = new MediaListener(resizeHandler)
    resizeHandler()
  },
  destroyed() {
    this.listener.destroy()
    this.$nextTick(() => location.reload())
  },
  methods: {
    handleSelect(index, params) {
      if (index === this.curPage) {
        return
      }
      this.curPage = index
      this.$router.push({
        name: pageOpts[index],
        query: Object.assign({}, this.$route.query),
        params: {module: params.module}
      })
    },
    logout() {
      loginApi.logout()
      this.$message.success('退出登录')
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
/* 桌面端样式 */
@media screen and (min-width: 661px) {
  
}

/* 移动端样式 */
@media screen and (max-width: 660px) {
  .ns-tools button,
  .ns-menu li:nth-child(1),
  .ns-menu li:nth-child(2),
  .ns-menu li:nth-child(5)
  {
    display: none;
  }
}

</style>
