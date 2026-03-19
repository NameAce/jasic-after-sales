<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true" size="small">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="queryParams.companyName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="公司类型" prop="typeCode">
          <el-select v-model="queryParams.typeCode" placeholder="全部" clearable>
            <el-option v-for="t in typeCodeOptions" :key="t.value" :label="t.label" :value="t.value" />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['org:company:add']" @click="handleAdd">新增公司</el-button>
      </div>
      <el-table v-loading="loading" :data="companyList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="公司名称" prop="companyName" width="200" />
        <el-table-column label="公司编码" prop="companyCode" width="120" />
        <el-table-column label="公司类型" prop="typeCode" width="120" />
        <el-table-column label="主体类型" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="getSubjectType(row.typeCode) === 'HQ'" type="warning" size="mini">总部</el-tag>
            <el-tag v-else-if="getSubjectType(row.typeCode) === 'SERVICE'" type="success" size="mini">网点</el-tag>
            <el-tag v-else size="mini">平台</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" width="100" />
        <el-table-column label="联系电话" prop="contactPhone" width="130" />
        <el-table-column label="地址" prop="address" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" fixed="right" width="160">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['org:company:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['org:company:remove']" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="公司编码" prop="companyCode">
          <el-input v-model="form.companyCode" placeholder="请输入公司编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="公司类型" prop="typeCode">
          <el-select v-model="form.typeCode" placeholder="请选择" :disabled="!!form.id">
            <el-option v-for="t in typeCodeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="管理员用户名" prop="adminUsername">
          <el-input v-model="form.adminUsername" placeholder="新增公司时必填，用于创建默认管理员账号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
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
import { listCompany, getCompany, addCompany, updateCompany, deleteCompany } from '@/api/org'
import { listCompanyType } from '@/api/org'

export default {
  name: 'CompanyManage',
  data() {
    return {
      loading: false,
      companyList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, companyName: '', typeCode: '', status: undefined },
      typeCodeOptions: [],
      typeCodeMap: {},
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
        companyCode: [{ required: true, message: '请输入公司编码', trigger: 'blur' }],
        typeCode: [{ required: true, message: '请选择公司类型', trigger: 'change' }],
        adminUsername: [{
          validator: (rule, value, callback) => {
            if (!this.form.id && !value) {
              callback(new Error('请输入管理员用户名'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }]
      }
    }
  },
  created() {
    this.loadTypeCodeOptions()
    this.getList()
  },
  methods: {
    loadTypeCodeOptions() {
      listCompanyType().then(res => {
        if (!res) return
        const types = res.data || []
        this.typeCodeOptions = types.map(t => ({ value: t.typeCode, label: t.typeName }))
        types.forEach(t => { this.typeCodeMap[t.typeCode] = t.subjectType })
      })
    },
    getSubjectType(typeCode) {
      return this.typeCodeMap[typeCode] || ''
    },
    getList() {
      this.loading = true
      listCompany(this.queryParams).then(res => {
        if (!res) return
        this.companyList = res.data.records
        this.total = res.data.total
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = { pageNum: 1, pageSize: 10, companyName: '', typeCode: '', status: undefined }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增公司'
      this.form = { status: 1 }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑公司'
      getCompany(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateCompany : addCompany
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除公司"${row.companyName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteCompany(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
