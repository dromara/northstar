<template>
  <el-dialog width="300px" title="合约查询" :visible="visible" append-to-body>
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
      <el-button @click="$emit('update:visible', false)">返 回</el-button>
    </div>
  </el-dialog>
</template>

<script>
import gatewayDataApi from '@/api/gatewayDataApi'

const filterMethods = {
  INDEX: (item) => item.unifiedsymbol.endsWith('FUTURES') && item.name.endsWith('指数'),
  FUTURES: (item) => item.unifiedsymbol.endsWith('FUTURES') && !item.name.endsWith('指数')
}

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
        { value: 'INDEX', label: '指数合约' },
        { value: 'FUTURES', label: '期货合约' },
        { value: 'OPTION', label: '期权合约', disabled: true },
        { value: 'OTHERS', label: '其他合约', disabled: true }
      ]
    }
  },
  watch: {
    gateway: async function () {
      this.unifiedSymbol = ''
      this.contractList = await gatewayDataApi.getContracts(this.gateway)
      console.log('gateway updated', this.contractList)
    }
  },
  computed: {
    gatewayList() {
      return ['CTP', 'SIM']
    },
    gwContractList() {
      if (!filterMethods[this.contractType]) return []
      return this.contractList.filter(filterMethods[this.contractType])
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
    }
  }
}
</script>

<style>
.text-selectable {
  padding-right: 10px;
}
</style>
