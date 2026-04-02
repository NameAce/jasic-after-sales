<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="归属总部" prop="companyId">
          <el-select v-model="queryParams.companyId" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in companyOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物料编码" prop="productCode">
          <el-input v-model="queryParams.productCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="queryParams.productModel" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDesc">
          <el-input v-model="queryParams.faultDesc" placeholder="请输入" clearable />
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
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="small"
          v-hasPerms="['system:faultRepairConfig:add']"
          @click="handleAdd"
        >
          新增
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="归属总部" prop="companyName" min-width="180" />
        <el-table-column label="物料编码" prop="productCode" min-width="140" />
        <el-table-column label="产品型号" prop="productModel" min-width="140" />
        <el-table-column label="故障描述" prop="faultDescSummary" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              v-hasPerms="['system:faultRepairConfig:list']"
              @click="handleDetail(row)"
            >
              查看
            </el-button>
            <el-button
              type="text"
              size="mini"
              v-hasPerms="['system:faultRepairConfig:update']"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              v-hasPerms="['system:faultRepairConfig:remove']"
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="860px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="归属总部" prop="companyId">
              <el-select v-model="form.companyId" placeholder="请选择归属总部" filterable style="width: 100%;">
                <el-option
                  v-for="item in companyOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料编码">
              <el-input v-model="form.productCode" placeholder="请输入物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品型号">
              <el-input v-model="form.productModel" placeholder="请输入产品型号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-header">
          <span>故障信息</span>
          <el-button type="text" @click="addFaultItem">新增故障信息</el-button>
        </div>

        <div v-for="(item, index) in form.faults" :key="index" class="fault-block">
          <div class="fault-block__toolbar">
            <span>故障 {{ index + 1 }}</span>
            <el-button
              v-if="form.faults.length > 1"
              type="text"
              style="color: #F56C6C;"
              @click="removeFaultItem(index)"
            >
              删除
            </el-button>
          </div>
          <el-form-item label="故障描述" :label-width="'96px'">
            <el-input v-model="item.faultDesc" placeholder="请输入故障描述" />
          </el-form-item>
          <div class="section-header section-header--mini">
            <span>维修说明</span>
            <el-button type="text" @click="addRepairOption(item)">新增维修说明</el-button>
          </div>
          <div
            v-for="(repairDesc, repairIndex) in item.repairOptions"
            :key="repairIndex"
            class="repair-option-row"
          >
            <el-input
              v-model="item.repairOptions[repairIndex]"
              placeholder="请输入维修说明"
            />
            <el-button
              v-if="item.repairOptions.length > 1"
              type="text"
              style="color: #F56C6C; margin-left: 12px;"
              @click="removeRepairOption(item, repairIndex)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="查看故障与维修配置" :visible.sync="detailVisible" width="860px" append-to-body>
      <div v-if="detail" class="detail-wrapper">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="归属总部">{{ detail.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '停用' }}</el-descriptions-item>
          <el-descriptions-item label="物料编码">{{ detail.productCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="产品型号">{{ detail.productModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-header" style="margin-top: 16px;">
          <span>故障信息</span>
        </div>
        <div v-for="(item, index) in detail.faults || []" :key="index" class="fault-detail-block">
          <div class="fault-detail-block__title">{{ item.faultDesc }}</div>
          <div class="fault-detail-block__options">
            <el-tag v-for="(repairDesc, repairIndex) in item.repairOptions || []" :key="repairIndex" size="mini">
              {{ repairDesc }}
            </el-tag>
          </div>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addFaultRepairConfig,
  deleteFaultRepairConfig,
  getFaultRepairConfig,
  listFaultRepairConfig,
  listFaultRepairConfigCompanyOptions,
  updateFaultRepairConfig
} from '@/api/system'

function createFaultItem() {
  return {
    faultDesc: '',
    repairOptions: ['']
  }
}

function buildDefaultQuery() {
  return {
    pageNum: 1,
    pageSize: 10,
    companyId: undefined,
    productCode: '',
    productModel: '',
    faultDesc: '',
    status: undefined
  }
}

function buildDefaultForm() {
  return {
    id: undefined,
    companyId: undefined,
    productCode: '',
    productModel: '',
    status: 1,
    remark: '',
    faults: [createFaultItem()]
  }
}

export default {
  name: 'FaultRepairConfigManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      tableData: [],
      total: 0,
      companyOptions: [],
      queryParams: buildDefaultQuery(),
      dialogVisible: false,
      dialogTitle: '新增故障与维修配置',
      form: buildDefaultForm(),
      rules: {
        companyId: [{ required: true, message: '请选择归属总部', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      detailVisible: false,
      detail: null
    }
  },
  created() {
    this.loadCompanyOptions()
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listFaultRepairConfig(this.queryParams).then(res => {
        const page = res.data || {}
        this.tableData = page.records || []
        this.total = Number(page.total || 0)
      }).finally(() => {
        this.loading = false
      })
    },
    loadCompanyOptions() {
      listFaultRepairConfigCompanyOptions().then(res => {
        this.companyOptions = res.data || []
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = buildDefaultQuery()
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增故障与维修配置'
      this.form = buildDefaultForm()
      this.dialogVisible = true
    },
    handleEdit(row) {
      getFaultRepairConfig(row.id).then(res => {
        this.dialogTitle = '编辑故障与维修配置'
        this.form = this.normalizeForm(res.data)
        this.dialogVisible = true
      })
    },
    handleDetail(row) {
      getFaultRepairConfig(row.id).then(res => {
        this.detail = this.normalizeForm(res.data)
        this.detailVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除当前配置？', '提示', { type: 'warning' }).then(() => {
        return deleteFaultRepairConfig(row.id)
      }).then(res => {
        if (!res) {
          return
        }
        this.$message.success('删除成功')
        this.getList()
      }).catch(() => {})
    },
    normalizeForm(data) {
      const form = buildDefaultForm()
      if (!data) {
        return form
      }
      return {
        id: data.id,
        companyId: data.companyId,
        productCode: data.productCode || '',
        productModel: data.productModel || '',
        status: typeof data.status === 'number' ? data.status : 1,
        remark: data.remark || '',
        faults: (data.faults && data.faults.length
          ? data.faults
          : [createFaultItem()]
        ).map(item => ({
          faultDesc: item.faultDesc || '',
          repairOptions: item.repairOptions && item.repairOptions.length ? item.repairOptions.slice() : ['']
        }))
      }
    },
    addFaultItem() {
      this.form.faults.push(createFaultItem())
    },
    removeFaultItem(index) {
      this.form.faults.splice(index, 1)
    },
    addRepairOption(item) {
      item.repairOptions.push('')
    },
    removeRepairOption(item, index) {
      item.repairOptions.splice(index, 1)
    },
    validateDynamicForm() {
      if (!this.form.productCode && !this.form.productModel) {
        this.$message.error('物料编码和产品型号不能同时为空')
        return false
      }
      const faults = this.form.faults || []
      if (!faults.length) {
        this.$message.error('请至少添加一条故障信息')
        return false
      }
      const faultSet = new Set()
      for (const item of faults) {
        const faultDesc = (item.faultDesc || '').trim()
        if (!faultDesc) {
          this.$message.error('故障描述不能为空')
          return false
        }
        if (faultSet.has(faultDesc)) {
          this.$message.error('同一配置下故障描述不能重复')
          return false
        }
        faultSet.add(faultDesc)
        const repairOptions = (item.repairOptions || []).map(option => (option || '').trim()).filter(Boolean)
        if (!repairOptions.length) {
          this.$message.error('维修说明不能为空')
          return false
        }
        if (new Set(repairOptions).size !== repairOptions.length) {
          this.$message.error('同一故障下维修说明不能重复')
          return false
        }
      }
      return true
    },
    buildSubmitPayload() {
      return {
        id: this.form.id,
        companyId: this.form.companyId,
        productCode: this.form.productCode ? this.form.productCode.trim() : '',
        productModel: this.form.productModel ? this.form.productModel.trim() : '',
        status: this.form.status,
        remark: this.form.remark ? this.form.remark.trim() : '',
        faults: (this.form.faults || []).map(item => ({
          faultDesc: item.faultDesc ? item.faultDesc.trim() : '',
          repairOptions: (item.repairOptions || []).map(option => (option || '').trim()).filter(Boolean)
        }))
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid || !this.validateDynamicForm()) {
          return
        }
        const request = this.form.id ? updateFaultRepairConfig : addFaultRepairConfig
        this.submitLoading = true
        request(this.buildSubmitPayload()).then(res => {
          if (!res) {
            return
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => {
          this.submitLoading = false
        })
      })
    }
  }
}
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.section-header--mini {
  margin-top: 4px;
}

.fault-block {
  padding: 16px 16px 4px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.fault-block__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 500;
}

.repair-option-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.detail-wrapper {
  min-height: 120px;
}

.fault-detail-block + .fault-detail-block {
  margin-top: 12px;
}

.fault-detail-block__title {
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
}

.fault-detail-block__options .el-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}
</style>
