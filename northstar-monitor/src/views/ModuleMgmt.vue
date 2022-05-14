<template>
  <div class="ns-page">
    <ModuleForm
      :visible.sync="moduleFormVisible"
      :readOnly="curTableIndex > -1 && curModule.enabled"
      :module="curModule"
      @onSave="onSave"
    />
    <ModulePerf
      :moduleName="curModule ? curModule.moduleName : ''"
      :visible.sync="modulePerfVisible"
    />
    <el-table height="100%" :data="list">
      <el-table-column label="模组名称" prop="moduleName" align="center" width="200px" />
      <el-table-column label="模组类型" prop="type" align="center" width="90px">
        <template slot-scope="scope">
          {{ { SPECULATION: '投机', ARBITRAGE: '套利' }[scope.row.type] }}
        </template>
      </el-table-column>
      <el-table-column label="模组周期" prop="barInterval" align="center" width="90px">
        <template slot-scope="scope">
          {{ `${scope.row.numOfMinPerBar} 分钟` }}
        </template>
      </el-table-column>
      <el-table-column label="交易策略" align="center" width="200px">
        <template slot-scope="scope">
          {{ scope.row.strategySetting.componentMeta.name }}
        </template>
      </el-table-column>
      <el-table-column label="平仓优化" align="center" width="90px">
        <template slot-scope="scope">
          {{
            { FIFO: '先开先平', PRIOR_TODAY: '平今优先', PRIOR_BEFORE_HEGDE_TODAY: '平昨锁今' }[
              scope.row.closingPolicy
            ]
          }}
        </template>
      </el-table-column>
      <el-table-column label="绑定账户" align="center" width="300px">
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
      <el-table-column label="绑定合约" align="center" min-width="200px">
        <template slot-scope="scope">
          {{
            (() => {
              return scope.row.moduleAccountSettingsDescription
                .map((item) => item.bindedUnifiedSymbols.join('，'))
                .join('；')
            })()
          }}
        </template>
      </el-table-column>

      <el-table-column label="当前状态/切换" prop="enabled" align="center" width="100px">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.enabled"
            type="success"
            @click.native="toggle(scope.$index, scope.row)"
            >启用</el-button
          >
          <el-button
            v-if="!scope.row.enabled"
            type="danger"
            @click.native="toggle(scope.$index, scope.row)"
            >停用</el-button
          >
        </template>
      </el-table-column>
      <el-table-column align="center" width="240px">
        <template slot="header">
          <el-button size="mini" type="primary" @click="handleCreate">新建</el-button>
        </template>
        <template slot-scope="scope">
          <el-button size="mini" @click="handlePerf(scope.$index, scope.row)">透视</el-button>
          <el-button size="mini" @click="handleRow(scope.$index, scope.row)">{{
            scope.row.enabled ? '查看' : '修改'
          }}</el-button>
          <el-popconfirm
            class="ml-10"
            title="确定移除吗？"
            @confirm="handleDelete(scope.$index, scope.row)"
          >
            <el-button slot="reference" size="mini" type="danger">删除</el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import ModuleForm from '@/components/ModuleForm'
import ModulePerf from '@/components/ModulePerformance'

import moduleApi from '@/api/moduleApi'

export default {
  components: {
    ModuleForm,
    ModulePerf
  },
  data() {
    return {
      moduleFormVisible: false,
      modulePerfVisible: false,
      curTableIndex: -1,
      curModule: null,
      list: []
    }
  },
  computed: {
    playbackableList() {
      return this.list.filter((i) => !i.enabled)
    }
  },
  mounted() {
    this.findAll()
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
      this.modulePerfVisible = true
    },
    handleRow(index, row) {
      this.curTableIndex = index
      this.curModule = row
      this.moduleFormVisible = true
    },
    async handleDelete(index, row) {
      await moduleApi.removeModule(row.moduleName)
      this.findAll()
    },
    async onSave(obj) {
      if (this.curTableIndex < 0) {
        await moduleApi.insertModule(obj)
      } else {
        await moduleApi.updateModule(obj)
      }
      this.findAll()
    },
    async findAll() {
      this.list = await moduleApi.getAllModules()
    },
    async toggle(index, row) {
      this.$confirm(
        `是否确定切换模组启停状态？当前状态为［${row.enabled ? '启用' : '停用'}]`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        await moduleApi.toggleModuleState(row.moduleName)
        await this.findAll()
      })
    }
  }
}
</script>

<style></style>
