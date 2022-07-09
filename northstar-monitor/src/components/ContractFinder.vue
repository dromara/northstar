<template>
  <el-dialog width="300px" title="合约查询" :visible="visible" append-to-body :before-close="close">
    <el-form label-width="100px">
      <el-form-item label="网关列表">
        <el-select v-model="gateway">
          <el-option v-for="gw in gatewayList" :label="gw" :value="gw" :key="gw"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约类型">
        <el-select v-model="contractType">
          <el-option
            v-for="t in contractTypeOptions"
            :label="t.label"
            :value="t.value"
            :key="t.value"
            :disabled="t.disabled"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约列表">
        <el-select v-model="unifiedSymbol" filterable>
          <el-option
            v-for="(c, i) in gwContractList"
            :label="c.name"
            :value="c.unifiedsymbol"
            :key="i"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约ID">
        <span ref="symbolText" class="text-selectable">{{ unifiedSymbol }}</span>
        <el-button
          class="compact"
          size="mini"
          title="复制"
          @click="copy"
          icon="el-icon-document-copy"
          :disabled="!unifiedSymbol"
        ></el-button>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">返 回</el-button>
    </div>
  </el-dialog>
</template>

<script>
import gatewayMgmtApi from '@/api/gatewayMgmtApi'
import { ContractField } from '@/lib/xyz/redtorch/pb/core_field_pb'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      gateway: '',
      contractType: '',
      unifiedSymbol: '',
      contractList: [],
      contractTypeOptions: [
        { value: 2, label: '期货合约' },
        { value: 3, label: '期权合约' }
      ]
    }
  },
  watch: {
    gateway: async function () {
      this.unifiedSymbol = ''
      try {
        await this.updateContractList()
      } catch (e) {
        this.$message.error(e.message)
      }
    }
  },
  computed: {
    gatewayList() {
      return ['CTP', 'SIM']
    },
    gwContractList() {
      return this.contractList.filter((item) => item.productclass === this.contractType)
    }
  },
  methods: {
    copy() {
      let range = document.createRange()
      let refNode = this.$refs.symbolText
      range.selectNodeContents(refNode)
      let selection = window.getSelection()
      selection.removeAllRanges()
      selection.addRange(range)
      document.execCommand('Copy')
    },
    async updateContractList() {
      this.contractList = await gatewayMgmtApi.getSubscribedContracts(this.gateway)
      this.contractList = this.contractList
        .map((item) => ContractField.deserializeBinary(item).toObject())
        .sort((a, b) => a['unifiedsymbol'].localeCompare(b['unifiedsymbol']))
    },
    close() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style>
.text-selectable {
  padding-right: 10px;
}
</style>
