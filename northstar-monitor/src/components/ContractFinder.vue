<template>
  <el-dialog width="300px" title="合约查询" :visible="visible" append-to-body :before-close="close">
    <el-form label-width="100px">
      <el-form-item label="网关类别">
        <el-select v-model="gatewayType">
          <el-option v-for="gw in gatewayTypes" :label="gw" :value="gw" :key="gw"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约类型">
        <el-select v-model="contractType">
          <el-option v-for="t in contractTypeOptions" :label="t" :value="t" :key="t"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约列表">
        <el-select v-model="unifiedSymbol" filterable>
          <el-option
            v-for="(c, i) in contractList"
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
import contractApi from '@/api/contractApi'
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
      gatewayType: '',
      contractType: '',
      unifiedSymbol: '',
      gatewayTypes: [],
      contractList: [],
      contractTypeOptions: []
    }
  },
  watch: {
    gatewayType: function (val) {
      this.contractType = ''
      this.unifiedSymbol = ''
      contractApi.getContractProviders(val).then((result) => {
        this.contractTypeOptions = result
      })
    },
    contractType: function (val) {
      if (val) {
        contractApi.getContractList(val).then((result) => {
          this.contractList = result
            .map((item) => ContractField.deserializeBinary(item).toObject())
            .sort((a, b) => a['unifiedsymbol'].localeCompare(b['unifiedsymbol']))
        })
      }
    }
  },
  mounted() {
    gatewayMgmtApi.getGatewayTypeDescriptions().then((result) => {
      this.gatewayTypes = result.filter((item) => !item.adminOnly).map((item) => item.name)
    })
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
