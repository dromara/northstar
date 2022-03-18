<template>
  <el-dialog
    width="700px"
    title="CTP网关配置"
    :visible.sync="dialogVisible"
    append-to-body
    :close-on-click-modal="false"
    :show-close="false"
    destroy-on-close
  >
    <el-form
      ref="ctpSettings"
      :model="ctpSettings"
      label-width="100px"
      width="200px"
      :rules="formRules"
    >
      <el-row>
        <el-col :span="8">
          <el-form-item label="网关账户" prop="userId">
            <el-input v-model="ctpSettings.userId" autocomplete="off"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="网关密码" prop="password">
            <el-input
              v-model="ctpSettings.password"
              type="password"
              autocomplete="off"
              show-password
            ></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="期货公司" prop="brokerId">
            <el-select v-model="ctpSettings.brokerId" placeholder="请选择">
              <el-option
                v-for="item in brokerOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              >
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="saveCtpSetting">保 存</el-button>
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
    ctpSettingsSrc: {
      type: Object,
      default: () => {}
    },
    gatewayUsage: {
      type: String,
      default: 'TRADE'
    }
  },
  data() {
    return {
      formRules: {
        userId: [{ required: true, message: '不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '不能为空', trigger: 'blur' }],
        brokerId: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      dialogVisible: false,
      ctpSettings: {
        userId: '',
        password: '',
        brokerId: ''
      },
      brokerOptions: [
        { label: '宏源主席', value: '1080' },
        { label: '宏源次席', value: '2070' }
      ]
    }
  },
  watch: {
    visible: function (val) {
      if (val) {
        this.dialogVisible = val
        if (!this.ctpSettingsSrc) {
          return
        }
        Object.assign(this.ctpSettings, this.ctpSettingsSrc)
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
      }
    }
  },
  methods: {
    close() {
      this.dialogVisible = false
      this.ctpSettings = this.$options.data().ctpSettings
    },
    saveCtpSetting() {
      this.$refs.ctpSettings.validate((valid) => {
        if (valid) {
          let obj = {}
          Object.assign(obj, this.ctpSettings)
          this.$emit('onSave', obj)
          this.close()
        }
      })
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
