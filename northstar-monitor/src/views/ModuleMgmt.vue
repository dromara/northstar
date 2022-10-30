<template>
  <div class="ns-page">
    <ModuleForm
      :visible.sync="moduleFormVisible"
      :readOnly="curTableIndex > -1 && curModule.runtime && curModule.runtime.enabled"
      :module="curModule"
      @onSave="saveModule"
    />
    <ModuleRuntime
      :visible.sync="ModuleRuntimeVisible"
      :module="curTableIndex > -1 ? curModule : ''"
      :moduleRuntimeSrc="curTableIndex > -1 ? curModule.runtime : ''"
    />
    <el-table height="100%" :data="moduleList">
      <el-table-column label="模组名称" prop="moduleName" sortable align="center" width="180px" />
      <el-table-column label="模组类型" prop="type" sortable align="center" width="100px">
        <template slot-scope="scope">
          {{ { SPECULATION: '投机', ARBITRAGE: '套利' }[scope.row.type] }}
        </template>
      </el-table-column>
      <el-table-column label="模组用途" prop="usage" sortable align="center" width="100px">
        <template slot-scope="scope">
          {{ { PLAYBACK: '回测', UAT: '模拟盘', PROD: '实盘' }[scope.row.usage] }}
        </template>
      </el-table-column>
      <el-table-column label="模组周期" prop="barInterval" sortable align="center" width="100px">
        <template slot-scope="scope">
          {{ `${scope.row.numOfMinPerBar} 分钟` }}
        </template>
      </el-table-column>
      <el-table-column label="交易策略" align="center" sortable width="200px">
        <template slot-scope="scope">
          {{ scope.row.strategySetting.componentMeta.name }}
        </template>
      </el-table-column>
      <el-table-column label="平仓优化" align="center" sortable width="100px">
        <template slot-scope="scope">
          {{
            { FIFO: '先开先平', PRIOR_TODAY: '平今优先', PRIOR_BEFORE_HEGDE_TODAY: '平昨锁今' }[
              scope.row.closingPolicy
            ]
          }}
        </template>
      </el-table-column>
      <el-table-column label="绑定账户" align="center" sortable width="minmax(120px,300px)">
        <template slot-scope="scope">
          {{
            (() => {
              return scope.row.moduleAccountSettingsDescription
                .map((item) => item.accountGatewayId)
                .join('；')
            })()
          }}
        </template>
      </el-table-column>
      <el-table-column label="绑定合约" align="center" width="minmax(120px, 400px)">
        <template slot-scope="scope">
          <span class="text-selectable">
            {{
              (() => {
                return scope.row.moduleAccountSettingsDescription
                  .map((item) => item.bindedUnifiedSymbols.join('，'))
                  .join('；')
              })()
            }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="当前状态" sortable align="center" width="100px">
        <template slot-scope="scope">
          <span
            :class="
              !scope.row.runtime ? '' : scope.row.runtime.enabled ? 'color-green' : 'color-red'
            "
          >
            {{ !scope.row.runtime ? '加载中' : scope.row.runtime.enabled ? '运行中' : '已停用' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" width="420px">
        <template slot="header">
          <el-button size="mini" type="primary" @click="handleCreate">新建</el-button>
        </template>
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.runtime && scope.row.runtime.enabled"
            type="danger"
            size="mini"
            @click.native="toggle(scope.$index, scope.row)"
            >停用</el-button
          >
          <el-button
            v-if="scope.row.runtime && !scope.row.runtime.enabled"
            type="success"
            size="mini"
            @click.native="toggle(scope.$index, scope.row)"
          >
            启用
          </el-button>
          <el-button
            v-if="scope.row.runtime"
            size="mini"
            @click="handlePerf(scope.$index, scope.row)"
            >运行状态</el-button
          >
          <el-button size="mini" @click="tailModuleLog(scope.row)">日志跟踪</el-button>
          <el-button
            v-if="scope.row.runtime"
            size="mini"
            @click="handleRow(scope.$index, scope.row)"
            >{{ scope.row.runtime.enabled ? '查看' : '修改' }}</el-button
          >
          <el-popconfirm
            v-if="scope.row.runtime && !scope.row.runtime.enabled"
            class="ml-10"
            title="确定移除吗？"
            @confirm="handleDelete(scope.$index, scope.row)"
          >
            <el-button slot="reference" size="mini" type="danger"> 删除 </el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import ModuleForm from '@/components/ModuleForm'
import ModuleRuntime from '@/components/ModuleRuntime'
import { mapGetters } from 'vuex'
import moduleApi from '@/api/moduleApi'

export default {
  components: {
    ModuleForm,
    ModuleRuntime
  },
  data() {
    return {
      moduleFormVisible: false,
      ModuleRuntimeVisible: false,
      curTableIndex: -1,
      curModule: null,
      timer: -1
    }
  },
  computed: {
    ...mapGetters(['moduleList'])
  },
  mounted() {
    if (!this.moduleList.length) {
      this.findAll()
    }
  },
  beforeDestroy() {
    clearTimeout(this.timer)
  },
  methods: {
    handleCreate() {
      this.moduleFormVisible = true
      this.curTableIndex = -1
      this.curModule = null
    },
    handlePerf(index, row) {
      this.curTableIndex = index
      this.curModule = row
      this.ModuleRuntimeVisible = true
    },
    handleRow(index, row) {
      this.curTableIndex = index
      this.curModule = row
      this.moduleFormVisible = true
    },
    async handleDelete(index, row) {
      await moduleApi.removeModule(row.moduleName)
      this.$store.commit(
        'updateList',
        this.moduleList.filter((item, i) => i !== index)
      )
    },
    findAll() {
      moduleApi.getAllModules().then((results) => {
        this.$store.commit('updateList', results)
        this.moduleList.map((item) => {
          moduleApi.getModuleRuntime(item.moduleName).then(
            (rt) => {
              this.moduleList.find((item, index, array) => {
                if (item.moduleName === rt.moduleName) {
                  array[index] = Object.assign({ runtime: rt }, item)
                  this.$store.commit('updateList', [...array])
                }
              })
            },
            (e) => {
              console.warn('请求异常：' + e.message)
              console.log('稍后自动重试')
              clearTimeout(this.timer)
              this.timer = setTimeout(this.findAll, 10000)
            }
          )
          return item
        })
      })
    },
    async saveModule(module) {
      console.log(module)
      const rt = await moduleApi.getModuleRuntime(module.moduleName)
      module.runtime = rt
      if (this.curTableIndex < 0) {
        const index = this.moduleList.findIndex((obj) => obj.moduleName === module.moduleName)
        if (index < 0) {
          this.moduleList.push(module)
        } else {
          this.moduleList[index] = module
        }
      } else {
        this.moduleList[this.curTableIndex] = module
      }
      this.$store.commit('updateList', [...this.moduleList])
    },
    async toggle(index, row) {
      await moduleApi.toggleModuleState(row.moduleName)
      row.runtime.enabled = !row.runtime.enabled
    },
    tailModuleLog(row) {
      this.$parent.handleSelect('9', { module: row.moduleName })
    }
  }
}
</script>

<style></style>
