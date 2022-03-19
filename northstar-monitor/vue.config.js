module.exports = {
  // 选项...
  productionSourceMap: false,
  configureWebpack: {
    externals: {
      vue: 'Vue',
      'element-ui': 'ELEMENT'
    }
  },
  devServer: {
    proxy: {
      '/northstar': {
        target: `http://localhost`
      }
    }
  }
}
