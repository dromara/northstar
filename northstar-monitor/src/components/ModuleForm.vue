<template>
  <el-dialog
    :title="readOnly ? '查看' : isUpdateMode ? '修改' : '新增'"
    :visible="visible"
    class="module-dialog"
    v-loading="loading"
    :close-on-click-modal="!isUpdateMode"
    element-loading-background="rgba(0, 0, 0, 0.3)"
    width="540px"
    @close="close"
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
          <el-menu-item index="4">
            <i class="el-icon-document-copy"></i>
            <span slot="title">已关联合约</span>
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
                :disabled="readOnly || isUpdateMode"
              ></el-input>
            </el-form-item>
            <el-form-item label="模组类型">
              <el-select v-model="form.type" :disabled="readOnly">
                <el-option label="投机" value="SPECULATION"></el-option>
                <el-option label="套利" value="ARBITRAGE"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="模组用途">
              <el-select v-model="form.usage" :disabled="readOnly">
                <el-option label="回测" value="PLAYBACK"></el-option>
                <el-option label="模拟盘" value="UAT"></el-option>
                <el-option label="实盘" value="PROD"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="平仓优化">
              <el-select v-model="form.closingPolicy" :disabled="readOnly">
                <el-option label="先开先平" value="FIRST_IN_FIRST_OUT"></el-option>
                <el-option label="平今优先" value="FIRST_IN_LAST_OUT"></el-option>
                <el-option
                  label="平昨锁今"
                  value="CLOSE_NONTODAY_HEGDE_TODAY"
                  :disabled="form.usage !== 'PROD'"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="K线周期">
              <el-input-number :disabled="readOnly" v-model="form.numOfMinPerBar" :min="1" />
              <span class="ml-10">分钟</span>
            </el-form-item>
            <el-form-item label="预热数据量">
              <el-input-number
                v-model="form.weeksOfDataForPreparation"
                :min="0"
                :disabled="readOnly || form.usage === 'PLAYBACK'"
              />
              <span class="ml-10">周</span>
            </el-form-item>
            <el-form-item label="缓存数据量">
              <el-input-number :disabled="readOnly" v-model="form.moduleCacheDataSize" :min="100">
              </el-input-number>
            </el-form-item>
          </div>
          <div v-show="activeIndex === '2'">
            <el-form-item>
              <el-checkbox id="showDemoStrategy" v-model="showDemoStrategy"
                >显示示例策略</el-checkbox
              >
            </el-form-item>
            <el-form-item label="绑定策略">
              <el-select
                v-model="form.strategySetting"
                placeholder="请选择"
                :disabled="readOnly || isUpdateMode"
                filterable
              >
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
                  class='bindContractSelector'
                  v-model="form.moduleAccountSettingsDescription[i].bindedContracts"
                  multiple
                  filterable
                >
                  <el-option
                    v-for="(item, i) in bindedContractsOptions[i]"
                    :value="item"
                    :label="item.name"
                    :key="i"
                  />
                </el-select>
              </el-form-item>
            </div>
          </div>
          <div v-show="activeIndex === '4'">
            <el-table
              :data="jointBindedContracts"
              style="width: 100%">
              <el-table-column
                prop="name"
                label="合约名称"
                align="center"
                width="100px"
                >
              </el-table-column>
              <el-table-column
                prop="unifiedSymbol"
                label="合约编码"
                align="center">
                <template slot-scope="scope">
                  <span style="user-select: all;">{{scope.row.unifiedSymbol}}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form>
      </el-main>
    </el-container>

    <div slot="footer" class="dialog-footer">
      <el-popconfirm
        v-if="!readOnly && isUpdateMode"
        class="mr-10"
        title="确定重置吗？"
        @confirm="saveSettingAndClose(true)"
      >
        <el-button id="resetModuleSettings" slot="reference" size="mini" type="warning" title="模组状态将重置为初始状态">
          重置模组
        </el-button>
      </el-popconfirm>
      <el-button @click="close">取 消</el-button>
      <el-button
        id="saveModuleSettings"
        v-if="!readOnly"
        type="primary"
        @click="saveSettingAndClose(false)"
        >保存{{ isUpdateMode ? '' : ` | 关闭` }}</el-button
      >
      <el-button v-if="!readOnly && !isUpdateMode" type="primary" @click="saveSettingAndMore"
        >保存 | 继续</el-button
      >
    </div>
  </el-dialog>
</template>

<script>
import gatewayMgmtApi from '../api/gatewayMgmtApi'
import moduleApi from '../api/moduleApi'
import contractApi from '../api/contractApi'

const initComponent = async (component, arr) => {
  const paramsMap = await moduleApi.componentParams(component)
  arr.push({
    componentMeta: component,
    initParams: Object.values(paramsMap).sort((a, b) => a.order - b.order),
    value: component.name
  })
}

export default {
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
      loading: false,
      showDemoStrategy: false,
      accountOptions: [],
      bindedContractsOptions: [],
      tradeStrategyOptionsSource: [],
      activeIndex: '1',
      choseAccounts: [],
      form: {
        moduleName: '',
        type: 'SPECULATION',
        usage: 'UAT',
        numOfMinPerBar: 1,
        weeksOfDataForPreparation: 0,
        moduleCacheDataSize: 500,
        closingPolicy: 'FIRST_IN_FIRST_OUT',
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
    isUpdateMode() {
      return !!this.module
    },
    tradeStrategyOptions() {
      if (this.showDemoStrategy) {
        return this.tradeStrategyOptionsSource
      }
      return this.tradeStrategyOptionsSource.filter((item) => !/示例/.test(item.componentMeta.name))
    },
    jointBindedContracts(){
      return this.form.moduleAccountSettingsDescription.map(item => item.bindedContracts).reduce((jointList, list) => jointList.concat(list), [])
    }
  },
  watch: {
    visible: async function (val) {
      if (val) {
        Object.assign(this.$data, this.$options.data())
        this.showDemoStrategy = this.isUpdateMode
        await this.initData()
        if (!this.module) {
          return
        }
        this.form = this.module
        this.form.strategySetting.value = this.form.strategySetting.componentMeta.name
        const selectedAcc = this.module.moduleAccountSettingsDescription.map((item) => this.accountOptions.filter(acc => acc.gatewayId === item.accountGatewayId)[0])
        const loadContractsPromise = selectedAcc.map(item => contractApi.getSubscribedContracts(item.gatewayId))
        this.bindedContractsOptions = await Promise.all(loadContractsPromise)
        this.choseAccounts = this.module.moduleAccountSettingsDescription.map((item) => {
          item.value = item.accountGatewayId
          return item
        })
      }
    },
    'form.usage': function (val) {
      if (val === 'PLAYBACK') {
        this.form.weeksOfDataForPreparation = 0
      }
      if (val === 'PLAYBACK' || val === 'UAT') {
        this.form.closingPolicy = 'FIRST_IN_FIRST_OUT'
      }
    }
  },
  methods: {
    async initData() {
      moduleApi.getStrategies().then((strategyMetas) => {
        strategyMetas.forEach(async (i) => initComponent(i, this.tradeStrategyOptionsSource))
        setTimeout(() => {
          this.tradeStrategyOptionsSource = this.tradeStrategyOptionsSource.sort((a, b) =>
            a.value.localeCompare(b.value)
          )
        }, 500)
      })
      const result = await gatewayMgmtApi.findAll('TRADE')
      this.accountOptions = result.map((item) => {
          item.value = item.gatewayId
          return item
        })
    },
    handleSelect(index) {
      this.activeIndex = index
    },
    async accountSelected(val) {
      this.form.moduleAccountSettingsDescription = []
      if (!val.length) return
      this.form.moduleAccountSettingsDescription = val.map((item) => {
        return {
          accountGatewayId: item.gatewayId,
          moduleAccountInitBalance: 0,
          bindedContracts: []
        }
      })
      const loadContractsPromise = val.map(item => contractApi.getSubscribedContracts(item.gatewayId))
      this.bindedContractsOptions = await Promise.all(loadContractsPromise)
    },
    async saveSetting(reset) {
      let pass =
        this.assertTrue(this.form.moduleName, '未指定模组名称') &&
        this.assertTrue(this.form.type, '未指定模组类型') &&
        this.assertTrue(this.form.numOfMinPerBar, '未指定K线周期') &&
        this.assertTrue(this.form.strategySetting.componentMeta.name, '未指定交易策略') &&
        this.assertTrue(this.form.moduleAccountSettingsDescription.length, '未指定交易账户')

      if (!pass) {
        throw new Error('校验不通过')
      }
      this.form.moduleAccountSettingsDescription.forEach((desc) => {
        if (!desc.bindedContracts.length) {
          const errMsg = `账户【${desc.accountGatewayId}】未关联合约`
          throw new Error(errMsg)
        }
      })

      const obj = JSON.parse(JSON.stringify(this.form))
      this.loading = true
      try {
        if (this.isUpdateMode) {
          await moduleApi.updateModule(obj, reset)
        } else {
          await moduleApi.insertModule(obj)
        }
      } finally {
        this.loading = false
      }
      this.$emit('onSave', obj)
    },
    async saveSettingAndClose(reset) {
      this.saveSetting(reset)
        .then(this.close)
        .catch((e) => this.$message.error(e.message))
    },
    saveSettingAndMore() {
      this.saveSetting(false).catch((e) => this.$message.error(e.message))
    },
    close() {
      this.$emit('update:visible', false)
      this.activeIndex = '1'
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
