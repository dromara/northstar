<template>
  <div id="app">
    <router-view />
  </div>
</template>
<script>

console.log('env:', process.env.NODE_ENV)
export default {}
</script>

<style lang="scss">
#app {
  height: 100%;
  display: flex;
}
</style>
