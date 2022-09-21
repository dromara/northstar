<template>
  <el-dialog title="盈亏曲线" :visible="visible" width="80%" append-to-body :before-close="close">
    <div class="perf-chart-wrapper">
      <module-performance
        :moduleInitBalance="moduleInitBalance"
        :moduleDealRecords="moduleDealRecords"
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
  methods: {
    close() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style>
.perf-chart-wrapper {
  height: 600px;
}
</style>
