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
        ><el-form :model="form" label-width="100px" class="module-form" inline>
          <div v-show="activeIndex === '1'">
            <el-form-item label="模组名称">
              <el-input
                v-model="form.moduleName"
                :maxlength="16"
                :disabled="readOnly || this.module"
              ></el-input>
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
                <el-input v-model="bindedContracts" :disabled="readOnly"></el-input>
              </el-tooltip>
            </el-form-item>
            <el-form-item label="平仓优化">
              <el-select v-model="form.closingPolicy" :disabled="readOnly">
                <el-option label="先开先平" value="FIFO"></el-option>
                <el-option label="平今优先" value="PRIOR_TODAY"></el-option>
                <el-option label="平昨锁今" value="PRIOR_BEFORE_HEGDE_TODAY"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="K线周期">
              <el-input-number :disabled="readOnly" v-model="form.numOfMinPerBar" :min="1" />
              <span class="ml-10">分钟</span>
            </el-form-item>
            <el-form-item label="预热数据量">
              <el-input-number
                v-model="form.numOfBarForPreparation"
                :min="0"
                :disabled="readOnly"
              />
              <span class="ml-10">根K线</span>
            </el-form-item>
          </div>
          <div v-show="activeIndex === '2'">
            <el-form-item label="交易策略">
              <el-select v-model="form.strategySetting" placeholder="请选择" :disabled="readOnly">
                <el-option
                  v-for="(p, i) in tradeStrategyOptions"
                  :label="p.componentMeta.name"
                  :value="p"
                  :key="i"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              v-for="(param, index) in form.strategySetting.initParams"
              :label="param.label"
              :key="param.field"
            >
              <el-select
                v-if="param.type === 'Options'"
                v-model="form.strategySetting.initParams[index]['value']"
                :class="param.unit ? 'with-unit' : ''"
                :disabled="readOnly"
              >
                <el-option v-for="(item, i) in param.options" :value="item" :key="i">{{
                  item
                }}</el-option>
              </el-select>
              <el-input
                v-else
                v-model="form.strategySetting.initParams[index]['value']"
                :class="param.unit ? 'with-unit' : ''"
                :type="param.type.toLowerCase()"
                :disabled="readOnly"
              />
              <span v-if="param.unit" class="value-unit"> {{ param.unit }}</span>
            </el-form-item>
          </div>
          <div v-show="activeIndex === '3'">
            <el-form-item label="绑定账号">
              <el-select
                v-model="choseAccounts"
                placeholder="请选择账户"
                multiple
                :disabled="readOnly"
                @change="accountSelected"
              >
                <el-option
                  v-for="(acc, i) in accountOptions"
                  :label="acc.gatewayId"
                  :value="acc"
                  :key="i"
                ></el-option>
              </el-select>
            </el-form-item>
            <div v-for="(acc, i) in form.moduleAccountSettingsDescription" :key="i">
              <el-divider content-position="left"
                >账户：{{ form.moduleAccountSettingsDescription[i].accountGatewayId }}</el-divider
              >
              <el-form-item label="模组分配金额">
                <el-input
                  v-model="form.moduleAccountSettingsDescription[i].moduleAccountInitBalance"
                  type="number"
                  :disabled="readOnly"
                />
              </el-form-item>
              <el-form-item label="关联合约">
                <el-select
                  v-model="form.moduleAccountSettingsDescription[i].bindedUnifiedSymbols"
                  multiple
                >
                  <el-option
                    v-for="(item, i) in bindedUnifiedSymbolsOptions"
                    :value="item.value"
                    :label="item.label"
                    :key="i"
                  />
                </el-select>
              </el-form-item>
            </div>
          </div>
        </el-form>
      </el-main>
    </el-container>

    <div slot="footer" class="dialog-footer">
      <el-button v-if="!readOnly" type="primary" @click="contractFinderVisible = true">
        合约查询
      </el-button>
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
      choseAccounts: [],
      bindedContracts: '',
      form: {
        moduleName: '',
        type: 'SPECULATION',
        numOfMinPerBar: '1',
        numOfBarForPreparation: '0',
        closingPolicy: 'FIFO',
        moduleAccountSettingsDescription: [],
        strategySetting: {
          componentMeta: {},
          initParams: [],
          value: ''
        }
      }
    }
  },
  computed: {
    bindedUnifiedSymbolsOptions() {
      const unifiedSymbols = this.bindedContracts.split(/;|；/).map((item) => item.trim())
      return unifiedSymbols.map((us) => ({ label: us.split('@')[0], value: us }))
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
        this.form = this.module
        this.bindedContracts = this.module.moduleAccountSettingsDescription
          .map((item) => item.bindedUnifiedSymbols.join(';'))
          .join(';')
        this.choseAccounts = this.module.moduleAccountSettingsDescription.map((item) => {
          item.value = item.accountGatewayId
          return item
        })
      }
    }
  },
  methods: {
    initData() {
      gatewayMgmtApi.findAll('TRADE').then((result) => {
        this.accountOptions = result.map((item) => {
          item.value = item.gatewayId
          return item
        })
      })
      moduleApi.getStrategies().then((strategyMetas) => {
        strategyMetas.forEach(async (i) => initComponent(i, this.tradeStrategyOptions))
      })
    },
    handleSelect(index) {
      this.activeIndex = index
    },
    accountSelected(val) {
      if (!val.length) return
      this.form.moduleAccountSettingsDescription = val.map((item) => {
        return {
          accountGatewayId: item.gatewayId,
          moduleAccountInitBalance: 0,
          bindedUnifiedSymbols: []
        }
      })
    },
    saveSetting() {
      let pass =
        this.assertTrue(this.form.moduleName, '未指定模组名称') &&
        this.assertTrue(this.form.type, '未指定模组类型') &&
        this.assertTrue(this.form.numOfMinPerBar, '未指定K线周期') &&
        this.assertTrue(this.form.strategySetting.componentMeta.name, '未指定交易策略') &&
        this.assertTrue(this.form.moduleAccountSettingsDescription.length, '未指定交易账户')

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
