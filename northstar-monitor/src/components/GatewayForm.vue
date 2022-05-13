<template>
  <el-dialog
    id="gatewayForm"
    :title="isUpdateMode ? '修改' : '新增'"
    :visible="visible"
    width="768px"
    :close-on-click-modal="false"
    :show-close="false"
  >
    <NsCtpForm
      :visible.sync="ctpFormVisible"
      :ctpSettingsSrc="form.settings"
      @onSave="(settings) => (form.settings = settings)"
    />
    <NsSimForm
      :visible.sync="simFormVisible"
      :settingsSrc="form.settings"
      @onSave="(settings) => (form.settings = settings)"
    />
    <el-form ref="gatewayForm" :model="form" label-width="100px" width="200px" :rules="formRules">
      <el-row>
        <el-col :span="8">
          <el-form-item :label="`${typeLabel}ID`" prop="gatewayId">
            <el-input
              v-model="form.gatewayId"
              autocomplete="off"
              :disabled="isUpdateMode || gatewayUsage === 'MARKET_DATA'"
            ></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item :label="`${typeLabel}描述`" prop="description">
            <el-input v-model="form.description" autocomplete="off" class="mxw-340"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item :label="`${typeLabel}类型`" prop="gatewayType">
            <el-select
              id="gatewayTypeOptions"
              v-model="form.gatewayType"
              placeholder="未知"
              @change="onChooseGatewayType"
              :disabled="isUpdateMode"
            >
              <el-option label="CTP" value="CTP"></el-option>
              <el-option v-if="$route.query.superuser" label="CTP_SIM" value="CTP_SIM"></el-option>
              <el-option label="SIM" value="SIM"></el-option>
              <!-- <el-option label="IB网关" value="beijing"></el-option> -->
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="自动连接">
            <el-switch v-model="form.autoConnect"></el-switch>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item :label="`${typeLabel}用途`" prop="gatewayUsage">
            <el-select
              v-model="form.gatewayUsage"
              placeholder="未知"
              @change="onChooseGatewayType"
              disabled
            >
              <el-option
                v-if="gatewayUsage === 'MARKET_DATA'"
                label="行情"
                value="MARKET_DATA"
              ></el-option>
              <el-option v-if="gatewayUsage === 'TRADE'" label="交易" value="TRADE"></el-option>
              <!-- <el-option label="IB网关" value="beijing"></el-option> -->
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8" v-if="gatewayUsage === 'TRADE'">
          <el-form-item label="行情网关" prop="bindedMktGatewayId">
            <el-select
              v-model="form.bindedMktGatewayId"
              placeholder="请选择"
              @change="onChooseGatewayType"
            >
              <el-option
                :label="item.gatewayId"
                :value="item.gatewayId"
                v-for="(item, i) in linkedGatewayOptions"
                :key="i"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="16" v-if="gatewayUsage === 'MARKET_DATA'">
          <el-form-item label="订阅合约">
            <el-select v-model="contractType" placeholder="请选择合约类型" class="mxw-140 mr-10">
              <el-option
                v-for="item in contractTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item"
                :disabled="item.disabled"
              >
              </el-option>
            </el-select>
            <el-select
              v-model="form.subscribedContracts"
              multiple
              collapse-tags
              placeholder="请选择合约"
            >
              <el-option
                v-for="item in contractOptions"
                :key="item.unifiedSymbol"
                :label="item.name"
                :value="item"
              >
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="gatewayUsage === 'MARKET_DATA'">
        <el-form-item label="已订阅合约">
          <div
            class="tag-wrapper"
            v-if="form.subscribedContracts && form.subscribedContracts.length"
          >
            <el-tag v-for="(item, i) in form.subscribedContracts" :key="i">{{ item.name }}</el-tag>
          </div>
          <el-tag type="info" v-else>没有订阅合约</el-tag>
        </el-form-item>
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button
        id="gatewaySettings"
        type="primary"
        @click="gatewaySettingConfig"
        :disabled="!form.gatewayType || (gatewayUsage !== 'TRADE' && form.gatewayType === 'SIM')"
        >{{ typeLabel }}配置</el-button
      >
      <el-button id="saveGatewaySettings" type="primary" @click="saveGateway">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import NsCtpForm from '@/components/CtpForm'
import NsSimForm from '@/components/SimForm'
import gatewayMgmtApi from '../api/gatewayMgmtApi'
import gatewayDataApi from '@/api/gatewayDataApi'

const GATEWAY_ADAPTER = {
  CTP: 'xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter',
  CTP_SIM: 'xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayAdapter',
  SIM: 'tech.xuanwu.northstar.gateway.sim.SimGatewayLocalImpl',
  IB: ''
}
const CONNECTION_STATE = {
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  DISCONNECTING: 'DISCONNECTING',
  DISCONNECTED: 'DISCONNECTED'
}
const filterMethods = {
  INDEX: (item) => item.unifiedsymbol.endsWith('FUTURES') && item.name.endsWith('指数'),
  FUTURES: (item) => item.unifiedsymbol.endsWith('FUTURES') && !item.name.endsWith('指数')
}
export default {
  components: {
    NsCtpForm,
    NsSimForm
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    gatewayDescription: {
      type: Object,
      default: () => {}
    },
    isUpdateMode: {
      type: Boolean,
      default: false
    },
    gatewayUsage: {
      type: String,
      default: 'TRADE'
    }
  },
  data() {
    return {
      linkedGatewayOptions: [],
      formRules: {
        gatewayId: [{ required: true, message: '不能为空', trigger: 'blur' }],
        gatewayType: [{ required: true, message: '不能为空', trigger: 'blur' }],
        gatewayUsage: [{ required: true, message: '不能为空', trigger: 'blur' }],
        bindedMktGatewayId: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      ctpFormVisible: false,
      simFormVisible: false,
      contractFormVisible: false,
      form: {
        gatewayId: '',
        description: '',
        gatewayType: '',
        gatewayUsage: '',
        gatewayAdapterType: '',
        connectionState: CONNECTION_STATE.DISCONNECTED,
        autoConnect: true,
        bindedMktGatewayId: '',
        subscribedContracts: [],
        settings: null
      },
      contractTypeOptions: [
        { value: 'INDEX', label: '指数合约' },
        { value: 'FUTURES', label: '期货合约' },
        { value: 'OPTION', label: '期权合约', disabled: true },
        { value: 'OTHERS', label: '其他合约', disabled: true }
      ],
      contractOptions: [],
      contractType: '',
      cacheContracts: {}
    }
  },
  computed: {
    typeLabel() {
      return this.gatewayUsage === 'TRADE' ? '账户' : '网关'
    }
  },
  watch: {
    visible: async function (val) {
      if (val) {
        this.form = Object.assign({}, this.gatewayDescription)
        this.form.gatewayUsage = this.gatewayUsage
        if (this.form.subscribedContracts) {
          this.form.subscribedContracts = this.form.subscribedContracts.map((item) =>
            Object.assign({ value: item.name }, item)
          )
        }
        this.linkedGatewayOptions = await gatewayMgmtApi.findAll('MARKET_DATA')
      }
    },
    contractType: function (val) {
      this.contractOptions = this.cacheContracts[this.form.gatewayType] || []
      this.contractOptions = this.contractOptions.filter(filterMethods[val.value]).map((item) => ({
        unifiedSymbol: item.unifiedsymbol,
        symbol: item.symbol,
        name: item.name,
        type: val.value,
        gatewayId: item.gatewayid,
        value: item.name
      }))
    }
  },
  created() {
    ;['CTP', 'SIM'].forEach(async (type) => {
      this.cacheContracts[type] = await gatewayDataApi.getContracts(type)
    })
  },
  methods: {
    onChooseGatewayType() {
      this.form.gatewayAdapterType = GATEWAY_ADAPTER[this.form.gatewayType]
      if (this.gatewayUsage === 'MARKET_DATA') {
        this.form.gatewayId = `${this.form.gatewayType}`
      }
    },
    gatewaySettingConfig() {
      if (this.form.gatewayType === 'CTP' || this.form.gatewayType === 'CTP_SIM') {
        this.ctpFormVisible = true
      }
      if (this.form.gatewayType === 'SIM') {
        this.simFormVisible = true
      }
    },
    async saveGateway() {
      if (this.gatewayUsage !== 'TRADE' && this.form.gatewayType === 'SIM') {
        this.form.settings = { nothing: 0 }
      }
      if (!this.form.settings || !Object.keys(this.form.settings).length) {
        throw new Error('网关配置不能为空')
      }
      this.$refs.gatewayForm
        .validate()
        .then(() => {
          let obj = {}
          Object.assign(obj, this.form)
          this.$emit('onSave', obj)
          this.close()
        })
        .catch((e) => {
          console.error(e)
        })
    },
    close() {
      this.$emit('update:visible', false)
      this.form = this.$options.data().form
    }
  }
}
</script>

<style>
.el-dialog__body {
  padding: 10px 20px 0px;
}
.tag-wrapper {
  overflow: auto;
  max-height: 200px;
}
.tag-wrapper .el-tag {
  margin-right: 10px;
}
</style>
