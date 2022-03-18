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
    <ModulePlayback :visible.sync="modulePlaybackVisible" :data="playbackableList" />
    <el-table height="100%" :data="list">
      <el-table-column label="模组名称" prop="moduleName" align="center" width="100px" />
      <el-table-column label="策略模式" prop="type" align="center" width="90px" />
      <el-table-column label="绑定账户" prop="accountGatewayId" align="center" />
      <el-table-column label="信号策略" prop="signalPolicy.componentMeta.name" align="center" />
      <el-table-column label="风控策略" align="center">
        <template slot-scope="scope">
          {{ scope.row.riskControlRules.map((i) => i.componentMeta.name).join(', ') }}
        </template>
      </el-table-column>
      <el-table-column label="交易策略" prop="dealer.componentMeta.name" align="center" />
      <el-table-column label="启停切换" prop="enabled" align="center" width="100px">
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
          <el-button size="mini" @click="modulePlaybackVisible = true">回测</el-button>
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
import ModulePlayback from '@/components/ModulePlayback'

import moduleApi from '@/api/moduleApi'

export default {
  components: {
    ModuleForm,
    ModulePerf,
    ModulePlayback
  },
  data() {
    return {
      moduleFormVisible: false,
      modulePerfVisible: false,
      modulePlaybackVisible: false,
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
      console.log(index, row)
      this.curTableIndex = index
      this.curModule = row
      this.modulePerfVisible = true
    },
    handleRow(index, row) {
      console.log(index, row)
      this.curTableIndex = index
      this.curModule = row
      this.moduleFormVisible = true
    },
    async handleDelete(index, row) {
      await moduleApi.removeModule(row.moduleName)
      this.findAll()
    },
    async onSave(obj) {
      console.log(obj)
      if (this.curTableIndex < 0) {
        await moduleApi.insertModule(obj)
      } else {
        await moduleApi.updateModule(obj)
      }
      this.findAll()
    },
    async findAll() {
      this.list = await moduleApi.getAllModules()
      console.log(this.list)
    },
    async toggle(index, row) {
      console.log(index, row)
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
