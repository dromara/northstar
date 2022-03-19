<template>
  <el-dialog
    :title="`${isUpdateMode ? '修改' : '新增'}持仓`"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    :show-close="false"
    width="200px"
    append-to-body
  >
    <div class="warning-text">
      <i class="el-icon-warning" /> 该操作仅用于手工同步模组持仓状态，请谨慎使用<br />
      <i class="el-icon-warning" /> 合约需要与交易策略设定一致，否则盈亏不会更新
    </div>
    <ContractFinder :visible.sync="contractFinderVisible" />
    <div class="form-wrapper">
      <el-form ref="positionInfo" :model="form" label-width="70px" width="200px" :rules="formRules">
        <el-form-item label="合约代码" prop="unifiedSymbol">
          <el-input v-model="form.unifiedSymbol" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="持仓方向" prop="positionDir">
          <el-select v-model="form.positionDir">
            <el-option value="PD_Long" label="多"></el-option>
            <el-option value="PD_Short" label="空"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开仓价" prop="openPrice">
          <el-input v-model="form.openPrice" type="number" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="止损价" prop="stopLossPrice">
          <el-input v-model="form.stopLossPrice" type="number" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="手数" prop="volume">
          <el-input v-model="form.volume" type="number" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="contractFinderVisible = true">合约查询</el-button>
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="savePosition">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import ContractFinder from './ContractFinder.vue'
export default {
  components: {
    ContractFinder
  },
  props: {
    data: {
      type: Object,
      default: () => {}
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      contractFinderVisible: false,
      form: {
        unifiedSymbol: '',
        positionDir: '',
        openPrice: '',
        stopLossPrice: '',
        volume: ''
      }
    }
  },
  computed: {
    isUpdateMode() {
      return !!this.data
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
        Object.assign(this.form, this.data)
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
      }
    }
  },
  methods: {
    savePosition() {
      let flag =
        this.assertTrue(this.form.unifiedSymbol, '未指定合约代码') &&
        this.assertTrue(this.form.positionDir, '未指定持仓方向') &&
        this.assertTrue(this.form.openPrice, '未设置开仓价') &&
        this.assertTrue(this.form.volume, '未设置手数')
      if (!flag) return

      this.$emit('save', this.form)
      this.dialogVisible = false
    },
    close() {
      this.$refs.positionInfo.resetFields()
      this.dialogVisible = false
    },
    assertTrue(expression, errMsg) {
      if (!expression) {
        this.$message.error(errMsg)
        return false
      }
      return true
    }
  }
}
</script>

<style>
.warning-text {
  color: #f1f43c;
  margin-bottom: 18px;
}
.form-wrapper {
  width: 250px;
  margin: auto;
}
</style>
