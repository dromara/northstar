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
      <el-form ref="positionInfo" :model="form" label-width="70px" width="200px">
        <el-form-item label="合约代码" prop="contractId">
          <el-select v-model="form.contractId">
            <el-option
              v-for="c in contractOptions"
              :key="c.value"
              :value="c.value"
              :label="c.name"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="成交方向" prop="positionDir">
          <el-select v-model="directionComb">
            <el-option :value="['D_Buy', 'OF_Open']" label="多开"></el-option>
            <el-option :value="['D_Sell', 'OF_Open']" label="空开"></el-option>
            <el-option :value="['D_Buy', 'OF_Close']" label="多平"></el-option>
            <el-option :value="['D_Sell', 'OF_Close']" label="空平"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="成交价" prop="price">
          <el-input v-model="form.price" type="number" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item id="editPositionVol" label="数量" prop="volume">
          <el-input v-model="form.volume" type="number" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button id="savePosition" type="primary" @click="savePosition">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import moduleApi from '@/api/moduleApi'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    contractOptions: {
      type: Object,
      default: () => {}
    },
    moduleName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      form: {
        contractId: '',
        direction: '',
        offsetFlag: '',
        price: '',
        volume: ''
      },
      directionComb: ''
    }
  },
  watch: {
    directionComb: function (val) {
      this.form.direction = val[0]
      this.form.offsetFlag = val[1]
    }
  },
  methods: {
    async savePosition() {
      let flag =
        this.assertTrue(this.form.contractId, '未指定合约代码') &&
        this.assertTrue(this.form.direction, '未指定成交方向') &&
        this.assertTrue(this.form.price, '未设置成交价') &&
        this.assertTrue(this.form.volume, '未设置数量')
      if (!flag) return

      await moduleApi.mockTradeAdjustment(this.moduleName, this.form)
      this.close()
    },
    close() {
      this.$refs.positionInfo.resetFields()
      this.$emit('update:visible', false)
      this.$emit('save')
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
