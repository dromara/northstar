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
      v-if="form.channelType !== 'PLAYBACK'"
      :visible.sync="gatewaySettingsFormVisible"
      :channelType="form.channelType"
      :gatewaySettingsMetaInfo="gatewaySettingsMetaInfo"
      :gatewaySettingsObject="form.settings"
      @onSave="(settings) => (form.settings = settings)"
    />
    <PlaybackForm
      v-else
      :visible.sync="gatewaySettingsFormVisible"
      :subscribedContracts="subscribedContracts"
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
                isUpdateMode || (gatewayUsage === 'MARKET_DATA' && channelType && !channelType.allowDuplication)
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
          <el-form-item :label="`${typeLabel}类型`" prop="channelType">
            <el-select
              v-model="channelType"
              placeholder="请选择"
              @change="onChooseGatewayType"
              :disabled="isUpdateMode"
            >
              <el-option
                v-for="(item, i) in channelTypeOptions"
                :label="item.name"
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
        <el-col :span="16" v-if="gatewayUsage === 'MARKET_DATA' && !isUpdateMode">
          <el-form-item label="订阅合约">
            <el-select
              v-model="subscribedContracts"
              multiple
              filterable
              remote
              :remote-method="searchContracts"
              collapse-tags
              reserve-keyword
              placeholder="合约可搜索，空格搜索全部"
              :loading="loading"
            >
              <el-option
                v-for="(item,i) in contractOptions"
                :key="i"
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
            v-if="subscribedContracts && subscribedContracts.length"
          >
            <el-tag v-for="(item, i) in subscribedContracts" :key="i">
              {{ item.name }}
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
        :disabled="!form.channelType || form.channelType === 'SIM'"
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
      loading: false,
      linkedGatewayOptions: [],
      formRules: {
        gatewayId: [{ required: true, message: '不能为空', trigger: 'blur', validator: (r,v,cb) => !this.form.gatewayId  ? cb(new Error(r.message)) : cb()}],
        channelType: [{ required: true, message: '不能为空', trigger: 'blur',  validator: (r,v,cb) => !this.form.channelType  ? cb(new Error(r.message)) : cb()}],
        gatewayUsage: [{ required: true, message: '不能为空', trigger: 'blur' }],
        bindedMktGatewayId: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      gatewaySettingsFormVisible: false,
      form: {
        gatewayId: '',
        description: '',
        channelType: '',
        gatewayUsage: '',
        gatewayAdapterType: '',
        connectionState: CONNECTION_STATE.DISCONNECTED,
        autoConnect: true,
        bindedMktGatewayId: '',
        subscribedContracts: [],
        settings: null
      },
      subscribedContracts: [],
      channelTypeOptions: [],
      contractOptions: [],
      channelType: '',
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
        this.subscribedContracts = this.form.subscribedContracts
        this.$nextTick(() => {
          gatewayMgmtApi.findAll('MARKET_DATA').then((result) => {
            this.linkedGatewayOptions = result
          })
          gatewayMgmtApi.getGatewayTypeDescriptions().then((result) => {
            this.channelTypeOptions = result
              .filter((item) => item.usage.indexOf(this.gatewayUsage) > -1)
              .filter((item) => !item.adminOnly || this.$route.query.superuser)
              .map((item) => Object.assign({value: item.name}, item))
            this.channelType = this.channelTypeOptions.find(item => item.name === this.form.channelType)
          })
        })
      }
    },
    'channelType': async function (val) {
      if (
        val &&
        this.gatewayUsage === 'MARKET_DATA' &&
        !this.isUpdateMode &&
        this.subscribedContracts
      ) {
        this.subscribedContracts = []
      }
      if (val) {
        this.form.channelType = val.name

        if (val.name !== 'SIM') {
          // 获取网关配置元信息
          gatewayMgmtApi.getGatewaySettingsMetaInfo(this.form.channelType).then((result) => {
            this.gatewaySettingsMetaInfo = result
            this.gatewaySettingsMetaInfo.sort((a, b) => (a.order < b.order ? -1 : 1))
          })
        }
      }
    }
  },
  methods: {
    onChooseGatewayType() {
      this.form.gatewayAdapterType = GATEWAY_ADAPTER[this.form.channelType]
      if (this.gatewayUsage === 'MARKET_DATA' && !this.channelType.allowDuplication) {
        this.form.gatewayId = `${this.channelType.name}`
      } else if (this.channelType.allowDuplication) {
        this.form.gatewayId = ''
      }
    },
    async saveGateway() {
      if (this.form.channelType === 'SIM') {
        this.form.settings = { nothing: 0 }
      }
      if (!this.form.settings || !Object.keys(this.form.settings).length) {
        throw new Error('网关配置不能为空')
      }
      this.form.channelType = this.channelType.name
      this.$refs.gatewayForm
        .validate()
        .then(() => {
          if (this.gatewayUsage === 'MARKET_DATA') {
            this.form.subscribedContracts = this.subscribedContracts
          }
          this.$emit('onSave', this.form)
          this.close()
        })
        .catch((e) => {
          console.error(e)
        })
    },
    searchContracts(query){
      if (query !== '') {
          this.loading = true;
            // 获取合约品种列表
          contractApi.getGatewayContracts(this.form.channelType === 'PLAYBACK' ? 'CTP' : this.form.channelType, query).then(result => {
            this.contractOptions = result
          }).finally(() => {
            this.loading = false;
          })
        } else {
          this.contractOptions = [];
        }
    },
    close() {
      this.$emit('update:visible', false)
      this.subscribedContracts = []
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
