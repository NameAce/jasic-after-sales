<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
      <el-tab-pane label="总部-一级签约" name="hqFirst" />
      <el-tab-pane label="一级-二级关系" name="firstSecond" />
    </el-tabs>

    <!-- 总部-一级签约 -->
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

    <!-- 一级-二级关系 -->
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

    <!-- 新增总部-一级弹窗 -->
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

    <!-- 新增一级-二级弹窗 -->
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
  listHqFirstContract, addHqFirstContract, updateHqFirstContract, deleteHqFirstContract,
  listFirstSecondRelation, addFirstSecondRelation, deleteFirstSecondRelation,
  listCompany, listCompanyType
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
      typeCodeMap: {},
      hqDialogVisible: false,
      hqDialogTitle: '新增签约',
      hqForm: {},
      hqSubmitLoading: false,
      hqFormRules: {
        hqCompanyId: [{ required: true, message: '请选择总部公司', trigger: 'change' }],
        firstCompanyId: [{ required: true, message: '请选择一级网点', trigger: 'change' }]
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
      listCompanyType().then(res => {
        if (!res) return
        (res.data || []).forEach(t => { this.typeCodeMap[t.typeCode] = t.subjectType })
      })
      listCompany({ pageNum: 1, pageSize: 999 }).then(res => {
        if (!res) return
        const all = res.data.records || []
        this.hqOptions = all.filter(c => (this.typeCodeMap[c.typeCode] || '').includes('HQ'))
        this.firstOptions = all.filter(c => c.typeCode === 'FIRST')
        this.secondOptions = all.filter(c => c.typeCode === 'SECOND')
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
      this.hqForm = { status: 1 }
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
.table-toolbar { margin-bottom: 12px; }
</style>
