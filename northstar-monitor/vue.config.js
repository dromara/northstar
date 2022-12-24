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
	  port: 8090,
    https: true,
    proxy: {
      '/northstar': {
        target: `https://localhost`
      },
      '/redirect*': {
        target: `https://localhost`,
      },
    }
  }
}
