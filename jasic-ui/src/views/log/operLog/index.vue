<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true" size="small">
        <el-form-item label="操作模块" prop="title">
          <el-input v-model="queryParams.title" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="操作人" prop="operUserName">
          <el-input v-model="queryParams.operUserName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="操作类型" prop="operType">
          <el-select v-model="queryParams.operType" placeholder="全部" clearable>
            <el-option label="新增" :value="1" />
            <el-option label="修改" :value="2" />
            <el-option label="删除" :value="3" />
            <el-option label="登录" :value="10" />
            <el-option label="登出" :value="11" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button type="danger" icon="el-icon-delete" size="small" v-hasPerms="['log:operLog:remove']" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button type="danger" icon="el-icon-delete" size="small" v-hasPerms="['log:operLog:remove']" plain @click="handleClean">清空</el-button>
      </div>
      <el-table v-loading="loading" :data="logList" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="日志ID" prop="id" width="80" />
        <el-table-column label="操作模块" prop="title" width="130" />
        <el-table-column label="操作类型" prop="operType" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="operTypeTagMap[row.operType] || ''" size="mini">
              {{ operTypeMap[row.operType] || row.operType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求方式" prop="requestMethod" width="90" align="center" />
        <el-table-column label="操作人" prop="operUserName" width="100" />
        <el-table-column label="IP地址" prop="ip" width="130" />
        <el-table-column label="请求URL" prop="requestUrl" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" prop="costTime" width="90" align="right" />
        <el-table-column label="操作时间" prop="operTime" width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleDetail(row)">详情</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog title="操作日志详情" :visible.sync="detailVisible" width="650px" append-to-body>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="日志ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ operTypeMap[detail.operType] }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operUserName }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ detail.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="调用方法" :span="2">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div style="max-height: 120px; overflow-y: auto; word-break: break-all;">{{ detail.requestParam }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <div style="max-height: 120px; overflow-y: auto; word-break: break-all;">{{ detail.responseResult }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="mini">
            {{ detail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span style="color: #F56C6C;">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { listOperLog, deleteOperLog, cleanOperLog } from '@/api/log'

export default {
  name: 'OperLogManage',
  data() {
    return {
      loading: false,
      logList: [],
      total: 0,
      queryParams: {
        pageNum: 1, pageSize: 10,
        title: '', operUserName: '', operType: undefined, status: undefined,
        beginTime: '', endTime: ''
      },
      dateRange: [],
      selectedIds: [],
      operTypeMap: { 1: '新增', 2: '修改', 3: '删除', 10: '登录', 11: '登出', 12: '强退', 99: '其他' },
      operTypeTagMap: { 1: 'success', 2: '', 3: 'danger', 10: 'warning', 11: 'info', 12: 'danger' },
      detailVisible: false,
      detail: {}
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = this.dateRange[0]
        params.endTime = this.dateRange[1]
      }
      listOperLog(params).then(res => {
        if (!res) return
        this.logList = res.data.records
        this.total = res.data.total
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.dateRange = []
      this.queryParams = {
        pageNum: 1, pageSize: 10,
        title: '', operUserName: '', operType: undefined, status: undefined,
        beginTime: '', endTime: ''
      }
      this.getList()
    },
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id)
    },
    handleBatchDelete() {
      this.$confirm(`确认删除选中的 ${this.selectedIds.length} 条日志？`, '提示', { type: 'warning' }).then(() => {
        deleteOperLog(this.selectedIds.join(',')).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleClean() {
      this.$confirm('确认清空所有操作日志？此操作不可恢复！', '警告', { type: 'warning' }).then(() => {
        cleanOperLog().then(res => {
          if (!res) return
          this.$message.success('清空成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleDetail(row) {
      this.detail = row
      this.detailVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
