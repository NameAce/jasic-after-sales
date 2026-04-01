<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="queryParams.barcode" placeholder="请输入条码" clearable />
        </el-form-item>
        <el-form-item label="总部" prop="hqCompanyId">
          <el-select v-model="queryParams.hqCompanyId" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in hqOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物料编码" prop="productCode">
          <el-input v-model="queryParams.productCode" placeholder="请输入物料编码" clearable />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="queryParams.productModel" placeholder="请输入产品型号" clearable />
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
          v-hasPerms="['system:machineBarcode:add']"
          @click="handleAdd"
        >
          新增
        </el-button>
        <el-button
          icon="el-icon-upload2"
          size="small"
          v-hasPerms="['system:machineBarcode:import']"
          @click="handleOpenImport"
        >
          JSON导入
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="条码" prop="barcode" min-width="180" />
        <el-table-column label="归属总部" prop="hqCompanyName" min-width="180" />
        <el-table-column label="物料编码" prop="productCode" min-width="120" />
        <el-table-column label="产品型号" prop="productModel" min-width="140" />
        <el-table-column label="品牌编码" prop="brandCode" width="110" />
        <el-table-column label="质保状态" prop="warrantyStatus" width="120" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updateTime" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              v-hasPerms="['system:machineBarcode:update']"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              v-hasPerms="['system:machineBarcode:remove']"
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="机器条码" prop="barcode">
          <el-input v-model="form.barcode" placeholder="请输入机器条码" />
        </el-form-item>
        <el-form-item label="归属总部" prop="hqCompanyId">
          <el-select v-model="form.hqCompanyId" placeholder="请选择归属总部" filterable style="width: 100%;">
            <el-option
              v-for="item in hqOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物料编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入物料编码" />
        </el-form-item>
        <el-form-item label="产品型号" prop="productModel">
          <el-input v-model="form.productModel" placeholder="请输入产品型号" />
        </el-form-item>
        <el-form-item label="品牌编码" prop="brandCode">
          <el-input v-model="form.brandCode" placeholder="请输入品牌编码" />
        </el-form-item>
        <el-form-item label="质保状态" prop="warrantyStatus">
          <el-input v-model="form.warrantyStatus" placeholder="例如 IN_WARRANTY" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="JSON批量导入" :visible.sync="importDialogVisible" width="760px" append-to-body>
      <div class="import-tip">
        按数组提交，支持按条码覆盖更新已有档案。示例见下方“填充示例”。
      </div>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="16"
        resize="none"
        placeholder='请输入 JSON 数组，例如 [{"barcode":"JASIC-001","hqCompanyId":2,"productCode":"P-100","status":1}]'
      />
      <div slot="footer">
        <el-button @click="fillImportExample">填充示例</el-button>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">开始导入</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listMachineBarcode,
  getMachineBarcode,
  listMachineBarcodeHqOptions,
  addMachineBarcode,
  updateMachineBarcode,
  deleteMachineBarcode,
  importMachineBarcode
} from '@/api/system'

export default {
  name: 'MachineBarcodeManage',
  data() {
    return {
      loading: false,
      total: 0,
      tableData: [],
      hqOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        barcode: '',
        hqCompanyId: undefined,
        productCode: '',
        productModel: '',
        status: undefined
      },
      dialogVisible: false,
      dialogTitle: '',
      submitLoading: false,
      form: {},
      importDialogVisible: false,
      importText: '',
      importLoading: false,
      rules: {
        barcode: [{ required: true, message: '请输入机器条码', trigger: 'blur' }],
        hqCompanyId: [{ required: true, message: '请选择归属总部', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadHqOptions()
    this.getList()
  },
  methods: {
    loadHqOptions() {
      listMachineBarcodeHqOptions().then(res => {
        if (!res) return
        this.hqOptions = res.data || []
      })
    },
    getList() {
      this.loading = true
      listMachineBarcode(this.queryParams).then(res => {
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
        barcode: '',
        hqCompanyId: undefined,
        productCode: '',
        productModel: '',
        status: undefined
      }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增条码档案'
      this.form = {
        barcode: '',
        hqCompanyId: undefined,
        productCode: '',
        productModel: '',
        brandCode: '',
        warrantyStatus: '',
        status: 1,
        remark: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑条码档案'
      getMachineBarcode(row.id).then(res => {
        if (!res) return
        this.form = Object.assign({ status: 1 }, res.data)
        this.dialogVisible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateMachineBarcode : addMachineBarcode
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除条码档案 "${row.barcode}" 吗？`, '提示', { type: 'warning' }).then(() => {
        deleteMachineBarcode(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleOpenImport() {
      this.importText = ''
      this.importDialogVisible = true
    },
    fillImportExample() {
      const exampleCompanyId = this.hqOptions.length > 0 ? this.hqOptions[0].id : 1
      this.importText = JSON.stringify([
        {
          barcode: 'JASIC-001',
          hqCompanyId: exampleCompanyId,
          productCode: 'P-100',
          productModel: 'MODEL-A',
          brandCode: 'JASIC',
          warrantyStatus: 'IN_WARRANTY',
          status: 1,
          remark: '初始导入'
        }
      ], null, 2)
    },
    submitImport() {
      if (!this.importText) {
        this.$message.warning('请输入导入 JSON')
        return
      }
      let payload = null
      try {
        payload = JSON.parse(this.importText)
      } catch (error) {
        this.$message.error('JSON 格式不正确')
        return
      }
      if (!Array.isArray(payload)) {
        this.$message.error('导入内容必须是 JSON 数组')
        return
      }
      this.importLoading = true
      importMachineBarcode(payload).then(res => {
        if (!res) return
        this.$message.success(`导入成功，共处理 ${res.data} 条`)
        this.importDialogVisible = false
        this.getList()
      }).finally(() => { this.importLoading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
.import-tip {
  margin-bottom: 12px;
  color: #606266;
  line-height: 1.6;
}
</style>
