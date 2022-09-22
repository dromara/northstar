<template>
  <el-dialog
    id="gatewayForm"
    :title="isUpdateMode ? '修改' : '新增'"
    :visible="visible"
    width="768px"
    :close-on-click-modal="false"
    :show-close="false"
  >
    <GatewaySettingsForm
      v-if="form.gatewayType !== 'PLAYBACK'"
      :visible.sync="gatewaySettingsFormVisible"
      :gatewayType="form.gatewayType"
      :gatewaySettingsMetaInfo="gatewaySettingsMetaInfo"
      :gatewaySettingsObject="form.settings"
      @onSave="(settings) => (form.settings = settings)"
    />
    <PlaybackForm
      v-else
      :visible.sync="gatewaySettingsFormVisible"
      :subscribedContractGroups="subscribedContractGroups"
      :playbackSettingsSrc="form.settings"
      @onSave="(settings) => (form.settings = settings)"
    />
    <el-form ref="gatewayForm" :model="form" label-width="100px" width="200px" :rules="formRules">
      <el-row>
        <el-col :span="8">
          <el-form-item :label="`${typeLabel}ID`" prop="gatewayId">
            <el-input
              v-model="form.gatewayId"
              autocomplete="off"
              :disabled="
                isUpdateMode || (gatewayUsage === 'MARKET_DATA' && form.gatewayType !== 'PLAYBACK')
              "
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
              v-model="form.gatewayType"
              placeholder="未知"
              @change="onChooseGatewayType"
              :disabled="isUpdateMode"
            >
              <el-option
                v-for="(item, i) in gatewayTypeOptions"
                :label="item"
                :value="item"
                :key="i"
              ></el-option>
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
                :id="`bindedGatewayOption_${item.gatewayId}`"
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
            <el-select
              v-model="subscribedContractGroups"
              multiple
              filterable
              collapse-tags
              placeholder="请选择合约"
            >
              <el-option
                v-for="item in contractDefOptions"
                :key="item.name"
                :label="item.label"
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
            v-if="subscribedContractGroups && subscribedContractGroups.length"
          >
            <el-tag v-for="(item, i) in subscribedContractGroups" :key="i">
              {{ item.name + { FUTURES: '期货', OPTION: '期权' }[item.productClass] }}
            </el-tag>
          </div>
          <el-tag type="info" v-else>没有订阅合约</el-tag>
        </el-form-item>
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button
        type="primary"
        @click="gatewaySettingsFormVisible = true"
        :disabled="!form.gatewayType || form.gatewayType === 'SIM'"
        >{{ typeLabel }}配置</el-button
      >
      <el-button id="saveGatewaySettings" type="primary" @click="saveGateway">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import GatewaySettingsForm from '@/components/GatewaySettingsForm'
import PlaybackForm from '@/components/PlaybackForm'
import gatewayMgmtApi from '@/api/gatewayMgmtApi'
import contractApi from '@/api/contractApi'

const GATEWAY_ADAPTER = {
  CTP: 'xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter',
  CTP_SIM: 'xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayAdapter',
  SIM: 'tech.xuanwu.northstar.gateway.sim.SimGatewayLocalImpl',
  PLAYBACK: 'tech.quantit.northstar.gateway.playback.PlaybackGatewayAdapter'
}
const CONNECTION_STATE = {
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  DISCONNECTING: 'DISCONNECTING',
  DISCONNECTED: 'DISCONNECTED'
}
export default {
  components: {
    GatewaySettingsForm,
    PlaybackForm
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
      gatewaySettingsFormVisible: false,
      form: {
        gatewayId: '',
        description: '',
        gatewayType: '',
        gatewayUsage: '',
        gatewayAdapterType: '',
        connectionState: CONNECTION_STATE.DISCONNECTED,
        autoConnect: true,
        bindedMktGatewayId: '',
        subscribedContractGroups: [],
        settings: null
      },
      subscribedContractGroups: [],
      gatewayTypeOptions: [],
      contractDefOptions: [],
      contractType: '',
      gatewaySettingsMetaInfo: []
    }
  },
  computed: {
    typeLabel() {
      return this.gatewayUsage === 'TRADE' ? '账户' : '网关'
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.form = Object.assign({}, this.gatewayDescription)
        this.form.gatewayUsage = this.gatewayUsage
        this.$nextTick(() => {
          gatewayMgmtApi.findAll('MARKET_DATA').then((result) => {
            this.linkedGatewayOptions = result
          })
          gatewayMgmtApi.getGatewayTypeDescriptions().then((result) => {
            this.gatewayTypeOptions = result
              .filter((item) => item.usage.indexOf(this.gatewayUsage) > -1)
              .filter((item) => !item.adminOnly || this.$route.query.superuser)
              .map((item) => item.name)
          })
        })

        setTimeout(() => {
          if (this.form.subscribedContractGroups) {
            this.subscribedContractGroups = this.form.subscribedContractGroups.map((defId) =>
              this.contractDefOptions.find((item) => `${item.name}@${item.productClass}` === defId)
            )
          }
        }, 300)
      }
    },
    'form.gatewayType': function (val) {
      if (
        val &&
        this.gatewayUsage === 'MARKET_DATA' &&
        !this.isUpdateMode &&
        this.subscribedContractGroups
      ) {
        this.subscribedContractGroups = []
      }
      if (val) {
        this.contractDefOptions = []

        if (val !== 'SIM') {
          // 获取网关配置元信息
          gatewayMgmtApi.getGatewaySettingsMetaInfo(val).then((result) => {
            this.gatewaySettingsMetaInfo = result
            this.gatewaySettingsMetaInfo.sort((a, b) => (a.order < b.order ? -1 : 1))
          })
        }

        // 获取合约品种列表
        const type = { FUTURES: '期货', OPTION: '期权' }
        contractApi.getContractProviders(val.replace('_SIM', '')).then((result) => {
          const promiseList = result.map((pvd) => contractApi.getContractDefs(pvd))
          Promise.all(promiseList).then((results) => {
            results.forEach((res) => {
              this.contractDefOptions = this.contractDefOptions.concat(res)
              this.contractDefOptions = this.contractDefOptions.map((item) => {
                item.value = item.name + '@' + item.productClass
                item.label = item.name + type[item.productClass]
                return item
              })
            })
          })
        })
      }
    }
  },
  methods: {
    onChooseGatewayType() {
      this.form.gatewayAdapterType = GATEWAY_ADAPTER[this.form.gatewayType]
      if (this.gatewayUsage === 'MARKET_DATA' && this.form.gatewayType !== 'PLAYBACK') {
        this.form.gatewayId = `${this.form.gatewayType}`
      }
    },
    async saveGateway() {
      if (this.form.gatewayType === 'SIM') {
        this.form.settings = { nothing: 0 }
      }
      if (!this.form.settings || !Object.keys(this.form.settings).length) {
        throw new Error('网关配置不能为空')
      }
      this.$refs.gatewayForm
        .validate()
        .then(() => {
          if (this.gatewayUsage === 'MARKET_DATA') {
            this.form.subscribedContractGroups = this.subscribedContractGroups.map(
              (item) => item.value
            )
          }
          this.$emit('onSave', this.form)
          this.close()
        })
        .catch((e) => {
          console.error(e)
        })
    },
    close() {
      this.$emit('update:visible', false)
      this.subscribedContractGroups = []
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
