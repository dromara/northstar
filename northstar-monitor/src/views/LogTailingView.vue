<template>
  <div class="ns-page flex-col">
    <div class="ns-page-header">
      <h1 v-if="mode === 'platform'">交易平台日志</h1>
      <h1 v-else>{{ moduleName }}模组日志</h1>
    </div>
    <div class="ns-page-action">
      <el-form inline label-width="70px">
        <el-form-item label="打印级别">
          <el-button-group class="mr-20">
            <el-button :type="logLevel === 'ERROR' ? 'primary' : ''" @click="logLevel = 'ERROR'">
              ERROR
            </el-button>
            <el-button :type="logLevel === 'WARN' ? 'primary' : ''" @click="logLevel = 'WARN'">
              WARN
            </el-button>
            <el-button :type="logLevel === 'INFO' ? 'primary' : ''" @click="logLevel = 'INFO'">
              INFO
            </el-button>
            <el-button :type="logLevel === 'DEBUG' ? 'primary' : ''" @click="logLevel = 'DEBUG'">
              DEBUG
            </el-button>
            <el-button :type="logLevel === 'TRACE' ? 'primary' : ''" @click="logLevel = 'TRACE'">
              TRACE
            </el-button>
          </el-button-group>
        </el-form-item>
        <el-form-item label="刷新频率">
          <el-button-group class="mr-20">
            <el-button
              :type="updateInterval === 1000 ? 'primary' : ''"
              @click="updateInterval = 1000"
              >1秒</el-button
            >
            <el-button
              :type="updateInterval === 2000 ? 'primary' : ''"
              @click="updateInterval = 2000"
              >2秒</el-button
            >
            <el-button
              :type="updateInterval === 5000 ? 'primary' : ''"
              @click="updateInterval = 5000"
              >5秒</el-button
            >
          </el-button-group>
        </el-form-item>
        <el-form-item label="显示行数">
          <el-button-group class="mr-20">
            <el-button
              :type="numOfLinesInView === 100 ? 'primary' : ''"
              @click="numOfLinesInView = 100"
              >100行</el-button
            >
            <el-button
              :type="numOfLinesInView === 200 ? 'primary' : ''"
              @click="numOfLinesInView = 200"
              >200行</el-button
            >
            <el-button
              :type="numOfLinesInView === 500 ? 'primary' : ''"
              @click="numOfLinesInView = 500"
              >500行</el-button
            >
          </el-button-group>
        </el-form-item>
        <el-form-item label="自动滚动">
          <el-switch v-model="autoScroll"></el-switch>
        </el-form-item>
      </el-form>
    </div>

    <div ref="logView" v-if="logContent.length" class="ns-page-body">
      <div v-for="(line, i) in logContent" :key="i">{{ line }}</div>
    </div>
    <div
      v-else
      class="ns-page-body-placeholder flex align-center"
      v-loading="loading"
      element-loading-background="rgba(0, 0, 0, 0.3)"
    >
      没有日志数据
    </div>
  </div>
</template>

<script>
import logApi from '@/api/logApi'

const LOG_UPDATE_INTERVAL = 'logUpdateInterval'
const LOG_NUM_OF_LINES_IN_VIEW = 'numOfLinesInView'
export default {
  data() {
    return {
      mode: 'platform',
      moduleName: '',
      logLevel: '',
      updateInterval: 1000,
      numOfLinesInView: 100,
      positionOffset: 0,
      logContent: [],
      timer: '',
      autoScroll: true,
      lastInit: 0,
      loading: true
    }
  },
  watch: {
    updateInterval: function (val) {
      localStorage.setItem(LOG_UPDATE_INTERVAL, val)
    },
    numOfLinesInView: function (val) {
      localStorage.setItem(LOG_NUM_OF_LINES_IN_VIEW, val)
    },
    logLevel: function (val) {
      if (this.mode === 'platform') {
        logApi.setPlatformLogLevel(val)
      } else {
        logApi.setModuleLogLevel(this.moduleName, val)
      }
    }
  },
  mounted() {
    this.pageInit()
  },
  destroyed() {
    clearTimeout(this.timer)
  },
  methods: {
    async pageInit() {
      const time = new Date().getTime()
      if (time - this.lastInit < 1000) {
        return
      }
      this.lastInit = time
      this.updateInterval = parseInt(localStorage.getItem(LOG_UPDATE_INTERVAL) || 1000)
      this.numOfLinesInView = parseInt(localStorage.getItem(LOG_NUM_OF_LINES_IN_VIEW) || 500)
      const moduleName = this.$route.params.module
      console.log('route', this.$route)
      if (moduleName) {
        this.mode = 'module'
        this.moduleName = moduleName
        this.logLevel = await logApi.getModuleLogLevel(moduleName)
        this.tailModuleLog(moduleName)
      } else {
        this.mode = 'platform'
        this.logLevel = await logApi.getPlatformLogLevel()
        this.tailPlatformLog()
      }
    },
    tailPlatformLog() {
      logApi.tailPlatformLog(this.positionOffset, this.numOfLinesInView).then((result) => {
        this.tailingLog(result)
        this.timer = setTimeout(this.tailPlatformLog, this.updateInterval)
      })
    },
    tailModuleLog(moduleName) {
      logApi
        .tailModuleLog(moduleName, this.positionOffset, this.numOfLinesInView)
        .then((result) => {
          this.tailingLog(result)
          this.timer = setTimeout(() => {
            this.tailModuleLog(moduleName)
          }, this.updateInterval)
        })
    },
    tailingLog(logDescription) {
      this.positionOffset = logDescription.endPosition
      this.logContent = this.logContent.concat(logDescription.linesOfLog)
      if (this.logContent.length > this.numOfLinesInView) {
        this.logContent = this.logContent.filter(
          (item, i) => i >= this.logContent.length - this.numOfLinesInView
        )
      }
      this.loading = false
      if (this.autoScroll) {
        this.$nextTick(() => {
          if (this.$refs.logView) {
            this.$refs.logView.scrollTop = this.$refs.logView.scrollHeight
          }
        })
      }
    }
  }
}
</script>

<style>
.ns-page-header h1 {
  margin-top: 0;
}
.ns-page-body {
  overflow: auto;
  overflow-wrap: anywhere;
}
.ns-page-body-placeholder {
  height: 100%;
}
</style>
