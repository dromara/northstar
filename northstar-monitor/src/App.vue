<template>
  <div id="app">
    <router-view />
    <socket-connection />
  </div>
  
</template>
<script>
import SocketConnection from '@/components/SocketConnection'

export default {
  components:{
    SocketConnection
  }
}
</script>


<style lang="scss">
#app {
  height: 100%;
  display: flex;
}
</style>
