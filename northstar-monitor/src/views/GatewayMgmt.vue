<template>
  <div class="ns-page">
    <NsGatewayForm
      :visible.sync="dialogFormVisible"
      :gatewayDescription="curGatewayDescription"
      :gatewayUsage="gatewayUsage"
      :isUpdateMode="curTableIndex > -1"
      @onSave="handleSave"
    />
    <NsSimBalanceForm
      :visible.sync="simBalanceFormVisible"
      :simGatewayId="curGatewayDescription.gatewayId"
    />
    <el-table
      :data="
        tableData.filter(
          (data) => !search || data.name.toLowerCase().includes(search.toLowerCase())
        )
      "
      height="100%"
    >
      <el-table-column
        :label="`${typeLabel}ID`"
        prop="gatewayId"
        width="200px"
        header-align="center"
        align="center"
      >
      </el-table-column>
      <el-table-column
        :label="`${typeLabel}描述`"
        prop="description"
        width="120px"
        header-align="center"
        align="center"
      >
      </el-table-column>
      <el-table-column
        :label="`${typeLabel}类型`"
        prop="gatewayType"
        width="80px"
        header-align="center"
        align="center"
      >
      </el-table-column>
      <el-table-column
        label="连接状态"
        prop="connectionState"
        width="80px"
        header-align="center"
        align="center"
      >
        <template slot-scope="scope">
          <span
            :class="
              scope.row.connectionState === 'CONNECTED'
                ? 'color-green'
                : scope.row.connectionState === 'DISCONNECTED'
                ? 'color-red'
                : ''
            "
            >{{
              {
                CONNECTING: '连接中',
                CONNECTED: '已连接',
                DISCONNECTING: '断开中',
                DISCONNECTED: '已断开'
              }[scope.row.connectionState]
            }}</span
          >
        </template>
      </el-table-column>
      <el-table-column
        label="自动连接"
        prop="autoConnect"
        width="80px"
        header-align="center"
        align="center"
      >
        <template slot-scope="scope">
          {{ scope.row.autoConnect ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="gatewayUsage !== 'TRADE'"
        label="行情反馈"
        width="80px"
        header-align="center"
        align="center"
      >
        <template slot-scope="scope">
          {{ scope.row.isActive ? '活跃' : '-' }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="gatewayUsage === 'TRADE'"
        label="关联网关"
        prop="bindedMktGatewayId"
        width="150px"
        header-align="center"
        align="center"
      >
      </el-table-column>
      <el-table-column
        label="适配器类型"
        prop="gatewayAdapterType"
        header-align="center"
        align="center"
      >
      </el-table-column>
      <el-table-column align="center" width="300px">
        <template slot="header">
          <el-button size="mini" type="primary" @click="handleCreate">新建</el-button>
        </template>
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.connectionState === 'DISCONNECTED'"
            size="mini"
            type="success"
            @click="connect(scope.row)"
            >连线</el-button
          >
          <el-button
            v-if="scope.row.connectionState === 'CONNECTED'"
            size="mini"
            type="danger"
            @click="disconnect(scope.row)"
            >断开</el-button
          >
          <el-button
            size="mini"
            v-if="
              gatewayUsage === 'TRADE' &&
              scope.row.connectionState === 'CONNECTED' &&
              scope.row.gatewayType === 'SIM'
            "
            @click="handleMoneyIO(scope.row)"
            >出入金</el-button
          >
          <el-button
            size="mini"
            @click="handleEdit(scope.$index, scope.row)"
            :disabled="scope.row.connectionState !== 'DISCONNECTED'"
            >修改</el-button
          >
          <el-popconfirm
            class="ml-10"
            title="确定移除吗？"
            @confirm="handleDelete(scope.$index, scope.row)"
          >
            <el-button
              size="mini"
              type="danger"
              slot="reference"
              :disabled="scope.row.connectionState !== 'DISCONNECTED'"
              >删除</el-button
            ></el-popconfirm
          >
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import NsGatewayForm from '@/components/GatewayForm'
import NsSimBalanceForm from '@/components/SimBalanceMgmt'
import gatewayMgmtApi from '../api/gatewayMgmtApi'

let timer

export default {
  components: {
    NsGatewayForm,
    NsSimBalanceForm
  },
  props: {
    gatewayUsage: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      dialogFormVisible: false,
      simBalanceFormVisible: false,
      curTableIndex: -1,
      curGatewayDescription: {},
      tableData: [],
      search: ''
    }
  },
  beforeDestroy() {
    clearTimeout(timer)
  },
  mounted() {
    console.log('GatewayManagement created. Usage:' + this.gatewayUsage)
    const timelyUpdate = () => {
      timer = setTimeout(timelyUpdate, 5000)
      this.updateList().catch(() => clearTimeout(timer))
    }
    timelyUpdate()
  },
  computed: {
    typeLabel() {
      return this.gatewayUsage === 'TRADE' ? '账户' : '网关'
    }
  },
  methods: {
    async updateList() {
      const data = await gatewayMgmtApi.findAll(this.gatewayUsage)
      if (this.gatewayUsage !== 'TRADE') {
        const tableDataPromise = data.map(async (item) => {
          item.isActive = await gatewayMgmtApi.isActive(item.gatewayId)
          return item
        })
        this.tableData = await Promise.all(tableDataPromise)
      } else {
        this.tableData = data
      }
    },
    handleCreate() {
      this.dialogFormVisible = true
      this.curTableIndex = -1
      this.curGatewayDescription = {}
    },
    handleEdit(index, row) {
      console.log(index, row)
      this.curTableIndex = index
      this.curGatewayDescription = row
      this.dialogFormVisible = true
    },
    async handleDelete(index, row) {
      console.log(index, row)
      await gatewayMgmtApi.remove(row.gatewayId)
      this.updateList()
    },
    async handleSave(obj) {
      console.log(obj)
      if (this.curTableIndex > -1) {
        await gatewayMgmtApi.update(obj)
        this.updateList()
        return
      }
      this.tableData.push(obj)
      await gatewayMgmtApi.create(obj)
      this.updateList()
    },
    async connect(row) {
      await gatewayMgmtApi.connect(row.gatewayId)
      this.updateList()
    },
    async disconnect(row) {
      await gatewayMgmtApi.disconnect(row.gatewayId)
      this.updateList()
    },
    handleMoneyIO(row) {
      console.log(row)
      this.curGatewayDescription = row
      this.simBalanceFormVisible = true
    }
  }
}
</script>

<style></style>
