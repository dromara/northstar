<template>
  <div class="ns-page">
    <ModuleForm
      :visible.sync="moduleFormVisible"
      :readOnly="curTableIndex > -1 && curModule.runtime.enabled"
      :module="curModule"
      @onSave="onSave"
    />
    <ModuleRuntime
      :visible.sync="ModuleRuntimeVisible"
      :module="curTableIndex > -1 ? curModule : ''"
      :moduleRuntimeSrc="curTableIndex > -1 ? curModule.runtime : ''"
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

      <el-table-column label="当前状态" prop="enabled" align="center" width="90px">
        <template slot-scope="scope">
          <span :class="scope.row.runtime.enabled ? 'color-green' : 'color-red'">
            {{ scope.row.runtime.enabled ? '运行中' : '已停用' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" width="320px">
        <template slot="header">
          <el-button size="mini" type="primary" @click="handleCreate">新建</el-button>
        </template>
        <template slot-scope="scope">
          <el-popconfirm
            v-if="scope.row.runtime.enabled"
            class="mr-10"
            title="确定停用吗？"
            @confirm="toggle(scope.$index, scope.row)"
          >
            <el-button type="danger" slot="reference">停用</el-button>
          </el-popconfirm>
          <el-button
            v-if="!scope.row.runtime.enabled"
            type="success"
            size="mini"
            @click.native="toggle(scope.$index, scope.row)"
          >
            启用
          </el-button>
          <el-button size="mini" @click="handlePerf(scope.$index, scope.row)">运行状态</el-button>
          <el-button size="mini" @click="handleRow(scope.$index, scope.row)">{{
            scope.row.runtime.enabled ? '查看' : '修改'
          }}</el-button>
          <el-popconfirm
            class="ml-10"
            title="确定移除吗？"
            @confirm="handleDelete(scope.$index, scope.row)"
          >
            <el-button v-if="!scope.row.runtime.enabled" slot="reference" size="mini" type="danger">
              删除
            </el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import ModuleForm from '@/components/ModuleForm'
import ModuleRuntime from '@/components/ModuleRuntime'

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
      list: []
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
      this.ModuleRuntimeVisible = true
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
      const results = await moduleApi.getAllModules()
      const allReq = results.map(async (item) => {
        item.runtime = await moduleApi.getModuleRuntime(item.moduleName)
        return item
      })
      this.list = await Promise.all(allReq)
    },
    async toggle(index, row) {
      await moduleApi.toggleModuleState(row.moduleName)
      await this.findAll()
    }
  }
}
</script>

<style></style>
