<template>
  <el-dialog width="300px" title="合约查询" :visible.sync="dialogVisible" append-to-body>
    <el-form label-width="100px">
      <el-form-item label="网关列表">
        <el-select v-model="gateway" filterable>
          <el-option v-for="gw in gatewayList" :label="gw" :value="gw" :key="gw"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="合约列表">
        <el-select v-model="unifiedSymbol" filterable>
          <el-option
            v-for="(c, i) in gwContractList"
            :label="c.name"
            :value="c.unifiedSymbol"
            :value-key="c.unifiedSymbol"
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
      <el-button @click="dialogVisible = false">返 回</el-button>
    </div>
  </el-dialog>
</template>

<script>
import dataSyncApi from '@/api/dataSyncApi'
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      gateway: '',
      unifiedSymbol: '',
      contractList: []
    }
  },
  watch: {
    gateway: function () {
      this.unifiedSymbol = ''
    },
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
        this.unifiedSymbol = ''
      }
    }
  },
  computed: {
    gatewayList() {
      const gatewayMap = {}
      this.contractList.forEach((i) => (gatewayMap[i.gatewayId] = true))
      return Object.keys(gatewayMap)
    },
    gwContractList() {
      return this.contractList.filter((i) => i.gatewayId === this.gateway)
    }
  },
  created() {
    const sortFunc = (a, b) => {
      return a['unifiedSymbol'].localeCompare(b['unifiedSymbol'])
    }

    dataSyncApi.getAvailableContracts().then((list) => {
      console.log('合约总数', list.length)
      this.contractList = list
      this.contractList.sort(sortFunc)
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
    }
  }
}
</script>

<style>
.text-selectable {
  padding-right: 10px;
}
</style>
