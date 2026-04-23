<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="queryParams.barcode" placeholder="请输入条码" clearable />
        </el-form-item>
        <el-form-item label="发货单号" prop="deliverNumber">
          <el-input v-model="queryParams.deliverNumber" placeholder="请输入发货单号" clearable />
        </el-form-item>
        <el-form-item label="物料编码" prop="productCode">
          <el-input v-model="queryParams.productCode" placeholder="请输入物料编码" clearable />
        </el-form-item>
        <el-form-item label="机器小号" prop="machineNo">
          <el-input v-model="queryParams.machineNo" placeholder="请输入机器小号" clearable />
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
          type="warning"
          icon="el-icon-refresh"
          size="small"
          :loading="syncLoading"
          v-hasPerms="['system:machineBarcode:sync']"
          @click="handleFullSync"
        >
          执行同步任务
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="条码" prop="barcode" min-width="180" />
        <el-table-column label="发货单号" prop="deliverNumber" min-width="150" />
        <el-table-column label="归属总部" prop="hqCompanyName" min-width="180" />
        <el-table-column label="CRM公司ID" prop="custId" min-width="120" />
        <el-table-column label="销售组织" prop="salesOrg" min-width="120" />
        <el-table-column label="物料编码" prop="productCode" min-width="120" />
        <el-table-column label="商品名称" prop="productName" min-width="160" show-overflow-tooltip />
        <el-table-column label="产品型号" prop="productModel" min-width="160" show-overflow-tooltip />
        <el-table-column label="机器小号" prop="machineNo" min-width="140" show-overflow-tooltip />
        <el-table-column label="条码扫码时间" prop="scanDate" width="170" />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleView(row)">查看</el-button>
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

    <el-dialog title="条码档案详情" :visible.sync="detailVisible" width="760px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="条码">{{ detail.barcode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发货单号">{{ detail.deliverNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归属总部">{{ detail.hqCompanyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CRM公司ID">{{ detail.custId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="销售组织">{{ detail.salesOrg || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ detail.productCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ detail.productName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产品型号">{{ detail.productModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="机器小号">{{ detail.machineNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="条码扫码时间">{{ detail.scanDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后出库日期">{{ detail.lastOutDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="CRM创建时间">{{ detail.crmAddTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近同步时间">{{ detail.lastSyncTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="质保状态">{{ detail.warrantyStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listMachineBarcode,
  getMachineBarcode,
  fullSyncMachineBarcode
} from '@/api/system'

export default {
  name: 'MachineBarcodeManage',
  data() {
    return {
      loading: false,
      syncLoading: false,
      total: 0,
      tableData: [],
      detailVisible: false,
      detail: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        barcode: '',
        deliverNumber: '',
        productCode: '',
        machineNo: '',
        productModel: '',
        status: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listMachineBarcode(this.queryParams).then(res => {
        if (!res) return
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
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
        deliverNumber: '',
        productCode: '',
        machineNo: '',
        productModel: '',
        status: undefined
      }
      this.getList()
    },
    handleView(row) {
      getMachineBarcode(row.id).then(res => {
        if (!res) return
        this.detail = res.data || null
        this.detailVisible = true
      })
    },
    handleFullSync() {
      this.$confirm('确认提交条码档案同步任务吗？系统会在后台执行，并把结果写入任务日志。', '提示', { type: 'warning' }).then(() => {
        this.syncLoading = true
        fullSyncMachineBarcode().then(res => {
          if (!res) return
          this.$message.success(`任务已提交，执行日志ID：${res.data}`)
        }).finally(() => { this.syncLoading = false })
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
