<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" size="small">
        <el-form-item label="总部公司">
          <el-select v-model="queryCompanyId" placeholder="请选择" filterable @change="getList">
            <el-option v-for="c in hqOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="getList">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:region:add']" @click="handleAdd" :disabled="!queryCompanyId">新增大区</el-button>
      </div>
      <el-table v-loading="loading" :data="regionList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="大区名称" prop="regionName" width="200" />
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:region:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:region:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="460px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="大区名称" prop="regionName">
          <el-input v-model="form.regionName" placeholder="如：华东大区" />
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
import { listRegion, addRegion, updateRegion, deleteRegion } from '@/api/system'
import { listCompany, listCompanyType } from '@/api/org'

export default {
  name: 'RegionManage',
  data() {
    return {
      loading: false,
      regionList: [],
      queryCompanyId: undefined,
      hqOptions: [],
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        regionName: [{ required: true, message: '请输入大区名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadHqOptions()
  },
  methods: {
    loadHqOptions() {
      const typeCodeMap = {}
      listCompanyType().then(res => {
        if (!res) return
        (res.data || []).forEach(t => { typeCodeMap[t.typeCode] = t.subjectType })
      }).then(() => {
        listCompany({ pageNum: 1, pageSize: 999 }).then(res => {
          if (!res) return
          this.hqOptions = (res.data.records || []).filter(c => typeCodeMap[c.typeCode] === 'HQ')
          if (this.hqOptions.length > 0 && !this.queryCompanyId) {
            this.queryCompanyId = this.hqOptions[0].id
            this.getList()
          }
        })
      })
    },
    getList() {
      if (!this.queryCompanyId) return
      this.loading = true
      listRegion(this.queryCompanyId).then(res => {
        if (!res) return
        this.regionList = res.data || []
      }).finally(() => { this.loading = false })
    },
    handleAdd() {
      this.dialogTitle = '新增大区'
      this.form = { companyId: this.queryCompanyId }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑大区'
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateRegion : addRegion
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除大区"${row.regionName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteRegion(row.id).then(res => {
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
