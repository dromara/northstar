<template>
  <el-dialog
    :title="readOnly ? '查看' : this.module ? '修改' : '新增'"
    :visible.sync="dialogVisible"
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
      <el-aside width="150px">
        <el-menu :default-active="activeIndex" @select="handleSelect">
          <el-menu-item index="1">
            <i class="el-icon-setting"></i>
            <span slot="title">基础信息</span>
          </el-menu-item>
          <el-menu-item index="2">
            <i class="el-icon-s-opportunity"></i>
            <span slot="title">信号策略</span>
          </el-menu-item>
          <el-menu-item index="3">
            <i class="el-icon-warning"></i>
            <span slot="title">风控策略</span>
          </el-menu-item>
          <el-menu-item index="4">
            <i class="el-icon-s-custom"></i>
            <span slot="title">交易策略</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-compact"
        ><el-form :model="form" label-width="100px" class="module-form" inline :rules="formRules">
          <el-form-item v-if="activeIndex === '1'" label="模组名称">
            <el-input v-model="form.moduleName" :disabled="readOnly || this.module"></el-input>
          </el-form-item>
          <el-form-item v-if="activeIndex === '1'" label="模组类型">
            <el-select v-model="form.type" :disabled="readOnly">
              <el-option label="CTA" value="CTA"></el-option>
              <el-option label="ARBITRAGE" value="ARBITRAGE"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeIndex === '1'" label="绑定账户">
            <el-select v-model="form.accountGatewayId" :disabled="readOnly">
              <el-option
                v-for="account in accountOptions"
                :label="account"
                :value="account"
                :key="account"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="activeIndex === '1'" label="是否启用">
            <el-switch v-model="form.enabled" :disabled="readOnly"> </el-switch>
          </el-form-item>
          <el-form-item v-if="activeIndex === '2'" label="信号策略">
            <el-select
              v-model="chosenSignalPolicy"
              @change="onChosenSignalPolicy"
              placeholder="请选择"
              key="信号策略"
              :disabled="readOnly"
            >
              <el-option
                v-for="(p, i) in signalPolicyOptions"
                :label="p.componentMeta.name"
                :value="p.componentMeta.name"
                :key="i"
              ></el-option>
            </el-select>
          </el-form-item>
          <div v-if="activeIndex === '2'">
            <div v-for="(policy, i) in signalPolicyOptions" :key="i">
              <div v-if="chosenSignalPolicy === policy.componentMeta.name">
                <el-form-item
                  v-for="(param, index) in policy.initParams"
                  :label="param.label"
                  :key="param.field"
                >
                  <el-select
                    v-if="param.type === 'Options'"
                    v-model="signalPolicyOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :disabled="readOnly"
                  >
                    <el-option v-for="(item, i) in param.options" :value="item" :key="i">{{
                      item
                    }}</el-option>
                  </el-select>
                  <el-input
                    v-else
                    v-model="signalPolicyOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :type="param.type.toLowerCase()"
                    :disabled="readOnly"
                  />
                  <span v-if="param.unit" class="value-unit">{{ param.unit }}</span>
                </el-form-item>
              </div>
            </div>
          </div>

          <el-form-item v-if="activeIndex === '3'" label="风控策略">
            <el-select
              v-model="chosenRiskRules"
              @change="onChosenRiskRule"
              value-key="name"
              placeholder="请选择"
              key="风控策略"
              collapse-tags
              multiple
              clearable
              :disabled="readOnly"
            >
              <el-option
                v-for="(r, i) in riskRuleOptions"
                :label="r.componentMeta.name"
                :value="r.componentMeta.name"
                :key="i"
              ></el-option>
            </el-select>
          </el-form-item>
          <div v-if="activeIndex === '3'">
            <div v-for="(rule, i) in riskRuleOptions" :key="i">
              <div v-if="chosenRiskRules.indexOf(rule.componentMeta.name) !== -1">
                <el-form-item v-for="(param, k) in rule.initParams" :key="k" :label="param.label">
                  <el-select
                    v-if="param.type === 'Options'"
                    v-model="riskRuleOptions[i].initParams[k]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :disabled="readOnly"
                  >
                    <el-option v-for="(item, i) in param.options" :value="item" :key="i">{{
                      item
                    }}</el-option>
                  </el-select>
                  <el-input
                    v-model="riskRuleOptions[i].initParams[k]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :type="param.type.toLowerCase()"
                    :disabled="readOnly"
                  />
                  <span v-if="param.unit" class="value-unit">{{ param.unit }}</span>
                </el-form-item>
              </div>
            </div>
          </div>
          <el-form-item v-if="activeIndex === '4'" label="交易策略">
            <el-select
              v-model="chosenDealer"
              @change="onChosenDealer"
              value-key="name"
              placeholder="请选择"
              key="交易策略"
              :disabled="readOnly"
            >
              <el-option
                v-for="(dealer, i) in dealerOptions"
                :label="dealer.componentMeta.name"
                :value="dealer.componentMeta.name"
                :key="i"
              ></el-option>
            </el-select>
          </el-form-item>
          <div v-if="activeIndex === '4'">
            <div v-for="(dealer, i) in dealerOptions" :key="i">
              <div v-if="dealer.componentMeta.name === chosenDealer">
                <el-form-item
                  v-for="(param, index) in dealer.initParams"
                  :label="param.label"
                  :key="param.field"
                >
                  <el-select
                    v-if="param.type === 'Options'"
                    v-model="dealerOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :disabled="readOnly"
                  >
                    <el-option v-for="(item, i) in param.options" :value="item" :key="i">{{
                      item
                    }}</el-option>
                  </el-select>
                  <el-input
                    v-else
                    v-model="dealerOptions[i].initParams[index]['value']"
                    :class="param.unit ? 'with-unit' : ''"
                    :type="param.type.toLowerCase()"
                    :disabled="readOnly"
                  />
                  <span v-if="param.unit" class="value-unit">{{ param.unit }}</span>
                </el-form-item>
              </div>
            </div>
          </div>
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
      dialogVisible: false,
      contractFinderVisible: false,
      accountOptions: [],
      signalPolicyOptions: [],
      riskRuleOptions: [],
      dealerOptions: [],
      activeIndex: '1',
      chosenSignalPolicy: '',
      chosenRiskRules: [],
      chosenDealer: '',
      form: {
        moduleName: '',
        accountGatewayId: '',
        signalPolicy: {
          componentMeta: {},
          initParams: []
        },
        riskControlRules: [],
        dealer: {
          componentMeta: {},
          initParams: []
        },
        enabled: false,
        type: ''
      }
    }
  },
  mounted() {
    this.initData()
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
        if (!this.module) {
          return
        }
        this.form = Object.assign({}, this.module)

        this.chosenSignalPolicy = this.module.signalPolicy.componentMeta.name
        this.signalPolicyOptions.forEach((p) => {
          if (p.componentMeta.name === this.chosenSignalPolicy) {
            p.initParams = this.module.signalPolicy.initParams
          }
        })

        this.chosenDealer = this.module.dealer.componentMeta.name
        this.dealerOptions.forEach((d) => {
          if (d.componentMeta.name === this.chosenDealer) {
            d.initParams = this.module.dealer.initParams
          }
        })

        // 风控策略名 --> 初始化参数列表
        const ruleNameToParamsMap = this.module.riskControlRules.reduce((obj, rule) => {
          obj[rule.componentMeta.name] = rule.initParams
          return obj
        }, {})
        this.riskRuleOptions.forEach((r) => {
          if (ruleNameToParamsMap[r.componentMeta.name]) {
            r.initParams = ruleNameToParamsMap[r.componentMeta.name]
          }
        })
        this.chosenRiskRules = this.module.riskControlRules.map((i) => i.componentMeta.name)
        this.onChosenRiskRule(this.chosenRiskRules)
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
      }
    }
  },
  methods: {
    initData() {
      gatewayMgmtApi
        .findAll('TRADE')
        .then((result) => (this.accountOptions = result.map((i) => i.gatewayId)))
      moduleApi.getCtpSignalPolicies().then((policies) => {
        policies.forEach(async (i) => initComponent(i, this.signalPolicyOptions))
      })
      moduleApi.getDealers().then((dealers) => {
        dealers.forEach(async (i) => initComponent(i, this.dealerOptions))
      })
      moduleApi.getRiskControlRules().then((rules) => {
        rules.forEach(async (i) => initComponent(i, this.riskRuleOptions))
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
      const unsetItems1 = this.form.signalPolicy.initParams.filter((item) => !item.value)
      const unsetItems3 = this.form.dealer.initParams.filter((item) => !item.value)
      const unsetItems2 = []
      this.form.riskControlRules.forEach((rule) => {
        rule.initParams.forEach((item) => {
          if (!item.value) {
            unsetItems3.push(item)
          }
        })
      })
      const groupNames = ['信号策略', '风控策略', '交易策略']
      const unsetItemsTotal = [unsetItems1, unsetItems2, unsetItems3]
      // 二次校验
      for (let i = 0; i < 3; i++) {
        let unsetItems = unsetItemsTotal[i]
        if (!unsetItems.length) {
          continue
        }
        this.assertTrue(unsetItemsTotal[i].value, `${groupNames[i]}未设置${unsetItems[0].label}`)
        return
      }

      const obj = Object.assign({}, this.form)
      this.$emit('onSave', obj)
      this.close()
    },
    close() {
      this.dialogVisible = false
      this.activeIndex = '1'
      Object.assign(this.$data, this.$options.data())
      this.$nextTick(this.initData)
    },
    assertTrue(expression, errMsg) {
      if (!expression) {
        this.$message.error(errMsg)
        return false
      }
      return true
    },
    onChosenRiskRule() {
      // 处理数据增减
      this.form.riskControlRules = this.riskRuleOptions.filter(
        (i) => this.chosenRiskRules.indexOf(i.componentMeta.name) !== -1
      )
    },
    async onChosenDealer() {
      const arr = this.dealerOptions.filter((i) => i.componentMeta.name === this.chosenDealer)
      if (arr.length) {
        this.form.dealer = arr[0]
      }
    },
    async onChosenSignalPolicy() {
      const arr = this.signalPolicyOptions.filter(
        (i) => i.componentMeta.name === this.chosenSignalPolicy
      )
      if (arr.length) {
        this.form.signalPolicy = arr[0]
      }
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
