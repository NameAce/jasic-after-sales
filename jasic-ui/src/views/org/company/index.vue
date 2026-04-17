<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="queryParams.companyName" placeholder="请输入公司名称" clearable />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['org:company:add']" @click="handleAdd()">新增公司</el-button>
        <el-button type="success" icon="el-icon-download" size="small" v-hasPerms="['org:company:add']" @click="openExternalDialog">从 CRM 导入</el-button>
      </div>
      <el-table v-loading="loading" :data="companyList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="公司名称" prop="companyName" min-width="180" />
        <el-table-column label="公司简称" prop="companyShortName" width="140" />
        <el-table-column label="公司编码" prop="companyCode" width="140" />
        <el-table-column label="公司类型" width="120">
          <template slot-scope="{ row }">
            {{ getTypeLabel(row.typeCode) || row.typeCode }}
          </template>
        </el-table-column>
        <el-table-column label="主体类型" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="getSubjectType(row.typeCode) === 'HQ'" type="warning" size="mini">总部</el-tag>
            <el-tag v-else-if="getSubjectType(row.typeCode) === 'SERVICE'" type="success" size="mini">网点</el-tag>
            <el-tag v-else size="mini">平台</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90">
          <template slot-scope="{ row }">
            <el-tag :type="row.sourceType === 'CRM' ? 'info' : 'success'" size="mini">
              {{ row.sourceType || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" width="100" />
        <el-table-column label="联系电话" prop="contactPhone" width="130" />
        <el-table-column label="客服电话" prop="servicePhone" width="130" />
        <el-table-column label="销售组织" width="120">
          <template slot-scope="{ row }">
            {{ getSubjectType(row.typeCode) === 'HQ' ? (row.salesOrg || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="地区" min-width="180">
          <template slot-scope="{ row }">
            {{ formatRegion(row) }}
          </template>
        </el-table-column>
        <el-table-column label="地址" prop="address" min-width="220" show-overflow-tooltip />
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入公司名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司简称" prop="companyShortName">
              <el-input v-model="form.companyShortName" placeholder="请输入公司简称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司编码" prop="companyCode">
              <el-input
                v-model="form.companyCode"
                :disabled="!!form.id || companyCodeLocked"
                placeholder="请输入公司编码"
                @input="handleCompanyCodeInput"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司类型" prop="typeCode">
              <el-select v-model="form.typeCode" placeholder="请选择" :disabled="!!form.id">
                <el-option v-for="t in typeCodeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客服电话" prop="servicePhone">
              <el-input v-model="form.servicePhone" placeholder="请输入客服电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-input :value="form.sourceType || 'MANUAL'" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省份" prop="provinceName">
              <el-input v-model="form.provinceName" placeholder="请输入省份" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市" prop="cityName">
              <el-input v-model="form.cityName" placeholder="请输入城市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县" prop="districtName">
              <el-input v-model="form.districtName" placeholder="请输入区县" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入公司地址" />
        </el-form-item>

        <el-form-item v-if="isHqType(form.typeCode)" label="销售组织" prop="salesOrg">
          <el-input v-model="form.salesOrg" placeholder="请输入销售组织" />
        </el-form-item>

        <el-form-item v-if="!form.id" label="管理员用户名" prop="adminUsername">
          <el-input
            v-model="form.adminUsername"
            placeholder="新增公司时必填，用于创建默认管理员账号"
            @input="handleAdminUsernameInput"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="选择 CRM 公司" :visible.sync="externalDialogVisible" width="1100px" append-to-body>
      <el-form ref="externalQueryForm" :model="externalQueryParams" :inline="true" size="small">
        <el-form-item label="SAP 公司编码" prop="companyCode">
          <el-input v-model="externalQueryParams.companyCode" placeholder="请输入 SAP 公司编码" clearable />
        </el-form-item>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="externalQueryParams.companyName" placeholder="请输入公司名称" clearable />
        </el-form-item>
        <el-form-item label="CRM 状态" prop="custState">
          <el-select v-model="externalQueryParams.custState" placeholder="全部" clearable>
            <el-option v-for="item in externalStateOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleExternalQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetExternalQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="externalLoading" :data="externalCompanyList" border stripe style="margin-top: 12px;">
        <el-table-column label="SAP 公司编码" prop="companyCode" width="140" />
        <el-table-column label="公司名称" prop="companyName" min-width="180" />
        <el-table-column label="公司简称" prop="companyShortName" width="140" />
        <el-table-column label="建议类型" width="120">
          <template slot-scope="{ row }">
            {{ getTypeLabel(row.typeCode) || row.typeCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" width="100" />
        <el-table-column label="联系电话" prop="contactPhone" width="130" />
        <el-table-column label="地区" min-width="160">
          <template slot-scope="{ row }">
            {{ formatRegion(row) }}
          </template>
        </el-table-column>
        <el-table-column label="地址" prop="address" min-width="220" show-overflow-tooltip />
        <el-table-column label="CRM 状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.custState === 1 ? 'success' : 'info'" size="mini">{{ row.custStateLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="导入状态" width="100">
          <template slot-scope="{ row }">
            <el-tag v-if="row.existingCompanyId" type="warning" size="mini">已存在</el-tag>
            <el-tag v-else-if="row.canImport" type="success" size="mini">可导入</el-tag>
            <el-tag v-else type="danger" size="mini">不可导入</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因提示" prop="importDisabledReason" min-width="180" show-overflow-tooltip />
        <el-table-column label="最近同步时间" prop="lastSyncTime" width="160" />
        <el-table-column label="操作" fixed="right" width="100">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              :disabled="!row.canImport && !row.existingCompanyId"
              @click="handleUseExternal(row)"
            >选择</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="externalQueryParams.pageNum"
        :page-size="externalQueryParams.pageSize"
        :total="externalTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="val => { externalQueryParams.pageSize = val; getExternalList() }"
        @current-change="val => { externalQueryParams.pageNum = val; getExternalList() }"
      />
    </el-dialog>
  </div>
</template>

<script>
import {
  addCompany,
  deleteCompany,
  getCompany,
  getExternalCompanyImportPreview,
  listCompany,
  listCompanyType,
  listExternalCompany,
  updateCompany
} from '@/api/org'

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
      typeLabelMap: {},
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      adminUsernameManuallyEdited: false,
      lastAutoAdminUsername: '',
      submitLoading: false,
      companyCodeLocked: false,
      externalDialogVisible: false,
      externalLoading: false,
      externalCompanyList: [],
      externalTotal: 0,
      externalQueryParams: { pageNum: 1, pageSize: 10, companyCode: '', companyName: '', custState: undefined },
      externalStateOptions: [
        { value: 0, label: '待审核' },
        { value: 1, label: '审核通过' },
        { value: 2, label: '审核不通过' },
        { value: 3, label: '注销' },
        { value: 4, label: '资料已保存' },
        { value: 5, label: '申请注销' },
        { value: 6, label: '资料未填充' },
        { value: 9, label: '删除' }
      ],
      rules: {
        companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
        companyCode: [{
          validator: (rule, value, callback) => {
            if (!this.isHqType(this.form.typeCode) && !value) {
              callback(new Error('请输入公司编码'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }],
        typeCode: [{ required: true, message: '请选择公司类型', trigger: 'change' }],
        contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        address: [{ required: true, message: '请输入公司地址', trigger: 'blur' }],
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
        const types = (res && res.data) || []
        this.typeCodeOptions = types.map(t => ({ value: t.typeCode, label: t.typeName }))
        this.typeCodeMap = {}
        this.typeLabelMap = {}
        types.forEach(t => {
          this.typeCodeMap[t.typeCode] = t.subjectType
          this.typeLabelMap[t.typeCode] = t.typeName
        })
      })
    },
    getSubjectType(typeCode) {
      return this.typeCodeMap[typeCode] || ''
    },
    isHqType(typeCode) {
      return this.getSubjectType(typeCode) === 'HQ'
    },
    getTypeLabel(typeCode) {
      return this.typeLabelMap[typeCode] || ''
    },
    formatRegion(row) {
      return [row.provinceName, row.cityName, row.districtName].filter(Boolean).join(' / ') || '-'
    },
    normalizeText(value) {
      return value == null ? '' : String(value).trim()
    },
    getDefaultAdminUsername(companyCode) {
      return this.normalizeText(companyCode)
    },
    resetAdminUsernameLinkState(prefill = {}) {
      const companyCode = this.getDefaultAdminUsername(prefill.companyCode)
      const adminUsername = this.normalizeText(prefill.adminUsername)
      this.adminUsernameManuallyEdited = !!adminUsername && adminUsername !== companyCode
      this.lastAutoAdminUsername = adminUsername || companyCode
    },
    handleCompanyCodeInput(value) {
      if (this.form.id || this.companyCodeLocked) {
        return
      }
      const currentAdminUsername = this.normalizeText(this.form.adminUsername)
      if (currentAdminUsername && this.adminUsernameManuallyEdited && currentAdminUsername !== this.lastAutoAdminUsername) {
        return
      }
      const defaultAdminUsername = this.getDefaultAdminUsername(value)
      this.form.adminUsername = defaultAdminUsername
      this.lastAutoAdminUsername = defaultAdminUsername
      this.adminUsernameManuallyEdited = false
    },
    handleAdminUsernameInput(value) {
      if (this.form.id) {
        return
      }
      const currentAdminUsername = this.normalizeText(value)
      const defaultAdminUsername = this.getDefaultAdminUsername(this.form.companyCode)
      if (!currentAdminUsername || currentAdminUsername === defaultAdminUsername) {
        this.adminUsernameManuallyEdited = false
        this.lastAutoAdminUsername = currentAdminUsername
        return
      }
      this.adminUsernameManuallyEdited = true
    },
    createEmptyForm() {
      return {
        id: undefined,
        companyName: '',
        companyShortName: '',
        companyCode: '',
        typeCode: '',
        contactName: '',
        contactPhone: '',
        servicePhone: '',
        provinceName: '',
        cityName: '',
        districtName: '',
        address: '',
        salesOrg: '',
        sourceType: 'MANUAL',
        adminUsername: '',
        status: 1
      }
    },
    getList() {
      this.loading = true
      listCompany(this.queryParams).then(res => {
        if (!res) return
        this.companyList = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    getExternalList() {
      this.externalLoading = true
      listExternalCompany(this.externalQueryParams).then(res => {
        if (!res) return
        this.externalCompanyList = res.data.records || []
        this.externalTotal = res.data.total || 0
      }).finally(() => {
        this.externalLoading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = { pageNum: 1, pageSize: 10, companyName: '', typeCode: '', status: undefined }
      this.getList()
    },
    handleExternalQuery() {
      this.externalQueryParams.pageNum = 1
      this.getExternalList()
    },
    resetExternalQuery() {
      this.$refs.externalQueryForm.resetFields()
      this.externalQueryParams = { pageNum: 1, pageSize: 10, companyCode: '', companyName: '', custState: undefined }
      this.getExternalList()
    },
    handleAdd(prefill = {}) {
      this.dialogTitle = '新增公司'
      this.companyCodeLocked = !!prefill.companyCode
      this.form = Object.assign(this.createEmptyForm(), prefill)
      this.resetAdminUsernameLinkState(this.form)
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.openEditDialog(row.id)
    },
    openEditDialog(id) {
      this.dialogTitle = '编辑公司'
      this.companyCodeLocked = true
      getCompany(id).then(res => {
        if (!res) return
        this.form = Object.assign(this.createEmptyForm(), res.data || {})
        this.resetAdminUsernameLinkState(this.form)
        this.dialogVisible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    openExternalDialog() {
      this.externalDialogVisible = true
      this.externalQueryParams.pageNum = 1
      this.getExternalList()
    },
    handleUseExternal(row) {
      getExternalCompanyImportPreview(row.custId).then(res => {
        if (!res) return
        const preview = res.data || {}
        if (preview.existingCompanyId) {
          this.externalDialogVisible = false
          this.$confirm(`公司编码 ${preview.companyCode || '-'} 已存在，是否打开编辑页？`, '提示', { type: 'warning' }).then(() => {
            this.openEditDialog(preview.existingCompanyId)
          }).catch(() => {})
          return
        }
        if (!preview.canImport) {
          this.$message.warning(preview.importDisabledReason || '当前记录不可导入')
          return
        }
        this.externalDialogVisible = false
        this.handleAdd({
          companyName: preview.companyName || '',
          companyShortName: preview.companyShortName || '',
          companyCode: preview.companyCode || '',
          adminUsername: preview.adminUsername || preview.companyCode || '',
          typeCode: preview.typeCode || '',
          contactName: preview.contactName || '',
          contactPhone: preview.contactPhone || '',
          servicePhone: preview.servicePhone || '',
          provinceName: preview.provinceName || '',
          cityName: preview.cityName || '',
          districtName: preview.districtName || '',
          address: preview.address || '',
          sourceType: preview.sourceType || 'CRM',
          status: preview.status == null ? 1 : preview.status
        })
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
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除公司“${row.companyName}”吗？`, '提示', { type: 'warning' }).then(() => {
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
.app-container {
  padding: 0;
}

.search-card {
  margin-bottom: 0;
}

.table-toolbar {
  margin-bottom: 12px;
}
</style>
