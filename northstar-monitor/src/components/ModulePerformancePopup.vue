<template>
  <el-dialog title="盈亏曲线" :visible="visible" width="80%" append-to-body top="5vh" :before-close="close">
    <div class="perf-chart-wrapper">
      <module-performance
        ref="chartContainer"
        :moduleInitBalance="moduleInitBalance"
        :moduleDealRecords="moduleDealRecords"
        :largeView="true"
      />
    </div>
  </el-dialog>
</template>

<script>
import ModulePerformance from './ModulePerformance.vue'

export default {
  components: {
    ModulePerformance
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    moduleInitBalance: {
      type: Number,
      default: 0
    },
    moduleDealRecords: {
      type: Array,
      default: () => []
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.$nextTick(() => {
          this.$refs.chartContainer.refresh()
        })
      }
    }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style>
.perf-chart-wrapper {
  height: 80vh;
}
</style>
