<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
      <el-tab-pane label="总部-一级签约" name="hqFirst" />
      <el-tab-pane label="一级-二级关系" name="firstSecond" />
    </el-tabs>

    <el-card v-if="activeTab === 'hqFirst'" shadow="never">
      <el-form :model="hqQuery" :inline="true" size="small" style="margin-bottom: 12px;">
        <el-form-item label="总部公司">
          <el-select v-model="hqQuery.hqCompanyId" placeholder="全部" clearable filterable>
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
        @size-change="handleHqSizeChange"
        @current-change="handleHqCurrentChange"
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
        <el-button type="success" icon="el-icon-download" size="small" v-hasPerms="['org:contract:add']" @click="openFsCrmImportDialog">从来源导入</el-button>
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
        @size-change="handleFsSizeChange"
        @current-change="handleFsCurrentChange"
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
        <el-button @click="hqDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="hqSubmitLoading" @click="submitHqForm">确定</el-button>
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
        <el-form-item label="客户编码">
          <el-input
            v-model.trim="crmImportQuery.kunnr"
            placeholder="请输入客户编码"
            clearable
            @keyup.enter.native="handleCrmImportSearch"
          />
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
        <el-button @click="crmImportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="crmImportSubmitLoading" @click="submitCrmImport">导入</el-button>
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
        <el-button @click="fsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fsSubmitLoading" @click="submitFsForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="从来源导入一级二级关系"
      :visible.sync="fsCrmImportDialogVisible"
      width="1180px"
      append-to-body
      @close="handleFsCrmImportDialogClose"
    >
      <el-form :model="fsCrmImportQuery" :inline="true" size="small" class="crm-import-filter">
        <el-form-item label="一级编码">
          <el-input
            v-model.trim="fsCrmImportQuery.firstCompanyCode"
            placeholder="请输入一级公司编码"
            clearable
            @keyup.enter.native="handleFsCrmImportSearch"
          />
        </el-form-item>
        <el-form-item label="二级编码">
          <el-input
            v-model.trim="fsCrmImportQuery.secondCompanyCode"
            placeholder="请输入二级公司编码"
            clearable
            @keyup.enter.native="handleFsCrmImportSearch"
          />
        </el-form-item>
        <el-form-item label="本地一级公司">
          <el-select v-model="fsCrmImportQuery.firstCompanyId" placeholder="全部" clearable filterable>
            <el-option v-for="c in firstOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="本地二级公司">
          <el-select v-model="fsCrmImportQuery.secondCompanyId" placeholder="全部" clearable filterable>
            <el-option v-for="c in secondOptions" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="fsCrmImportQuery.showAbnormal">查看异常数据</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleFsCrmImportSearch">搜索</el-button>
          <el-button @click="resetFsCrmImportQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table
        ref="fsCrmImportTable"
        v-loading="fsCrmImportLoading"
        :data="fsCrmImportList"
        border
        stripe
        row-key="id"
        @selection-change="handleFsCrmImportSelectionChange"
      >
        <el-table-column type="selection" width="55" :selectable="row => row.canImport" />
        <el-table-column label="一级CRM ID" prop="firstCustId" width="110" />
        <el-table-column label="一级编码" prop="firstCompanyCode" width="120" />
        <el-table-column label="一级名称" prop="firstCompanyName" min-width="160" show-overflow-tooltip />
        <el-table-column label="二级CRM ID" prop="secondCustId" width="110" />
        <el-table-column label="二级编码" prop="secondCompanyCode" width="120" />
        <el-table-column label="二级名称" prop="secondCompanyName" min-width="160" show-overflow-tooltip />
        <el-table-column label="本地一级" prop="localFirstCompanyName" min-width="140" show-overflow-tooltip />
        <el-table-column label="本地二级" prop="localSecondCompanyName" min-width="140" show-overflow-tooltip />
        <el-table-column label="来源更新时间" prop="crmOperTime" width="160" />
        <el-table-column label="导入状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.canImport" size="mini" type="success">可导入</el-tag>
            <el-tag v-else-if="row.existingRelation" size="mini">已存在</el-tag>
            <el-tag v-else-if="row.conflictingRelation" size="mini" type="danger">冲突</el-tag>
            <el-tag v-else size="mini" type="warning">异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="matchRemark" min-width="220" show-overflow-tooltip />
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        :current-page="fsCrmImportQuery.pageNum"
        :page-size="fsCrmImportQuery.pageSize"
        :total="fsCrmImportTotal"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleFsCrmImportSizeChange"
        @current-change="handleFsCrmImportCurrentChange"
      />
      <div slot="footer">
        <el-button @click="fsCrmImportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fsCrmImportSubmitLoading" @click="submitFsCrmImport">导入</el-button>
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
  listCrmFirstSecondRelationImport,
  importCrmFirstSecondRelation,
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
        kunnr: '',
        showAbnormal: false
      },
      fsDialogVisible: false,
      fsForm: {},
      fsSubmitLoading: false,
      fsFormRules: {
        firstCompanyId: [{ required: true, message: '请选择一级网点', trigger: 'change' }],
        secondCompanyId: [{ required: true, message: '请选择二级网点', trigger: 'change' }]
      },
      fsCrmImportDialogVisible: false,
      fsCrmImportLoading: false,
      fsCrmImportSubmitLoading: false,
      fsCrmImportList: [],
      fsCrmImportTotal: 0,
      fsCrmImportSelection: [],
      fsCrmImportQuery: {
        pageNum: 1,
        pageSize: 10,
        firstCompanyId: undefined,
        secondCompanyId: undefined,
        firstCompanyCode: '',
        secondCompanyCode: '',
        showAbnormal: false
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
    handleHqSizeChange(val) {
      this.hqQuery.pageNum = 1
      this.hqQuery.pageSize = val
      this.getHqFirstList()
    },
    handleHqCurrentChange(val) {
      this.hqQuery.pageNum = val
      this.getHqFirstList()
    },
    handleFsSizeChange(val) {
      this.fsQuery.pageNum = 1
      this.fsQuery.pageSize = val
      this.getFirstSecondList()
    },
    handleFsCurrentChange(val) {
      this.fsQuery.pageNum = val
      this.getFirstSecondList()
    },
    getHqFirstList() {
      this.hqLoading = true
      listHqFirstContract(this.hqQuery).then(res => {
        if (!res) return
        this.hqFirstList = res.data.records || []
        this.hqTotal = res.data.total || 0
      }).finally(() => { this.hqLoading = false })
    },
    getFirstSecondList() {
      this.fsLoading = true
      listFirstSecondRelation(this.fsQuery).then(res => {
        if (!res) return
        this.firstSecondList = res.data.records || []
        this.fsTotal = res.data.total || 0
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
        listRegion(row.hqCompanyId).then(res => {
          if (res) this.regionOptions = res.data || []
        })
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
        kunnr: '',
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
        kunnr: '',
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
        this.$message.warning('请选择要导入的CRM签约关系')
        return
      }
      this.crmImportSubmitLoading = true
      importCrmHqFirstContract({
        hqCompanyId: this.crmImportQuery.hqCompanyId,
        snapshotIds: this.crmImportSelection.map(item => item.id)
      }).then(res => {
        if (!res) return
        const data = res.data || {}
        this.$message.success(`选中 ${data.selectedCount || 0} 条，成功 ${data.successCount || 0} 条，已存在 ${data.existedCount || 0} 条，失败 ${data.failedCount || 0} 条`)
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
    },
    openFsCrmImportDialog() {
      this.fsCrmImportQuery = {
        pageNum: 1,
        pageSize: 10,
        firstCompanyId: undefined,
        secondCompanyId: undefined,
        firstCompanyCode: '',
        secondCompanyCode: '',
        showAbnormal: false
      }
      this.fsCrmImportDialogVisible = true
      this.fsCrmImportList = []
      this.fsCrmImportTotal = 0
      this.resetFsCrmImportSelection()
      this.getFsCrmImportList()
    },
    getFsCrmImportList() {
      this.fsCrmImportLoading = true
      listCrmFirstSecondRelationImport(this.fsCrmImportQuery).then(res => {
        if (!res) return
        this.fsCrmImportList = res.data.records || []
        this.fsCrmImportTotal = res.data.total || 0
        this.resetFsCrmImportSelection()
      }).catch(() => {
        this.fsCrmImportList = []
        this.fsCrmImportTotal = 0
        this.resetFsCrmImportSelection()
      }).finally(() => {
        this.fsCrmImportLoading = false
      })
    },
    handleFsCrmImportSearch() {
      this.fsCrmImportQuery.pageNum = 1
      this.getFsCrmImportList()
    },
    resetFsCrmImportQuery() {
      this.fsCrmImportQuery = {
        pageNum: 1,
        pageSize: 10,
        firstCompanyId: undefined,
        secondCompanyId: undefined,
        firstCompanyCode: '',
        secondCompanyCode: '',
        showAbnormal: false
      }
      this.getFsCrmImportList()
    },
    handleFsCrmImportSelectionChange(rows) {
      this.fsCrmImportSelection = rows || []
    },
    handleFsCrmImportSizeChange(val) {
      this.fsCrmImportQuery.pageNum = 1
      this.fsCrmImportQuery.pageSize = val
      this.getFsCrmImportList()
    },
    handleFsCrmImportCurrentChange(val) {
      this.fsCrmImportQuery.pageNum = val
      this.getFsCrmImportList()
    },
    resetFsCrmImportSelection() {
      this.fsCrmImportSelection = []
      this.$nextTick(() => {
        if (this.$refs.fsCrmImportTable) {
          this.$refs.fsCrmImportTable.clearSelection()
        }
      })
    },
    submitFsCrmImport() {
      if (!this.fsCrmImportSelection.length) {
        this.$message.warning('请选择要导入的一级二级关系')
        return
      }
      this.fsCrmImportSubmitLoading = true
      importCrmFirstSecondRelation({
        snapshotIds: this.fsCrmImportSelection.map(item => item.id)
      }).then(res => {
        if (!res) return
        const data = res.data || {}
        this.$message.success(`选中 ${data.selectedCount || 0} 条，成功 ${data.successCount || 0} 条，已存在 ${data.existedCount || 0} 条，冲突 ${data.conflictCount || 0} 条，失败 ${data.failedCount || 0} 条`)
        this.getFsCrmImportList()
        this.getFirstSecondList()
      }).finally(() => {
        this.fsCrmImportSubmitLoading = false
      })
    },
    handleFsCrmImportDialogClose() {
      this.resetFsCrmImportSelection()
    }
  },
  watch: {
    'hqForm.hqCompanyId'(val) {
      if (val) {
        listRegion(val).then(res => {
          if (res) this.regionOptions = res.data || []
        })
      } else {
        this.regionOptions = []
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
}

.table-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.crm-import-filter {
  margin-bottom: 12px;
}
</style>
