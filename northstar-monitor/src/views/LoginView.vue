<template>
  <div class="wrapper">
    <div class="version">版本号：v{{ version }}</div>
    <div class="logo"><img width="700px" src="../assets/logo.png" /></div>
    <div class="panel" v-on:keydown.enter="login">
      <el-form :model="userForm" status-icon label-width="100px" class="demo-userForm">
        <el-form-item label="用户名">
          <el-input v-model="userForm.name" clearable></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="userForm.pass" autocomplete="off" clearable></el-input>
        </el-form-item>
        <el-form-item v-if="showHost" label="服务端地址">
          <el-input autocomplete placeholder="域名或IP地址" v-model="domain"/>
        </el-form-item>
        <el-form-item>
          <el-button @click="login">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import loginApi from '@/api/loginApi'
import MediaListener from '@/utils/media-utils'

const listener = new MediaListener(() => {})

export default {
  data() {
    return {
      userForm: {
        name: '',
        pass: ''
      },
      domain: '',
      showHost: false,
      isEE: false,
      version: '0.0.1'
    }
  },
  created(){
    fetch('/version').then(result => {
      if(result.status === 200){
        return result.text()
      }
      return Promise.reject()
    }).then(v => {
      this.version = v.replace(/"/g,"")
    })
  },
  mounted() {
    this.isEE = !!(window.require && window.require('electron'))
    this.showHost = this.isEE || this.$route.query.desktop
    this.domain = localStorage.getItem('domain') || ''
  },
  methods: {
    async login() {
      if(this.domain){
        window.baseURL = `${this.isEE ? 'https:' : location.protocol}` + '//' + this.domain
        window.remoteHost = this.domain
        localStorage.setItem('domain', this.domain)
      }
      try{
        await loginApi.healthyCheck()
      } catch(e){
        console.log(e)
        this.$message.error('服务端未启动')
        return;
      }
      await loginApi.login(this.userForm.name, this.userForm.pass)
      window.socketioPort = await loginApi.socketioPort()
      localStorage.setItem('socketioPort', window.socketioPort)
      console.log('登录成功')
      this.$router.push({
        name: `${listener.isMobile() ? 'module' : 'mktgateway'}`,
        query: { auth: window.btoa(`${this.userForm.name}:${this.userForm.pass}`) }
      })
    }
  }
}
</script>

<style>
.version{
  position: fixed;
  right: 20px;
  top: 20px;
}
/* 桌面端样式 */
@media screen and (min-width: 661px) {
  .panel {
    /* width: 20%; */
    width: 300px;
    min-width: 300px;
    margin: auto;
  }
  .wrapper {
    display: flex;
    margin: auto;
    flex-direction: column;
  }
}

/* 移动端样式 */
@media screen and (max-width: 660px) {
  .wrapper {
    display: flex;
    margin: auto;
    flex-direction: column;
  }
  .panel {
    width: 80vw;
    padding: 0 5vw;
  }
  .logo{
    margin: 20px auto;
  }
  .logo img{
    width: 80vw;
  }
}
</style>
