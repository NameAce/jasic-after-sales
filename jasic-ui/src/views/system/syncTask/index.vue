<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="任务编码" prop="taskCode">
          <el-input v-model="queryParams.taskCode" placeholder="请输入任务编码" clearable />
        </el-form-item>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable />
        </el-form-item>
        <el-form-item label="处理器" prop="handlerCode">
          <el-select v-model="queryParams.handlerCode" placeholder="全部" clearable>
            <el-option
              v-for="item in handlerOptions"
              :key="item.handlerCode"
              :label="item.handlerName"
              :value="item.handlerCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:syncTask:add']" @click="handleAdd">
          新增任务
        </el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="任务编码" prop="taskCode" min-width="180" />
        <el-table-column label="任务名称" prop="taskName" min-width="180" />
        <el-table-column label="处理器" prop="handlerName" min-width="140" />
        <el-table-column label="Cron表达式" prop="cronExpression" min-width="160" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近状态" prop="lastStatus" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.lastStatus" :type="statusTagType(row.lastStatus)" size="mini">
              {{ row.lastStatus }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="最近结束时间" prop="lastEndTime" width="170" />
        <el-table-column label="下一次触发" prop="nextFireTime" width="170" />
        <el-table-column label="备注" prop="remark" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:syncTask:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:syncTask:execute']" @click="handleExecute(row)">执行</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:syncTask:log']" @click="handleViewLogs(row)">日志</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="val => { queryParams.pageSize = val; getList() }"
        @current-change="val => { queryParams.pageNum = val; getList() }"
      />
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务编码" prop="taskCode">
          <el-input v-model="form.taskCode" placeholder="请输入任务编码" />
        </el-form-item>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="处理器" prop="handlerCode">
          <el-select v-model="form.handlerCode" placeholder="请选择处理器" style="width: 100%;">
            <el-option
              v-for="item in handlerOptions"
              :key="item.handlerCode"
              :label="item.handlerName"
              :value="item.handlerCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="请输入Cron表达式，例如 0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="logDialogTitle" :visible.sync="logDialogVisible" width="1100px" append-to-body>
      <el-form :model="logQueryParams" :inline="true" size="small" style="margin-bottom: 12px;">
        <el-form-item label="状态" prop="status">
          <el-select v-model="logQueryParams.status" placeholder="全部" clearable>
            <el-option label="RUNNING" value="RUNNING" />
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleLogQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetLogQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="logLoading" :data="logTableData" border stripe max-height="460">
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="mini">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" prop="startTime" width="170" />
        <el-table-column label="结束时间" prop="endTime" width="170" />
        <el-table-column label="数据开始时间" prop="dataStartTime" width="170" />
        <el-table-column label="数据结束时间" prop="dataEndTime" width="170" />
        <el-table-column label="执行信息" prop="message" min-width="280" show-overflow-tooltip />
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="logQueryParams.pageNum"
        :page-size="logQueryParams.pageSize"
        :total="logTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="val => { logQueryParams.pageSize = val; getLogList() }"
        @current-change="val => { logQueryParams.pageNum = val; getLogList() }"
      />
      <div slot="footer">
        <el-button @click="logDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listSyncTask,
  getSyncTask,
  listSyncTaskHandlerOptions,
  addSyncTask,
  updateSyncTask,
  executeSyncTask,
  listSyncTaskLog
} from '@/api/system'

export default {
  name: 'SyncTaskManage',
  data() {
    return {
      loading: false,
      total: 0,
      tableData: [],
      handlerOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskCode: '',
        taskName: '',
        handlerCode: undefined,
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        taskCode: [{ required: true, message: '请输入任务编码', trigger: 'blur' }],
        taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
        handlerCode: [{ required: true, message: '请选择处理器', trigger: 'change' }],
        cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      logDialogVisible: false,
      logDialogTitle: '',
      currentLogTaskId: undefined,
      logLoading: false,
      logTotal: 0,
      logTableData: [],
      logQueryParams: {
        pageNum: 1,
        pageSize: 10,
        taskId: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.loadHandlerOptions()
    this.getList()
  },
  methods: {
    loadHandlerOptions() {
      listSyncTaskHandlerOptions().then(res => {
        if (!res) return
        this.handlerOptions = res.data || []
      })
    },
    getList() {
      this.loading = true
      listSyncTask(this.queryParams).then(res => {
        if (!res) return
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        taskCode: '',
        taskName: '',
        handlerCode: undefined,
        status: undefined
      }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增同步任务'
      this.form = {
        taskCode: '',
        taskName: '',
        handlerCode: '',
        cronExpression: '0 0 2 * * ?',
        status: 0,
        remark: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑同步任务'
      getSyncTask(row.id).then(res => {
        if (!res) return
        this.form = res.data || {}
        this.dialogVisible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateSyncTask : addSyncTask
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleExecute(row) {
      this.$confirm(`确认立即执行任务"${row.taskName}"吗？`, '提示', { type: 'warning' }).then(() => {
        executeSyncTask(row.id).then(res => {
          if (!res) return
          this.$message.success(`任务已提交，执行日志ID：${res.data}`)
          this.getList()
          if (this.currentLogTaskId === row.id) {
            this.getLogList()
          }
        })
      }).catch(() => {})
    },
    handleViewLogs(row) {
      this.currentLogTaskId = row.id
      this.logDialogTitle = `执行日志 - ${row.taskName}`
      this.logQueryParams = {
        pageNum: 1,
        pageSize: 10,
        taskId: row.id,
        status: undefined
      }
      this.logDialogVisible = true
      this.getLogList()
    },
    getLogList() {
      this.logLoading = true
      listSyncTaskLog(this.logQueryParams).then(res => {
        if (!res) return
        this.logTableData = res.data.records || []
        this.logTotal = res.data.total || 0
      }).finally(() => { this.logLoading = false })
    },
    handleLogQuery() {
      this.logQueryParams.pageNum = 1
      this.getLogList()
    },
    resetLogQuery() {
      this.logQueryParams = {
        pageNum: 1,
        pageSize: 10,
        taskId: this.currentLogTaskId,
        status: undefined
      }
      this.getLogList()
    },
    statusTagType(status) {
      if (status === 'SUCCESS') return 'success'
      if (status === 'FAILED') return 'danger'
      if (status === 'RUNNING') return 'warning'
      return 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
