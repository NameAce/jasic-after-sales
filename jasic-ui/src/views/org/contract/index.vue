<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
      <el-tab-pane label="总部-一级签约" name="hqFirst" />
      <el-tab-pane label="一级-二级关系" name="firstSecond" />
    </el-tabs>

    <el-card v-if="activeTab === 'hqFirst'" shadow="never">
      <el-form :model="hqQuery" :inline="true" size="small" style="margin-bottom: 12px;">
        <el-form-item label="总部公司">
          <el-select v-model="hqQuery.hqCompanyId" placeholder="全部" clearable>
            <el-option v-for="c in hqOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="getHqFirstList">搜索</el-button>
        </el-form-item>
      </el-form>
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['org:contract:add']" @click="handleAddHqFirst">新增签约</el-button>
        <el-button type="success" icon="el-icon-download" size="small" v-hasPerms="['org:contract:add']" @click="openCrmImportDialog">从CRM导入</el-button>
      </div>
      <el-table v-loading="hqLoading" :data="hqFirstList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="总部公司" prop="hqCompanyName" width="200" />
        <el-table-column label="一级网点" prop="firstCompanyName" width="200" />
        <el-table-column label="大区" prop="regionName" width="140" />
        <el-table-column label="签约时间" prop="contractTime" width="160" />
        <el-table-column label="状态" prop="status" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['org:contract:update']" @click="handleEditHqFirst(row)">编辑</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['org:contract:remove']" @click="handleDeleteHqFirst(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="hqQuery.pageNum"
        :page-size="hqQuery.pageSize"
        :total="hqTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="val => { hqQuery.pageSize = val; getHqFirstList() }"
        @current-change="val => { hqQuery.pageNum = val; getHqFirstList() }"
      />
    </el-card>

    <el-card v-if="activeTab === 'firstSecond'" shadow="never">
      <el-form :model="fsQuery" :inline="true" size="small" style="margin-bottom: 12px;">
        <el-form-item label="一级网点">
          <el-select v-model="fsQuery.firstCompanyId" placeholder="全部" clearable filterable>
            <el-option v-for="c in firstOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="getFirstSecondList">搜索</el-button>
        </el-form-item>
      </el-form>
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['org:contract:add']" @click="handleAddFirstSecond">新增关系</el-button>
      </div>
      <el-table v-loading="fsLoading" :data="firstSecondList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="一级网点" prop="firstCompanyName" width="200" />
        <el-table-column label="二级网点" prop="secondCompanyName" width="200" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['org:contract:remove']" @click="handleDeleteFirstSecond(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="fsQuery.pageNum"
        :page-size="fsQuery.pageSize"
        :total="fsTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="val => { fsQuery.pageSize = val; getFirstSecondList() }"
        @current-change="val => { fsQuery.pageNum = val; getFirstSecondList() }"
      />
    </el-card>

    <el-dialog :title="hqDialogTitle" :visible.sync="hqDialogVisible" width="500px" append-to-body>
      <el-form ref="hqForm" :model="hqForm" :rules="hqFormRules" label-width="90px">
        <el-form-item label="总部公司" prop="hqCompanyId">
          <el-select v-model="hqForm.hqCompanyId" placeholder="请选择" filterable style="width: 100%;">
            <el-option v-for="c in hqOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="一级网点" prop="firstCompanyId">
          <el-select v-model="hqForm.firstCompanyId" placeholder="请选择" filterable style="width: 100%;">
            <el-option v-for="c in firstOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属大区" prop="regionId">
          <el-select v-model="hqForm.regionId" placeholder="请选择" clearable style="width: 100%;">
            <el-option v-for="r in regionOptions" :key="r.id" :label="r.regionName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="签约时间" prop="contractTime">
          <el-date-picker v-model="hqForm.contractTime" type="date" value-format="yyyy-MM-dd" placeholder="请选择" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="hqForm.status">
            <el-radio :label="1">有效</el-radio>
            <el-radio :label="0">失效</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="hqDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="hqSubmitLoading" @click="submitHqForm">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="从CRM导入签约"
      :visible.sync="crmImportDialogVisible"
      width="1180px"
      append-to-body
      @close="handleCrmImportDialogClose"
    >
      <el-form :model="crmImportQuery" :inline="true" size="small" class="crm-import-filter">
        <el-form-item label="总部公司">
          <el-select
            v-model="crmImportQuery.hqCompanyId"
            placeholder="请选择总部公司"
            clearable
            filterable
            @change="handleCrmImportHqChange"
          >
            <el-option v-for="c in hqOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="一级公司">
          <el-select v-model="crmImportQuery.firstCompanyId" placeholder="全部" clearable filterable>
            <el-option v-for="c in firstOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="大区">
          <el-select v-model="crmImportQuery.regionId" placeholder="全部" clearable filterable>
            <el-option v-for="r in crmImportRegionOptions" :key="r.id" :label="r.regionName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="crmImportQuery.showAbnormal">查看异常数据</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleCrmImportSearch">搜索</el-button>
          <el-button @click="resetCrmImportQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table
        ref="crmImportTable"
        v-loading="crmImportLoading"
        :data="crmImportList"
        border
        stripe
        row-key="id"
        @selection-change="handleCrmImportSelectionChange"
      >
        <el-table-column type="selection" width="55" :selectable="row => row.canImport" />
        <el-table-column label="客户编码" prop="kunnr" width="120" />
        <el-table-column label="CRM企业名称" prop="crmCompanyName" min-width="180" show-overflow-tooltip />
        <el-table-column label="销售组织" prop="salesOrg" width="120" />
        <el-table-column label="CRM大区" min-width="160">
          <template slot-scope="{ row }">
            <span>{{ row.regionName || '-' }}</span>
            <span v-if="row.regionCode">（{{ row.regionCode }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="一级公司" prop="firstCompanyName" min-width="160" show-overflow-tooltip />
        <el-table-column label="本地大区" prop="localRegionName" min-width="150" show-overflow-tooltip />
        <el-table-column label="CRM状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.aliveFlag === 1 ? 'success' : 'info'">
              {{ row.aliveFlag === 1 ? '有效' : '失效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="导入状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.canImport" size="mini" type="success">可导入</el-tag>
            <el-tag v-else-if="row.existingContract" size="mini">已存在</el-tag>
            <el-tag v-else size="mini" type="warning">异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="matchRemark" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="crmImportQuery.pageNum"
        :page-size="crmImportQuery.pageSize"
        :total="crmImportTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleCrmImportSizeChange"
        @current-change="handleCrmImportCurrentChange"
      />
      <div slot="footer">
        <el-button @click="crmImportDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="crmImportSubmitLoading" @click="submitCrmImport">导 入</el-button>
      </div>
    </el-dialog>

    <el-dialog title="新增一级-二级关系" :visible.sync="fsDialogVisible" width="500px" append-to-body>
      <el-form ref="fsForm" :model="fsForm" :rules="fsFormRules" label-width="90px">
        <el-form-item label="一级网点" prop="firstCompanyId">
          <el-select v-model="fsForm.firstCompanyId" placeholder="请选择" filterable style="width: 100%;">
            <el-option v-for="c in firstOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="二级网点" prop="secondCompanyId">
          <el-select v-model="fsForm.secondCompanyId" placeholder="请选择" filterable style="width: 100%;">
            <el-option v-for="c in secondOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="fsDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="fsSubmitLoading" @click="submitFsForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listHqFirstContract,
  addHqFirstContract,
  updateHqFirstContract,
  deleteHqFirstContract,
  listCrmHqFirstContractImport,
  importCrmHqFirstContract,
  listFirstSecondRelation,
  addFirstSecondRelation,
  deleteFirstSecondRelation,
  listCompany
} from '@/api/org'
import { listRegion } from '@/api/system'

export default {
  name: 'ContractManage',
  data() {
    return {
      activeTab: 'hqFirst',
      hqLoading: false,
      hqFirstList: [],
      hqTotal: 0,
      hqQuery: { pageNum: 1, pageSize: 10, hqCompanyId: undefined },
      fsLoading: false,
      firstSecondList: [],
      fsTotal: 0,
      fsQuery: { pageNum: 1, pageSize: 10, firstCompanyId: undefined },
      hqOptions: [],
      firstOptions: [],
      secondOptions: [],
      regionOptions: [],
      hqDialogVisible: false,
      hqDialogTitle: '新增签约',
      hqForm: {},
      hqSubmitLoading: false,
      hqFormRules: {
        hqCompanyId: [{ required: true, message: '请选择总部公司', trigger: 'change' }],
        firstCompanyId: [{ required: true, message: '请选择一级网点', trigger: 'change' }]
      },
      crmImportDialogVisible: false,
      crmImportLoading: false,
      crmImportSubmitLoading: false,
      crmImportList: [],
      crmImportTotal: 0,
      crmImportSelection: [],
      crmImportRegionOptions: [],
      crmImportQuery: {
        pageNum: 1,
        pageSize: 10,
        hqCompanyId: undefined,
        firstCompanyId: undefined,
        regionId: undefined,
        showAbnormal: false
      },
      fsDialogVisible: false,
      fsForm: {},
      fsSubmitLoading: false,
      fsFormRules: {
        firstCompanyId: [{ required: true, message: '请选择一级网点', trigger: 'change' }],
        secondCompanyId: [{ required: true, message: '请选择二级网点', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadCompanyOptions()
    this.getHqFirstList()
  },
  methods: {
    loadCompanyOptions() {
      const baseParams = { pageNum: 1, pageSize: 999 }
      Promise.all([
        listCompany({ ...baseParams, category: 'HQ' }),
        listCompany({ ...baseParams, category: 'FIRST_LEVEL' }),
        listCompany({ ...baseParams, category: 'SECOND_LEVEL' })
      ]).then(([hqRes, firstRes, secondRes]) => {
        if (hqRes) this.hqOptions = hqRes.data.records || []
        if (firstRes) this.firstOptions = firstRes.data.records || []
        if (secondRes) this.secondOptions = secondRes.data.records || []
      })
    },
    handleTabClick() {
      if (this.activeTab === 'hqFirst') this.getHqFirstList()
      else this.getFirstSecondList()
    },
    getHqFirstList() {
      this.hqLoading = true
      listHqFirstContract(this.hqQuery).then(res => {
        if (!res) return
        this.hqFirstList = res.data.records
        this.hqTotal = res.data.total
      }).finally(() => { this.hqLoading = false })
    },
    getFirstSecondList() {
      this.fsLoading = true
      listFirstSecondRelation(this.fsQuery).then(res => {
        if (!res) return
        this.firstSecondList = res.data.records
        this.fsTotal = res.data.total
      }).finally(() => { this.fsLoading = false })
    },
    handleAddHqFirst() {
      this.hqDialogTitle = '新增签约'
      this.hqForm = { hqCompanyId: this.hqQuery.hqCompanyId, status: 1 }
      this.hqDialogVisible = true
      this.$nextTick(() => this.$refs.hqForm && this.$refs.hqForm.clearValidate())
    },
    handleEditHqFirst(row) {
      this.hqDialogTitle = '编辑签约'
      this.hqForm = { ...row }
      if (row.hqCompanyId) {
        listRegion(row.hqCompanyId).then(res => { if (res) this.regionOptions = res.data || [] })
      }
      this.hqDialogVisible = true
    },
    submitHqForm() {
      this.$refs.hqForm.validate(valid => {
        if (!valid) return
        this.hqSubmitLoading = true
        const api = this.hqForm.id ? updateHqFirstContract : addHqFirstContract
        api(this.hqForm).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.hqDialogVisible = false
          this.getHqFirstList()
        }).finally(() => { this.hqSubmitLoading = false })
      })
    },
    handleDeleteHqFirst(row) {
      this.$confirm('确认删除该签约关系？', '提示', { type: 'warning' }).then(() => {
        deleteHqFirstContract(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getHqFirstList()
        })
      }).catch(() => {})
    },
    openCrmImportDialog() {
      this.crmImportQuery = {
        pageNum: 1,
        pageSize: 10,
        hqCompanyId: this.hqQuery.hqCompanyId,
        firstCompanyId: undefined,
        regionId: undefined,
        showAbnormal: false
      }
      this.crmImportDialogVisible = true
      this.crmImportList = []
      this.crmImportTotal = 0
      this.crmImportRegionOptions = []
      this.resetCrmImportSelection()
      if (this.crmImportQuery.hqCompanyId) {
        this.loadCrmImportRegions(this.crmImportQuery.hqCompanyId)
        this.getCrmImportList()
      }
    },
    getCrmImportList() {
      if (!this.crmImportQuery.hqCompanyId) {
        this.crmImportList = []
        this.crmImportTotal = 0
        this.resetCrmImportSelection()
        return
      }
      this.crmImportLoading = true
      listCrmHqFirstContractImport(this.crmImportQuery).then(res => {
        if (!res) return
        this.crmImportList = res.data.records || []
        this.crmImportTotal = res.data.total || 0
        this.resetCrmImportSelection()
      }).catch(() => {
        this.crmImportList = []
        this.crmImportTotal = 0
        this.resetCrmImportSelection()
      }).finally(() => {
        this.crmImportLoading = false
      })
    },
    handleCrmImportSearch() {
      if (!this.crmImportQuery.hqCompanyId) {
        this.$message.warning('请选择总部公司')
        return
      }
      this.crmImportQuery.pageNum = 1
      this.getCrmImportList()
    },
    resetCrmImportQuery() {
      const hqCompanyId = this.crmImportQuery.hqCompanyId
      this.crmImportQuery = {
        pageNum: 1,
        pageSize: 10,
        hqCompanyId,
        firstCompanyId: undefined,
        regionId: undefined,
        showAbnormal: false
      }
      if (hqCompanyId) {
        this.loadCrmImportRegions(hqCompanyId)
        this.getCrmImportList()
      } else {
        this.crmImportRegionOptions = []
        this.crmImportList = []
        this.crmImportTotal = 0
        this.resetCrmImportSelection()
      }
    },
    handleCrmImportHqChange(val) {
      this.crmImportQuery.pageNum = 1
      this.crmImportQuery.regionId = undefined
      this.resetCrmImportSelection()
      if (val) {
        this.loadCrmImportRegions(val)
        this.getCrmImportList()
      } else {
        this.crmImportRegionOptions = []
        this.crmImportList = []
        this.crmImportTotal = 0
      }
    },
    loadCrmImportRegions(hqCompanyId) {
      if (!hqCompanyId) {
        this.crmImportRegionOptions = []
        return
      }
      listRegion(hqCompanyId).then(res => {
        if (!res) return
        this.crmImportRegionOptions = res.data || []
      })
    },
    handleCrmImportSelectionChange(rows) {
      this.crmImportSelection = rows || []
    },
    handleCrmImportSizeChange(val) {
      this.crmImportQuery.pageNum = 1
      this.crmImportQuery.pageSize = val
      this.getCrmImportList()
    },
    handleCrmImportCurrentChange(val) {
      this.crmImportQuery.pageNum = val
      this.getCrmImportList()
    },
    resetCrmImportSelection() {
      this.crmImportSelection = []
      this.$nextTick(() => {
        if (this.$refs.crmImportTable) {
          this.$refs.crmImportTable.clearSelection()
        }
      })
    },
    submitCrmImport() {
      if (!this.crmImportSelection.length) {
        this.$message.warning('请选择要导入的 CRM 签约关系')
        return
      }
      this.crmImportSubmitLoading = true
      importCrmHqFirstContract({
        hqCompanyId: this.crmImportQuery.hqCompanyId,
        snapshotIds: this.crmImportSelection.map(item => item.id)
      }).then(res => {
        if (!res) return
        const data = res.data || {}
        this.$message.success(`成功 ${data.successCount || 0} 条，已存在跳过 ${data.existedCount || 0} 条，映射失败 ${data.failedCount || 0} 条`)
        this.getCrmImportList()
        this.getHqFirstList()
      }).finally(() => {
        this.crmImportSubmitLoading = false
      })
    },
    handleCrmImportDialogClose() {
      this.crmImportRegionOptions = []
      this.resetCrmImportSelection()
    },
    handleAddFirstSecond() {
      this.fsForm = {}
      this.fsDialogVisible = true
      this.$nextTick(() => this.$refs.fsForm && this.$refs.fsForm.clearValidate())
    },
    submitFsForm() {
      this.$refs.fsForm.validate(valid => {
        if (!valid) return
        this.fsSubmitLoading = true
        addFirstSecondRelation(this.fsForm).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.fsDialogVisible = false
          this.getFirstSecondList()
        }).finally(() => { this.fsSubmitLoading = false })
      })
    },
    handleDeleteFirstSecond(row) {
      this.$confirm('确认删除该关系？', '提示', { type: 'warning' }).then(() => {
        deleteFirstSecondRelation(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getFirstSecondList()
        })
      }).catch(() => {})
    }
  },
  watch: {
    'hqForm.hqCompanyId'(val) {
      if (val) {
        listRegion(val).then(res => { if (res) this.regionOptions = res.data || [] })
      } else {
        this.regionOptions = []
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.table-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
.crm-import-toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.crm-import-filter {
  margin-bottom: 12px;
}
.crm-import-toolbar__text {
  color: #606266;
}
.crm-import-toolbar__text span {
  color: #303133;
  font-weight: 500;
}
</style>
