<template>
  <el-dialog
    width="500px"
    title="SIM模拟网关配置"
    :visible="visible"
    append-to-body
    :close-on-click-modal="false"
    :show-close="false"
    @close="onClose"
  >
    <el-form
      ref="settingForm"
      :model="settings"
      label-width="330px"
      width="200px"
      :rules="formRules"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="模拟交易每笔手续费（单位：元）：" :required="true" prop="fee">
            <el-input v-model="settings.fee" type="number" autocomplete="off"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="() => $emit('update:visible', false)">取 消</el-button>
      <el-button type="primary" @click="saveSetting">保 存</el-button>
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
    settingsSrc: {
      type: Object,
      default: () => {}
    },
    isCreate: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      formRules: {
        fee: [{ required: true, message: '不能为空', trigger: 'blur' }]
      },
      settings: {
        fee: ''
      }
    }
  },
  watch: {
    settingsSrc: function (val) {
      console.log(val)
      Object.assign(this.settings, val)
    }
  },
  mounted() {
    this.settings = this.settingsSrc || {}
  },
  methods: {
    onClose() {
      this.$emit('update:visible', false)
    },
    saveSetting() {
      this.$refs.settingForm.validate((valid) => {
        if (valid) {
          let obj = {}
          Object.assign(obj, this.settings)
          this.$emit('onSave', obj)
          this.$emit('update:visible', false)
          this.$refs.settingForm.resetFields()
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
.ns-row-wrapper {
  margin-bottom: 18px;
}
</style>
