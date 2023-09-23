<template>
  <el-dialog
    title="邮件通知设置"
    :visible="visible"
    :close-on-click-modal="false"
    :show-close="false"
    width="200px"
  >
    <el-form label-width="120px">
      <el-form-item label="禁用邮件通知">
        <el-checkbox v-model="form.disabled"></el-checkbox>
      </el-form-item>
    </el-form>
    <el-form
      ref="mailSettings"
      :model="form"
      :disabled="form.disabled"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="SMTP地址" prop="emailSMTPHost">
        <el-input
          v-model="form.emailSMTPHost"
          :placeholder="form.disabled ? '' : '例如smtp.163.com'"
        />
      </el-form-item>
      <el-form-item label="Email邮箱地址" prop="emailUsername">
        <el-input
          v-model="form.emailUsername"
          autocomplete="off"
          :placeholder="form.disabled ? '' : '发送人的邮箱'"
        ></el-input>
      </el-form-item>
      <el-form-item label="邮箱授权码" prop="emailPassword">
        <el-input
          v-model="form.emailPassword"
          autocomplete="off"
          :placeholder="form.disabled ? '' : '可在邮箱设置中找到'"
        ></el-input>
      </el-form-item>
      <el-form-item label="订阅邮箱列表" prop="subscriberList">
        <el-input
          type="textarea"
          :rows="3"
          :placeholder="form.disabled ? '' : '接收人的邮箱，如有多个用分号隔开'"
          v-model="subscriberListSrc"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="订阅事件列表" prop="interestTopicList">
        <el-select
          v-model="form.interestTopicList"
          multiple
          :placeholder="form.disabled ? '' : '可多选'"
        >
          <el-option value="TRADE" key="1">成交事件</el-option>
          <el-option value="ORDER" key="2">订单事件</el-option>
          <el-option value="NOTICE" key="3">消息事件</el-option>
          <el-option value="LOGGED_IN" key="4">连线事件</el-option>
          <el-option value="LOGGED_OUT" key="5">离线事件</el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="test">邮件测试</el-button>
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="saveMailConfig">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import mailConfigApi from '@/api/mailConfigApi'

export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      subscriberListSrc: '',
      form: {
        disabled: true,
        emailSMTPHost: '',
        emailUsername: '',
        emailPassword: '',
        subscriberList: [],
        interestTopicList: []
      },
      rules: {
        emailSMTPHost: [{ required: true, message: '不能为空', trigger: 'blur' }],
        emailUsername: [{ required: true, message: '不能为空', trigger: 'blur' }],
        emailPassword: [{ required: true, message: '不能为空', trigger: 'blur' }],
        subscriberList: [{ required: true, message: '不能为空', trigger: 'blur' }],
        interestTopicList: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      testable: false
    }
  },
  watch: {
    subscriberListSrc: function (val) {
      if (val) {
        this.form.subscriberList = val.split(/;|；/).map((mail) => mail.trim())
      }
    },
    visible: function(val){
      if(val){
        mailConfigApi.testable().then(result => this.testable = result)
      }
    }
  },
  created() {
    mailConfigApi.getConfig().then((result) => {
      this.form = result
      if (result.subscriberList) {
        this.subscriberListSrc = result.subscriberList.join(';\n')
      }
    })
  },
  methods: {
    saveMailConfig() {
      this.$refs.mailSettings.validate((valid) => {
        if (valid) {
          mailConfigApi.saveConfig(this.form)
          this.close()
        }
      })
    },
    close() {
      this.$emit('update:visible', false)
    },
    test(){
      this.$refs.mailSettings.validate(async (valid) => {
        if (valid) {
          await mailConfigApi.saveConfig(this.form)
          await mailConfigApi.test()
        }
      })
    }
  }
}
</script>

<style></style>
