<template>
  <el-dialog
    :title="`持仓调整`"
    :visible="visible"
    :close-on-click-modal="false"
    :show-close="false"
    width="200px"
    append-to-body
  >
    <div class="warning-text pb-20">
      <i class="el-icon-warning" /> 该操作仅用于手工同步模组持仓状态，请谨慎使用<br />
      <i class="el-icon-warning" /> 持仓调整实际上是生成一个手工的成交单
    </div>
    <div class="form-wrapper">
      <el-form ref="positionInfo" :model="form" label-width="70px" width="200px" formRules>
        <el-form-item label="合约代码" prop="unifiedSymbol">
          <el-input v-model="form.unifiedSymbol" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="成交方向" prop="positionDir">
          <el-select v-model="form.positionDir">
            <el-option :value="[1, 1]" label="多开"></el-option>
            <el-option :value="[2, 1]" label="空开"></el-option>
            <el-option :value="[1, 2]" label="多平"></el-option>
            <el-option :value="[2, 2]" label="空平"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="成交价" prop="price">
          <el-input v-model="form.price" type="number" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="手数" prop="volume">
          <el-input v-model="form.volume" type="number" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="savePosition">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  props: {
    contractOptions: {
      type: Array,
      default: () => []
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      form: {
        unifiedSymbol: '',
        direction: '',
        price: '',
        volume: ''
      }
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        Object.assign(this.form, this.data)
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
      this.close()
    },
    close() {
      this.$refs.positionInfo.resetFields()
      this.$emit('update:visible', false)
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
.form-wrapper {
  width: 250px;
  margin: auto;
}
</style>
