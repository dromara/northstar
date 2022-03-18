<template>
  <el-dialog
    width="300px"
    title="模拟网关出入金"
    :visible="visible"
    append-to-body
    @close="onClose"
  >
    <el-row>
      <el-col :span="24">
        <span class="row-lh">账户ID：{{ simGatewayId }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="24">
        <span class="row-lh">账户余额： {{ accountBalance || 0 | accountingFormatter }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="24">
        <span class="row-lh">账户可用金额： {{ accountAvailable || 0 | accountingFormatter }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="7">
        <span class="row-lh">出入金额：</span>
      </el-col>
      <el-col :span="17">
        <el-input v-model="money" clearable type="number" />
      </el-col>
    </el-row>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="moneyIO">出入金</el-button>
    </div>
  </el-dialog>
</template>

<script>
import gatewayMgmtApi from '@/api/gatewayMgmtApi'
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    simGatewayId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      money: '',
      accountBalance: '',
      accountAvailable: ''
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.moneyIO()
        this.updateAmount()
      }
    }
  },
  mounted() {
    this.updateAmount()
  },
  methods: {
    onClose() {
      this.$emit('update:visible', false)
    },
    async moneyIO() {
      console.log(this.simGatewayId)
      await gatewayMgmtApi.moneyIO(this.simGatewayId, this.money || 0)
      this.money = ''
      setTimeout(() => {
        this.updateAmount()
      }, 300)
    },
    updateAmount() {
      this.accountBalance = this.$store.getters.getAccountById(this.simGatewayId).account?.balance
      this.accountAvailable = this.$store.getters.getAccountById(
        this.simGatewayId
      ).account?.available
    }
  }
}
</script>

<style>
.row-lh {
  line-height: 32px;
}
</style>
