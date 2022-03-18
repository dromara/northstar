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
      '/': {
        target: `http://localhost:${process.env.PORT}`
      }
    }
  }
}
