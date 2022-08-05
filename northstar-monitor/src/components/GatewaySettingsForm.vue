<template>
  <el-dialog
    :title="`${gatewayType}网关配置`"
    width="300px"
    :visible="visible"
    append-to-body
    :close-on-click-modal="false"
    :show-close="false"
    destroy-on-close
  >
    <el-form :model="gatewaySettings" label-width="100px" width="200px">
      <el-form-item v-for="(item, i) in gatewaySettingsMetaInfo" :label="item.label" :key="i">
        <el-input
          v-if="['TEXT', 'PASSWORD', 'NUMBER'].indexOf(item.type) > -1"
          v-model="gatewaySettings[item.name]"
          :type="item.type === 'NUMBER' ? 'number' : 'text'"
          :show-password="item.type === 'PASSWORD'"
          :placeholder="item.placeholder"
          autocomplete="off"
        ></el-input>
        <el-date-picker
          v-if="item.type === 'DATE'"
          v-model="gatewaySettings[item.name]"
          type="date"
          placeholder="选择日期"
        >
        </el-date-picker>
        <el-select
          v-if="['SELECT', 'MULTI_SELECT'].indexOf(item.type) > -1"
          v-model="gatewaySettings[item.name]"
          :multiple="item.type === 'MULTI_SELECT'"
          collapse-tags
        >
          <el-option
            v-for="(val, i) in item.optionsVal"
            :label="item.options[i]"
            :value="val"
            :key="i"
          ></el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="saveSettings">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    gatewayType: {
      type: String,
      default: ''
    },
    gatewaySettingsMetaInfo: {
      type: Array,
      default: () => []
    },
    gatewaySettingsObject: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      gatewaySettings: {}
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        if (!this.gatewaySettingsObject) {
          return
        }
        this.gatewaySettings = Object.assign({}, this.gatewaySettingsObject)
      }
    }
  },
  methods: {
    close() {
      this.$emit('update:visible', false)
      this.gatewaySettings = {}
    },
    saveSettings() {
      this.gatewaySettingsMetaInfo.forEach((item) => {
        if (!this.gatewaySettings[item.name]) {
          throw new Error(`【${item.label}】不能为空`)
        }
      })
      let obj = {}
      Object.assign(obj, this.gatewaySettings)
      this.$emit('onSave', obj)
      this.close()
    }
  }
}
</script>

<style>
.el-dialog__body {
  padding: 10px 20px 0px;
}
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

input[type='number'] {
  -moz-appearance: textfield;
}
</style>
