<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="合约订阅配置"
    append-to-body
    :close-on-click-modal="false"
    :show-close="false"
    width="700px"
    destroy-on-close
  >
    <div class="warning-text">
      <i class="el-icon-warning" /> 合约名称，例如螺纹钢；合约编码，例如rb2205。
    </div>
    <div class="warning-text">
      <i class="el-icon-warning" />
      实盘合约数据依赖实盘账户。如没有合约数据，请先创建实盘账户。详情请咨询社群
    </div>
    <div class="align-center mt-10">
      <el-transfer
        filterable
        :filter-method="filterMethod"
        :titles="['可选择合约', '已选择合约']"
        filter-placeholder="请输入合约名称或编码"
        v-model="value"
        :data="optionData"
      >
      </el-transfer>
    </div>
    <div slot="footer" class="dialog-footer mt-10">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="onSave">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import gatewayDataApi from '@/api/gatewayDataApi'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    gatewayType: {
      type: String,
      default: ''
    },
    chosenValues: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      dialogVisible: false,
      options: [],
      value: [],
      filterMethod() {
        return true
      }
    }
  },
  computed: {
    optionData() {
      return this.options.map((item, i) => Object.assign({ label: item.name, key: i }, item))
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
      }
    },
    gatewayType: async function (val) {
      if (val) {
        this.options = await gatewayDataApi.getContracts(this.gatewayType)
      }
    }
  },
  methods: {
    onSave() {
      this.$emit('save', this.value)
    }
  }
}
</script>

<style></style>
