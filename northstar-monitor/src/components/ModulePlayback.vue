<template>
  <el-dialog
    title="模组回测"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    class="module-dialog"
    width="500px"
  >
    <div class="warning-text"><i class="el-icon-warning" /> 只有停用的模组才能进行回测</div>
    <PlaybackPerformance
      :visible.sync="playbackPerformanceVisible"
      :moduleName="playbackDetailOf"
    />
    <el-row class="mb-10" :gutter="10">
      <el-col :span="9">
        <el-input
          type="number"
          placeholder="回测账户初始金额"
          prefix-icon="el-icon-money"
          v-model="playbackAccountInitBalance"
          clearable
        ></el-input>
      </el-col>
      <el-col :span="9">
        <el-input
          type="number"
          placeholder="每笔手续费（元）"
          prefix-icon="el-icon-thumb"
          v-model="playbackTickOfFee"
          clearable
        ></el-input>
      </el-col>
    </el-row>
    <el-row>
      <el-date-picker
        v-model="dates"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :picker-options="pickerOptions"
      >
      </el-date-picker>
      <el-button class="btn-position ml-10" @click="startPlayback" :disabled="playbackRunning">
        开始回测
      </el-button>
    </el-row>
    <el-table
      :data="data"
      style="width: 100%"
      height="400"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" align="center"> </el-table-column>
      <el-table-column prop="moduleName" label="模组名称" width="120" align="center">
      </el-table-column>
      <el-table-column prop="playbackBalance" label="回测账户余额" align="center">
      </el-table-column>
      <el-table-column :label="`回测进度：${playbackProcess}%`" align="center">
        <template slot-scope="scope">
          <el-button @click="playbackRecord(scope.row.moduleName)">回测明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div slot="footer"></div>
  </el-dialog>
</template>

<script>
import playbackApi from '@/api/playbackApi'
import PlaybackPerformance from './PlaybackPerformance.vue'
export default {
  components: {
    PlaybackPerformance
  },
  props: {
    data: {
      type: Array,
      default: () => []
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      playbackPerformanceVisible: false,
      playbackRunning: false,
      playbackProcess: 0,
      playbackAccountInitBalance: '',
      playbackTickOfFee: '',
      playbackDetailOf: '',
      dates: [],
      chosenModule: [],
      pickerOptions: {
        disabledDate(time) {
          return time.getTime() > Date.now() || time.getTime() < new Date('2021-10-01').getTime()
        }
      }
    }
  },
  watch: {
    visible: async function (val) {
      if (val) {
        this.dialogVisible = val
        this.playbackRunning = !(await playbackApi.getPlaybackReadiness())
      }
    },
    dialogVisible: function (val) {
      if (!val) {
        this.$emit('update:visible', val)
      }
    }
  },
  methods: {
    async startPlayback() {
      if (!this.dates.length) {
        throw new Error('回测起止日期未填')
      }
      if (!this.chosenModule.length) {
        throw new Error('未选中回测模组')
      }
      if (!this.playbackAccountInitBalance) {
        throw new Error('未填入回测账户初始余额')
      }
      this.playbackRunning = true
      await playbackApi.startPlay(
        this.dates[0].format('yyyyMMdd'),
        this.dates[1].format('yyyyMMdd'),
        this.chosenModule.map((i) => i.moduleName),
        this.playbackAccountInitBalance,
        this.playbackTickOfFee
      )
      const checkProcessJobs = async () => {
        this.playbackProcess = await playbackApi.getProcess()
        this.chosenModule.map((i) => {
          playbackApi.getBalance(i.moduleName).then((balance) => {
            i.playbackBalance = balance
          })
        })
        if (this.playbackProcess < 100) {
          setTimeout(checkProcessJobs, 3000)
        } else {
          this.playbackRunning = false
        }
      }
      checkProcessJobs()
    },
    handleSelectionChange(selection) {
      this.chosenModule = selection
    },
    playbackRecord(moduleName) {
      this.playbackDetailOf = moduleName
      this.playbackPerformanceVisible = true
    }
  }
}
</script>

<style>
.btn-position {
  position: absolute;
}
input.el-range-input {
  background-color: transparent !important;
}
.el-date-table td.in-range div,
.el-date-table td.in-range div:hover,
.el-date-table.is-week-mode .el-date-table__row.current div,
.el-date-table.is-week-mode .el-date-table__row:hover div {
  background-color: darkgray;
}
</style>
