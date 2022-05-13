<template>
  <el-dialog
    :title="readOnly ? '查看' : this.module ? '修改' : '新增'"
    :visible="visible"
    :close-on-click-modal="false"
    :show-close="false"
    class="module-dialog"
    width="540px"
  >
    <ContractFinder :visible.sync="contractFinderVisible" />
    <el-alert
      v-if="this.module && !this.readOnly"
      class="mb-10"
      title="修改模组会重置模组除持仓状态外的所有属性状态，请知悉"
      type="warning"
      show-icon
      :closable="false"
    >
    </el-alert>
    <el-container>
      <el-aside width="150px" :style="{ overflow: 'hidden' }">
        <el-menu :default-active="activeIndex" @select="handleSelect">
          <el-menu-item index="1">
            <i class="el-icon-setting"></i>
            <span slot="title">基础信息</span>
          </el-menu-item>
          <el-menu-item index="2">
            <i class="el-icon-s-opportunity"></i>
            <span slot="title">交易策略</span>
          </el-menu-item>
          <el-menu-item index="3">
            <i class="el-icon-s-custom"></i>
            <span slot="title">账户绑定</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-compact"
        ><el-form :model="form" label-width="100px" class="module-form" inline :rules="formRules">
          <div v-if="activeIndex === '1'">
            <el-form-item label="模组名称">
              <el-input v-model="form.moduleName" :disabled="readOnly || this.module"></el-input>
            </el-form-item>
            <el-form-item value label="模组类型">
              <el-select v-model="form.type" :disabled="readOnly">
                <el-option label="投机" value="SPECULATION"></el-option>
                <el-option label="套利" value="ARBITRAGE"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="绑定合约">
              <el-tooltip
                class="item"
                effect="dark"
                content="如有多个合约用 ; 分隔"
                placement="bottom-end"
              >
                <el-input v-model="form.moduleName" :disabled="readOnly || this.module"></el-input>
              </el-tooltip>
            </el-form-item>
            <el-form-item label="平仓优化">
              <el-select v-model="form.type" :disabled="readOnly">
                <el-option label="先开先平" value="FIFO"></el-option>
                <el-option label="平今优先" value="PRIOR_TODAY"></el-option>
                <el-option label="平昨锁今" value="PRIOR_BEFORE_HEGDE_TODAY"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="K线周期">
              <el-input-number v-model="num" @change="handleChange" :min="1" />
              <span class="ml-10">分钟</span>
            </el-form-item>
            <el-form-item label="预热数据量">
              <el-input-number v-model="num" @change="handleChange" :min="1" />
              <span class="ml-10">根K线</span>
            </el-form-item>
          </div>
          <div v-if="activeIndex === '2'">
            <el-form-item v-if="activeIndex === '2'" label="信号策略">
              <el-select placeholder="请选择" key="策略" :disabled="readOnly">
                <el-option
                  v-for="(p, i) in tradeStrategyOptions"
                  :label="p.componentMeta.name"
                  :value="p.componentMeta.name"
                  :key="i"
                ></el-option>
              </el-select>
            </el-form-item>
            <div v-for="(policy, i) in tradeStrategyOptions" :key="i">
              <div v-if="chosenSignalPolicy === policy.componentMeta.name">
                <el-form-item
                  v-for="(param, index) in policy.initParams"
                  :label="param.label"
                  :key="param.field"
                >
                  <el-select
                    v-if="param.type === 'Options'"
                    v-model="tradeStrategyOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :disabled="readOnly"
                  >
                    <el-option v-for="(item, i) in param.options" :value="item" :key="i">{{
                      item
                    }}</el-option>
                  </el-select>
                  <el-input
                    v-else
                    v-model="tradeStrategyOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :type="param.type.toLowerCase()"
                    :disabled="readOnly"
                  />
                  <span v-if="param.unit" class="value-unit">{{ param.unit }}</span>
                </el-form-item>
              </div>
            </div>
          </div>
          <div v-if="activeIndex === '3'"></div>
        </el-form>
      </el-main>
    </el-container>

    <div slot="footer" class="dialog-footer">
      <el-button v-if="!readOnly" type="primary" @click="contractFinderVisible = true"
        >合约查询</el-button
      >
      <el-button @click="close">取 消</el-button>
      <el-button v-if="!readOnly" type="primary" @click="saveSetting">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import ContractFinder from './ContractFinder'
import gatewayMgmtApi from '../api/gatewayMgmtApi'
import moduleApi from '../api/moduleApi'

const initComponent = async (component, arr) => {
  const paramsMap = await moduleApi.componentParams(component)
  arr.push({
    componentMeta: component,
    initParams: Object.values(paramsMap).sort((a, b) => a.order - b.order)
  })
}

export default {
  components: {
    ContractFinder
  },
  props: {
    readOnly: {
      type: Boolean,
      default: false
    },
    visible: {
      type: Boolean,
      default: false
    },
    module: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      contractFinderVisible: false,
      accountOptions: [],
      tradeStrategyOptions: [],
      activeIndex: '1',
      chosenStrategy: '',
      form: {
        moduleName: '',
        type: '',
        numOfMinPerBar: '',
        numOfBarForPreparation: '',
        closingPolicy: '',
        accountGatewayId: '',
        strategySetting: {
          componentMeta: {},
          initParams: []
        }
      }
    }
  },
  mounted() {
    this.initData()
  },
  watch: {
    visible: function (val) {
      if (val) {
        if (!this.module) {
          return
        }
        this.form = Object.assign({}, this.module)

        this.chosenSignalPolicy = this.module.signalPolicy.componentMeta.name
        this.tradeStrategyOptions.forEach((p) => {
          if (p.componentMeta.name === this.chosenSignalPolicy) {
            p.initParams = this.module.signalPolicy.initParams
          }
        })

        // this.chosenDealer = this.module.dealer.componentMeta.name
        // this.dealerOptions.forEach((d) => {
        //   if (d.componentMeta.name === this.chosenDealer) {
        //     d.initParams = this.module.dealer.initParams
        //   }
        // })
      }
    }
  },
  methods: {
    initData() {
      gatewayMgmtApi
        .findAll('TRADE')
        .then((result) => (this.accountOptions = result.map((i) => i.gatewayId)))
      moduleApi.getStrategies().then((strategyMetas) => {
        strategyMetas.forEach(async (i) => initComponent(i, this.tradeStrategyOptions))
      })
    },
    handleSelect(index) {
      this.activeIndex = index
    },
    saveSetting() {
      let pass =
        this.assertTrue(this.form.moduleName, '未指定模组名称') &&
        this.assertTrue(this.form.type, '未指定模组类型') &&
        this.assertTrue(this.form.accountGatewayId, '未指定绑定账户') &&
        this.assertTrue(this.form.signalPolicy.componentMeta.name, '未指定信号策略') &&
        this.assertTrue(this.form.dealer.componentMeta.name, '未指定交易策略')

      if (!pass) {
        return
      }

      const obj = Object.assign({}, this.form)
      this.$emit('onSave', obj)
      this.close()
    },
    close() {
      this.activeIndex = '1'
      Object.assign(this.$data, this.$options.data())
      this.$nextTick(this.initData)
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

<style scoped>
.main-compact {
  padding-bottom: 0px;
}
.module-dialog {
  min-width: 376px;
}
.module-form .el-input,
.module-form .el-select {
  width: 200px;
}

.value-unit {
  /* font-size: 16px; */
  padding-left: 5px;
}
.module-form .with-unit {
  width: 100px;
}
</style>
