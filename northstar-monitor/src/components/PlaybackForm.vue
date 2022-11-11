<template>
  <el-dialog
    width="520px"
    title="历史回放网关配置"
    :visible="visible"
    append-to-body
    :close-on-click-modal="false"
    :show-close="false"
    destroy-on-close
  >
    <el-form
      ref="playbackSettings"
      :model="playbackSettings"
      label-width="100px"
      width="200px"
      :rules="formRules"
    >
      <el-form-item label="预热起始日">
        <el-date-picker
          style="width: 193px"
          v-model="preStartDate"
          type="date"
          placeholder="请选择"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item label="回放日期" prop="dateRange">
        <el-date-picker
          v-model="playbackSettings.dateRange"
          type="daterange"
          align="left"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :picker-options="pickerOptions"
        >
        </el-date-picker>
      </el-form-item>
      <el-form-item label="回放精度" prop="precision">
        <el-select v-model="playbackSettings.precision">
          <el-option label="极低（每分钟1个TICK）" value="EXTREME" key="0"></el-option>
          <el-option label="低（每分钟4个TICK）" value="LOW" key="1"></el-option>
          <el-option label="中（每分钟30个TICK）" value="MEDIUM" key="2"></el-option>
          <el-option label="高（每分钟120个TICK）" value="HIGH" key="3"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="回放速度" prop="speed">
        <el-select v-model="playbackSettings.speed">
          <el-option label="正常" value="NORMAL" key="1"></el-option>
          <el-option label="快速" value="SPRINT" key="2"></el-option>
          <el-option label="超速" value="RUSH" key="3"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="回放合约" prop="unifiedSymbols">
        <el-select
          v-model="playbackSettings.unifiedSymbols"
          multiple
          :disabled="playbackSettings.unifiedSymbols.length >= 10"
        >
          <el-option
            v-for="(item, i) in contractOptions"
            :label="item.name"
            :value="item.unifiedsymbol"
            :key="i"
          ></el-option>
        </el-select>
        <div v-if="playbackSettings.unifiedSymbols.length >= 10" class="warning-text">
          <i class="el-icon-warning" /> 最多只能同时选择十个合约<br />
        </div>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="savePlaybackSetting">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import contractApi from '@/api/contractApi'
import { ContractField } from '@/lib/xyz/redtorch/pb/core_field_pb'

import moment from 'moment'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    playbackSettingsSrc: {
      type: Object,
      default: () => {}
    },
    subscribedContractGroups: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      formRules: {
        dateRange: [{ required: true, message: '不能为空', trigger: 'blur' }],
        precision: [{ required: true, message: '不能为空', trigger: 'blur' }],
        speed: [{ required: true, message: '不能为空', trigger: 'blur' }],
        unifiedSymbols: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      pickerOptions: {
        shortcuts: [
          {
            text: '最近一周',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近三个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              picker.$emit('pick', [start, end])
            }
          }
        ]
      },
      preStartDate: '',
      contractOptions: [],
      playbackSettings: {
        preStartDate: '',
        dateRange: '',
        startDate: '',
        endDate: '',
        precision: '',
        speed: '',
        unifiedSymbols: []
      },
      isUpdateMode: false
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        if (this.playbackSettingsSrc) {
          this.isUpdateMode = Object.keys(this.playbackSettingsSrc).length > 0
        }
        this.contractOptions = []
        this.subscribedContractGroups.forEach((item) => {
          contractApi.getSubscribableContractList(item.value).then((result) => {
            const contracts = result
              .map((item) => ContractField.deserializeBinary(item).toObject())
              .sort((a, b) => a['name'].localeCompare(b['name']))
            this.contractOptions = this.contractOptions.concat(contracts)
          })
        })
        if (!this.playbackSettingsSrc) {
          return
        }
        Object.assign(this.playbackSettings, this.playbackSettingsSrc)
        this.playbackSettings.dateRange = [
          moment(this.playbackSettingsSrc.startDate, 'YYYYMMDD').toDate(),
          moment(this.playbackSettingsSrc.endDate, 'YYYYMMDD').toDate()
        ]
        this.preStartDate = moment(this.playbackSettingsSrc.preStartDate, 'YYYYMMDD').toDate()
      }
    },
    'playbackSettings.unifiedSymbols': function (val) {
      if (val.length >= 10) {
        val.length = 10
      }
    }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
      this.playbackSettings = this.$options.data().playbackSettings
    },
    savePlaybackSetting() {
      this.$refs.playbackSettings.validate((valid) => {
        if (valid) {
          const [start, end] = this.playbackSettings.dateRange
          this.playbackSettings.startDate = moment(start).format('yyyyMMDD')
          this.playbackSettings.endDate = moment(end).format('yyyyMMDD')
          this.playbackSettings.preStartDate = moment(this.preStartDate || start).format('yyyyMMDD')
          let obj = {}
          Object.assign(obj, this.playbackSettings)
          this.$emit('onSave', obj)
          this.close()
        }
      })
    }
  }
}
</script>

<style>
.el-range-input {
  background-color: inherit;
}
.el-date-table td.in-range div,
.el-date-table td.in-range div:hover,
.el-date-table.is-week-mode .el-date-table__row.current div,
.el-date-table.is-week-mode .el-date-table__row:hover div {
  background-color: rgba(20, 20, 20, 0.4);
}
</style>
