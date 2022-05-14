<template>
  <div class="wrapper">
    <div class="logo"><img width="700px" src="../assets/logo.png" />v{{ version }}</div>
    <div class="panel" v-on:keydown.enter="login">
      <el-form :model="userForm" status-icon label-width="80px" class="demo-userForm">
        <el-form-item label="用户名">
          <el-input v-model="userForm.name" clearable></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="userForm.pass" autocomplete="off" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button @click="login">提交</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import loginApi from '@/api/loginApi'
import { version } from '@/../package.json'
export default {
  data() {
    return {
      userForm: {
        name: '',
        pass: ''
      },
      hostUrl: '',
      version: version
    }
  },
  methods: {
    async login() {
      await loginApi.login(this.userForm.name, this.userForm.pass)
      console.log('登陆成功')
      this.$router.push({ name: 'mktgateway' })
    },
    resetForm() {}
  }
}
</script>

<style>
.panel {
  /* width: 20%; */
  width: 300px;
  min-width: 300px;
  margin: auto;
}
.logo {
}
.wrapper {
  display: flex;
  margin: auto;
  flex-direction: column;
}
</style>
