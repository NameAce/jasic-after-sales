<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="queryParams.configName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="queryParams.configKey" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="是否内置" prop="configType">
          <el-select v-model="queryParams.configType" placeholder="全部" clearable>
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:config:add']" @click="handleAdd">新增</el-button>
        <el-button icon="el-icon-refresh" size="small" v-hasPerms="['system:config:refresh']" @click="handleRefreshCache">刷新缓存</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="参数名称" prop="configName" width="200" />
        <el-table-column label="参数键名" prop="configKey" width="260" />
        <el-table-column label="参数键值" prop="configValue" />
        <el-table-column label="内置" prop="configType" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.configType === 1 ? 'warning' : 'info'" size="mini">
              {{ row.configType === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:config:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              v-hasPerms="['system:config:remove']"
              :disabled="row.configType === 1"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="4" placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="是否内置" prop="configType">
          <el-radio-group v-model="form.configType">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, addConfig, updateConfig, deleteConfig, refreshConfigCache } from '@/api/system'

export default {
  name: 'ConfigManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, configName: '', configKey: '', configType: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
        configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
        configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }],
        configType: [{ required: true, message: '请选择是否内置', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listConfig(this.queryParams).then(res => {
        if (!res) return
        this.tableData = res.data.records
        this.total = res.data.total
      }).finally(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = { pageNum: 1, pageSize: 10, configName: '', configKey: '', configType: undefined }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增参数'
      this.form = { configValue: '', configType: 0, remark: '' }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑参数'
      getConfig(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateConfig : addConfig
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除参数"${row.configName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteConfig(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleRefreshCache() {
      refreshConfigCache().then(res => {
        if (!res) return
        this.$message.success('缓存刷新成功')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
