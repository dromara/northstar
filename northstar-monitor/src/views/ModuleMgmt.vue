<template>
  <div class="ns-page">
    <ModuleForm
      :visible.sync="moduleFormVisible"
      :readOnly="curTableIndex > -1 && curModule.runtime && curModule.runtime.enabled"
      :module="curModule ? JSON.parse(JSON.stringify(curModule)) : null"
      @onSave="saveModule"
    />
    <ModuleRuntime
      :visible.sync="ModuleRuntimeVisible"
      :module="curTableIndex > -1 ? curModule : ''"
      :moduleRuntimeSrc="curTableIndex > -1 ? curModule.runtime : ''"
    />
    <el-table height="100%" :data="moduleList">
      <el-table-column type="index" width="42px" />
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
      <el-table-column label="模组周期" prop="numOfMinPerBar" sortable align="center" width="100px">
        <template slot-scope="scope">
          {{ `${scope.row.numOfMinPerBar} 分钟` }}
        </template>
      </el-table-column>
      <el-table-column
        label="交易策略"
        align="center"
        prop="strategySetting.componentMeta.name"
        sortable
        width="200px"
      >
        <template slot-scope="scope">
          {{ scope.row.strategySetting.componentMeta.name }}
        </template>
      </el-table-column>
      <el-table-column label="平仓优化" prop="closingPolicy" align="center" sortable width="100px">
        <template slot-scope="scope">
          {{
            { FIRST_IN_FIRST_OUT: '先开先平', FIRST_IN_LAST_OUT: '平今优先', CLOSE_NONTODAY_HEGDE_TODAY: '平昨锁今' }[
              scope.row.closingPolicy
            ]
          }}
        </template>
      </el-table-column>
      <el-table-column label="绑定账户" sortable align="center" width="minmax(100px, 200px)" min-width="100px">
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
      <el-table-column label="绑定合约" sortable align="center" width="minmax(100px, auto)" min-width="100px">
        <template slot-scope="scope">
          <span class="text-selectable">
            {{
              (() => {
                return scope.row.moduleAccountSettingsDescription
                  .map((item) => item.bindedContracts.map(item => item.name).join('，'))
                  .join('；')
              })()
            }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="持仓状态" sortable align="center" width="100px">
        <template slot-scope="scope">
          <el-tag size="small">{{
            !scope.row.runtime ? '-' :
                {
                  HOLDING_LONG: '持多单',
                  HOLDING_SHORT: '持空单',
                  EMPTY: '无持仓',
                  EMPTY_HEDGE: '对冲锁仓',
                  HOLDING_HEDGE: '对冲持仓',
                  PENDING_ORDER: '等待成交'
                }[scope.row.runtime.moduleState] || '-'
              }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="当前状态"
        prop="runtime.enabled"
        sortable
        align="center"
        width="100px"
      >
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
      <el-table-column align="center" width="400px">
        <template slot="header">
          <el-button id="createModule" size="mini" type="primary" @click="handleCreate">新建</el-button>
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
      timer: -1,
      delayTimer: -1,
    }
  },
  computed: {
    ...mapGetters(['moduleList'])
  },
  created(){
    if(!this.moduleList.length){
      moduleApi.getAllModules().then(modules => {
        this.$store.commit('updateList', modules.sort((a,b) => a.moduleName.localeCompare(b.moduleName)))
      })
    }
  },
  mounted() {
    this.autoRefreshList()
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
        this.moduleList.filter((item) => item.moduleName !== row.moduleName)
      )
    },
    async autoRefreshList() {
      moduleApi.getAllModules().then(modules => {
        if(modules.length > 0){
          const statusPromises = modules.map(m => moduleApi.getModuleStatus(m.moduleName))
          const statePromises = modules.map(m => moduleApi.getModuleState(m.moduleName))

          Promise.all([...statusPromises, ...statePromises]).then(results => {
            const statuses = results.slice(0, modules.length);
            const states = results.slice(modules.length);

            // 将对应的状态和状态组合在一起
            const combinedResults = modules.map((module, index) => ({
                ...module,
                runtime: statuses[index] !== null ? {
                  moduleState: states[index],
                  enabled: statuses[index]
                } : null,
            }));
            this.updateModuleList(combinedResults)
          });
        }
      })
      this.timer = setTimeout(this.autoRefreshList, 30000)   // 每30秒刷新一次
    },
    updateModuleList(modules){
        this.$store.commit('updateList', [])      // 确保界面有刷新，直接提交新对象时会刷新失败
        this.$store.commit('updateList', modules.sort((a,b) => a.moduleName.localeCompare(b.moduleName)))
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
      clearTimeout(this.timer)
      clearTimeout(this.delayTimer)
      row.runtime.enabled = !row.runtime.enabled
      await moduleApi.toggleModuleState(row.moduleName)
      this.delayTimer = setTimeout(this.autoRefreshList, 1000)
    },
    tailModuleLog(row) {
      this.$parent.handleSelect('9', { module: row.moduleName })
    }
  }
}
</script>

<style></style>
