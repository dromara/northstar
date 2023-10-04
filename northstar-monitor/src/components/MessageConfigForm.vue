<template>
  <el-dialog
    title="消息通知设置"
    :visible="visible"
    :close-on-click-modal="false"
    :show-close="false"
    width="200px"
  >
    <el-form label-width="100px" >
      <el-form-item label="订阅事件列表" >
        <el-select
          v-model="subEvents"
          multiple
          placeholder="可多选"
        >
          <el-option value="TRADE" key="1">成交事件</el-option>
          <el-option value="ORDER" key="2">订单事件</el-option>
          <el-option value="NOTICE" key="3">消息事件</el-option>
          <el-option value="LOGGED_IN" key="4">连线事件</el-option>
          <el-option value="LOGGED_OUT" key="5">离线事件</el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <el-form :model="settings" label-width="100px" width="160px">
      <el-form-item v-for="(item, i) in Object.values(this.settings)" :label="item.label" :key="i" :required="item.required">
        <el-input
          v-if="['TEXT', 'PASSWORD', 'NUMBER'].indexOf(item.type) > -1"
          v-model="settings[item.name].value"
          :type="item.type === 'NUMBER' ? 'number' : 'text'"
          :show-password="item.type === 'PASSWORD'"
          :placeholder="item.placeholder"
          autocomplete="off"
        ></el-input>
        <el-date-picker
          v-if="item.type === 'DATE'"
          v-model="settings[item.name].value"
          type="date"
          placeholder="选择日期"
        >
        </el-date-picker>
        <el-select
          v-if="['SELECT', 'MULTI_SELECT'].indexOf(item.type) > -1"
          v-model="settings[item.name].value"
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
      <el-button type="primary" @click="test">消息测试</el-button>
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="saveConfig">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import alertingApi from '@/api/alertingApi'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      subEvents: [],
      settings:{}
    }
  },
  created() {
    alertingApi.subEvents().then(events => {
      this.subEvents = events
    })
    alertingApi.getSettings().then(result => {
      this.settings = result
    })
  },
  watch: {
    'subEvents.length': function(){
      this.saveSubEvents()
    }
  },
  methods: {
    saveSubEvents(){
      alertingApi.saveEvents(this.subEvents)
    },
    saveConfig() {
      alertingApi.saveSettings(this.settings)
      this.close()
    },
    close() {
      this.$emit('update:visible', false)
    },
    test(){
      alertingApi.testSettings(this.settings).then(() => {
        this.$message.success('测试消息已发送')
      })
    }
  }
}
</script>

<style></style>
