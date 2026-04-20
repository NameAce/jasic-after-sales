<template>
  <div class="app-container">
    <el-card shadow="never" class="notify-card">
      <div slot="header" class="notify-card__header">
        <span>消息中心</span>
      </div>
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="待处理" name="TODO" />
        <el-tab-pane label="历史记录" name="HISTORY" />
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="messageList"
        border
        stripe
        empty-text="暂无消息"
        @row-click="handleRowClick"
      >
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="摘要" prop="summary" min-width="320" show-overflow-tooltip />
        <el-table-column label="工单号" prop="bizNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" min-width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.todoStatus)" size="mini">
              {{ statusLabel(row.todoStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="170" />
      </el-table>

      <el-pagination
        class="pagination"
        :current-page="pageState[activeTab].pageNum"
        :page-size="pageState[activeTab].pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script>
import { getNotifyTodoPage, markNotifyMessageRead } from '@/api/notify'

const WORK_ORDER_DETAIL_ROUTE = '/afterSales/workOrder'

function buildDefaultPageState() {
  return {
    pageNum: 1,
    pageSize: 10
  }
}

const STATUS_META = {
  PENDING: { label: '待处理', type: 'warning' },
  READ: { label: '已读', type: 'info' },
  DONE: { label: '已处理', type: 'success' },
  INVALID: { label: '已失效', type: 'danger' }
}

export default {
  name: 'NotifyCenter',
  data() {
    return {
      activeTab: 'TODO',
      loading: false,
      total: 0,
      messageList: [],
      pageState: {
        TODO: buildDefaultPageState(),
        HISTORY: buildDefaultPageState()
      }
    }
  },
  created() {
    this.initializePage()
  },
  methods: {
    initializePage() {
      this.refreshTodoCount()
      this.loadList()
    },
    refreshTodoCount() {
      return this.$store.dispatch('notify/fetchTodoCount').catch(() => {})
    },
    loadList() {
      const currentPageState = this.pageState[this.activeTab]
      this.loading = true
      return getNotifyTodoPage({
        box: this.activeTab,
        pageNum: currentPageState.pageNum,
        pageSize: currentPageState.pageSize
      }).then(res => {
        if (!res) {
          return
        }
        const data = res.data || {}
        this.messageList = data.rows || []
        this.total = data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleTabChange() {
      this.loadList()
      this.refreshTodoCount()
    },
    handleSizeChange(pageSize) {
      this.pageState[this.activeTab].pageSize = pageSize
      this.pageState[this.activeTab].pageNum = 1
      this.loadList()
    },
    handleCurrentChange(pageNum) {
      this.pageState[this.activeTab].pageNum = pageNum
      this.loadList()
    },
    handleRowClick(row) {
      this.openMessage(row)
    },
    openMessage(row) {
      const workOrderId = this.resolveWorkOrderId(row)
      if (!workOrderId) {
        this.$message.warning('当前消息缺少工单跳转信息')
        return
      }
      const jump = () => {
        this.$router.push({
          path: WORK_ORDER_DETAIL_ROUTE,
          query: {
            detailId: String(workOrderId),
            fromNotify: '1'
          }
        }).catch(() => {})
      }
      if (row.todoStatus !== 'PENDING') {
        jump()
        return
      }
      markNotifyMessageRead(row.id).then(res => {
        if (!res) {
          return
        }
        jump()
      })
    },
    resolveWorkOrderId(row) {
      const routeValueId = Number(row && row.routeValue)
      if (Number.isFinite(routeValueId) && routeValueId > 0) {
        return routeValueId
      }
      const bizId = Number(row && row.bizId)
      if (Number.isFinite(bizId) && bizId > 0) {
        return bizId
      }
      return null
    },
    statusLabel(status) {
      return (STATUS_META[status] && STATUS_META[status].label) || status || '-'
    },
    statusTagType(status) {
      return (STATUS_META[status] && STATUS_META[status].type) || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.notify-card {
  .notify-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    font-weight: 600;
  }

  ::v-deep .el-table__body tr {
    cursor: pointer;
  }
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
