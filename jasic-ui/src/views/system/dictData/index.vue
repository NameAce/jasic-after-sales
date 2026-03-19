<template>
  <div class="app-container">
    <el-card shadow="never" class="type-card">
      <div class="type-header">
        <div>
          <div class="type-title">{{ dictTypeInfo.dictName || '字典数据管理' }}</div>
          <div class="type-subtitle">{{ dictTypeInfo.dictType || '-' }}</div>
        </div>
        <el-button icon="el-icon-back" size="small" @click="handleBack">返回</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="search-card" style="margin-top: 12px;">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="queryParams.dictLabel" placeholder="请输入" clearable />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:dictData:add']" @click="handleAdd">新增</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="字典标签" prop="dictLabel" width="180">
          <template slot-scope="{ row }">
            <span v-if="!row.listClass || row.listClass === 'default'">
              {{ row.dictLabel }}
            </span>
            <el-tag v-else :type="tagType(row.listClass)" :class="row.cssClass" size="mini">
              {{ row.dictLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字典键值" prop="dictValue" width="160" />
        <el-table-column label="排序" prop="dictSort" width="80" align="center" />
        <el-table-column label="默认" prop="isDefault" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="mini">
              {{ row.isDefault === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:dictData:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:dictData:remove']" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="字典类型">
          <el-input v-model="form.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="请输入字典键值" />
        </el-form-item>
        <el-form-item label="排序" prop="dictSort">
          <el-input-number v-model="form.dictSort" :min="0" />
        </el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="form.listClass" placeholder="请选择">
            <el-option v-for="item in listClassOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义样式">
          <el-input v-model="form.cssClass" placeholder="可选" />
        </el-form-item>
        <el-form-item label="默认值" prop="isDefault">
          <el-radio-group v-model="form.isDefault">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
import { listDictData, getDictData, addDictData, updateDictData, deleteDictData, getDictType } from '@/api/system'

export default {
  name: 'DictDataManage',
  data() {
    return {
      loading: false,
      dictTypeInfo: {},
      tableData: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, dictType: '', dictLabel: '', status: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      listClassOptions: [
        { label: '默认(default)', value: 'default' },
        { label: '主要(primary)', value: 'primary' },
        { label: '成功(success)', value: 'success' },
        { label: '信息(info)', value: 'info' },
        { label: '警告(warning)', value: 'warning' },
        { label: '危险(danger)', value: 'danger' }
      ],
      rules: {
        dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
        dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
        dictSort: [{ required: true, message: '请输入排序', trigger: 'change' }],
        isDefault: [{ required: true, message: '请选择默认值', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadDictType()
  },
  watch: {
    '$route.params.dictId'() {
      this.loadDictType()
    }
  },
  methods: {
    loadDictType() {
      const dictId = this.$route.params.dictId
      if (!dictId) return
      getDictType(dictId).then(res => {
        if (!res || !res.data) return
        this.dictTypeInfo = res.data
        this.queryParams.dictType = res.data.dictType
        this.queryParams.pageNum = 1
        this.getList()
      })
    },
    getList() {
      if (!this.queryParams.dictType) return
      this.loading = true
      listDictData(this.queryParams).then(res => {
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
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        dictType: this.dictTypeInfo.dictType || '',
        dictLabel: '',
        status: undefined
      }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增字典数据'
      this.form = {
        dictType: this.dictTypeInfo.dictType,
        dictSort: 0,
        listClass: 'default',
        cssClass: '',
        isDefault: 0,
        status: 1,
        remark: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑字典数据'
      getDictData(row.id).then(res => {
        if (!res) return
        this.form = res.data
        if (!this.form.listClass) {
          this.form.listClass = 'default'
        }
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateDictData : addDictData
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除数据项"${row.dictLabel}"？`, '提示', { type: 'warning' }).then(() => {
        deleteDictData(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleBack() {
      if (window.history.length > 1) {
        this.$router.back()
      } else {
        this.$router.push('/system/dictType')
      }
    },
    tagType(listClass) {
      return listClass === 'primary' ? '' : listClass
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.type-card { margin-bottom: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
.type-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.type-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.type-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
