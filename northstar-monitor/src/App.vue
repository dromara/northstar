<template>
  <div id="app">
    <router-view />
  </div>
</template>
<script>
console.log('env:', process.env.NODE_ENV)
const isEE = !!(window.require && window.require('electron'))
if(isEE){
  const {ipcRenderer} = window.require('electron')
  const remote = window.require('@electron/remote')
  const { FindInPage } = window.require('electron-find')
  let findInPage = new FindInPage(remote.getCurrentWebContents(), {
    preload: true,
    offsetTop: 15,
    offsetRight: 200,
  })
  ipcRenderer.on('on-find', () => {
    findInPage.openFindWindow()
  })
}

export default {}
</script>

<style lang="scss">
#app {
  height: 100%;
  display: flex;
}
</style>
