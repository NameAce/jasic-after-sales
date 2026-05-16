<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="工单号" prop="bizNo">
          <el-input v-model="queryParams.bizNo" placeholder="请输入工单号" clearable />
        </el-form-item>
        <el-form-item label="通知场景" prop="templateCode">
          <el-input v-model="queryParams.templateCode" placeholder="请输入通知场景编码" clearable />
        </el-form-item>
        <el-form-item label="渠道" prop="channelType">
          <el-select v-model="queryParams.channelType" placeholder="全部" clearable>
            <el-option label="小程序订阅消息" value="MP_SUBSCRIBE" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" prop="timeRange">
          <el-date-picker
            v-model="queryParams.timeRange"
            type="datetimerange"
            value-format="yyyy-MM-dd HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button icon="el-icon-refresh" size="small" @click="getList">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="业务编号" prop="bizNo" min-width="150" show-overflow-tooltip />
        <el-table-column label="通知场景" min-width="240" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <div>{{ templateLabel(row.templateCode) }}</div>
            <div class="muted-code">{{ row.templateCode || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="事件类型" prop="eventType" min-width="210" show-overflow-tooltip />
        <el-table-column label="接收对象" min-width="190" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <div>{{ formatReceiver(row) }}</div>
            <div v-if="row.receiverAddress" class="muted-code">{{ row.receiverAddress }}</div>
          </template>
        </el-table-column>
        <el-table-column label="渠道" min-width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.channelType ? 'primary' : 'info'">
              {{ channelLabel(row.channelType, row.messageId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="statusTagType(row.status)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果编码" prop="resultCode" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果说明" prop="resultMessage" min-width="240" show-overflow-tooltip />
        <el-table-column label="重试次数" prop="retryCount" width="90" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTrace:view']" @click="openEventDetail(row.eventId)">
              事件详情
            </el-button>
            <el-button
              v-if="row.dispatchId"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTrace:view']"
              @click="openDispatchDetail(row.dispatchId)"
            >
              分发详情
            </el-button>
            <el-button
              v-if="canRetryRow(row)"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTrace:retry']"
              @click="handleRowRetry(row)"
            >
              重试
            </el-button>
            <el-button
              v-if="canDeadRow(row)"
              type="text"
              size="mini"
              class="danger-action"
              v-hasPerms="['system:notifyTrace:dead']"
              @click="handleRowDead(row)"
            >
              标记死信
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

    <el-drawer
      :visible.sync="eventDrawer.visible"
      title="事件详情"
      size="820px"
      append-to-body
    >
      <div class="drawer-body" v-loading="eventDrawer.loading">
        <div class="drawer-toolbar" v-if="eventDrawer.data && eventDrawer.data.id">
          <el-button
            v-if="canRetryEventDetail(eventDrawer.data)"
            type="primary"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:retry']"
            @click="confirmRetry('event', eventDrawer.data.id)"
          >
            重试事件
          </el-button>
          <el-button
            v-if="canDeadEventDetail(eventDrawer.data)"
            type="danger"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:dead']"
            @click="confirmDead('event', eventDrawer.data.id)"
          >
            标记不再处理
          </el-button>
        </div>
        <el-descriptions v-if="eventDrawer.data" :column="2" border size="small">
          <el-descriptions-item label="事件ID">{{ eventDrawer.data.id }}</el-descriptions-item>
          <el-descriptions-item label="事件状态">
            <el-tag size="mini" :type="statusTagType(eventDrawer.data.status)">
              {{ statusLabel(eventDrawer.data.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="事件类型">{{ eventDrawer.data.eventType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="幂等键">{{ eventDrawer.data.eventKey || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ eventDrawer.data.bizType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务编号">{{ eventDrawer.data.bizNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ eventDrawer.data.bizId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收对象ID">{{ eventDrawer.data.receiverId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人ID">{{ eventDrawer.data.operatorId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ eventDrawer.data.retryCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ eventDrawer.data.processingTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次重试">{{ eventDrawer.data.nextRetryTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ eventDrawer.data.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ eventDrawer.data.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近错误" :span="2">
            <span class="error-text">{{ eventDrawer.data.errorMessage || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">事件载荷 payload_json</div>
        <pre class="json-view">{{ prettyJson(eventDrawer.data && eventDrawer.data.payloadJson) }}</pre>

        <div class="section-title">关联站内消息</div>
        <el-table :data="eventDrawer.data && eventDrawer.data.messages || []" size="mini" border>
          <el-table-column label="消息ID" prop="id" width="90" />
          <el-table-column label="接收人" min-width="150" show-overflow-tooltip>
            <template slot-scope="{ row }">
              {{ row.receiverName || '-' }} / {{ row.receiverId || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="statusTagType(row.todoStatus)">
                {{ statusLabel(row.todoStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失效原因" prop="invalidReason" min-width="160" show-overflow-tooltip />
        </el-table>

        <div class="section-title">关联分发任务</div>
        <el-table :data="eventDrawer.data && eventDrawer.data.dispatches || []" size="mini" border>
          <el-table-column label="分发ID" prop="id" width="90" />
          <el-table-column label="渠道" width="130">
            <template slot-scope="{ row }">{{ channelLabel(row.channelType) }}</template>
          </el-table-column>
          <el-table-column label="接收对象" min-width="150" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ receiverTypeLabel(row.receiverType) }} / {{ row.receiverId || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="statusTagType(row.dispatchStatus)">
                {{ statusLabel(row.dispatchStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结果说明" prop="resultMessage" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="190">
            <template slot-scope="{ row }">
              <el-button type="text" size="mini" @click="openDispatchDetail(row.id)">查看</el-button>
              <el-button
                v-if="canRetryDispatchDetail(row)"
                type="text"
                size="mini"
                v-hasPerms="['system:notifyTrace:retry']"
                @click="confirmRetry('dispatch', row.id)"
              >
                重试
              </el-button>
              <el-button
                v-if="canDeadDispatchDetail(row)"
                type="text"
                size="mini"
                class="danger-action"
                v-hasPerms="['system:notifyTrace:dead']"
                @click="confirmDead('dispatch', row.id)"
              >
                死信
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer
      :visible.sync="dispatchDrawer.visible"
      title="分发详情"
      size="820px"
      append-to-body
    >
      <div class="drawer-body" v-loading="dispatchDrawer.loading">
        <div class="drawer-toolbar" v-if="dispatchDrawer.data && dispatchDrawer.data.id">
          <el-button
            v-if="canRetryDispatchDetail(dispatchDrawer.data)"
            type="primary"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:retry']"
            @click="confirmRetry('dispatch', dispatchDrawer.data.id)"
          >
            重试分发
          </el-button>
          <el-button
            v-if="canDeadDispatchDetail(dispatchDrawer.data)"
            type="danger"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:dead']"
            @click="confirmDead('dispatch', dispatchDrawer.data.id)"
          >
            标记不再处理
          </el-button>
        </div>
        <el-descriptions v-if="dispatchDrawer.data" :column="2" border size="small">
          <el-descriptions-item label="分发ID">{{ dispatchDrawer.data.id }}</el-descriptions-item>
          <el-descriptions-item label="来源事件ID">{{ dispatchDrawer.data.eventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知场景">{{ dispatchDrawer.data.templateCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ channelLabel(dispatchDrawer.data.channelType) }}</el-descriptions-item>
          <el-descriptions-item label="接收对象">{{ receiverTypeLabel(dispatchDrawer.data.receiverType) }} / {{ dispatchDrawer.data.receiverId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收地址">{{ dispatchDrawer.data.receiverAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务编号">{{ dispatchDrawer.data.bizNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ dispatchDrawer.data.bizId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分发状态">
            <el-tag size="mini" :type="statusTagType(dispatchDrawer.data.dispatchStatus)">
              {{ statusLabel(dispatchDrawer.data.dispatchStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ dispatchDrawer.data.retryCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="结果编码">{{ dispatchDrawer.data.resultCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送成功时间">{{ dispatchDrawer.data.sentTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ dispatchDrawer.data.processingTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次重试">{{ dispatchDrawer.data.nextRetryTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ dispatchDrawer.data.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ dispatchDrawer.data.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近错误/结果说明" :span="2">
            <span class="error-text">{{ dispatchDrawer.data.resultMessage || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">分发载荷 payload_json</div>
        <pre class="json-view">{{ prettyJson(dispatchDrawer.data && dispatchDrawer.data.payloadJson) }}</pre>

        <div class="section-title">渠道响应 channel_response_json</div>
        <pre class="json-view">{{ prettyJson(dispatchDrawer.data && dispatchDrawer.data.channelResponseJson) }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  deadNotifyTraceDispatch,
  deadNotifyTraceEvent,
  getNotifyTraceDispatch,
  getNotifyTraceEvent,
  getNotifyTracePage,
  retryNotifyTraceDispatch,
  retryNotifyTraceEvent
} from '@/api/notify'

const RETRY_STATUS = ['FAILED', 'DEAD']
const EVENT_DEAD_STATUS = ['NEW', 'PROCESSING', 'FAILED']
const DISPATCH_DEAD_STATUS = ['PENDING', 'PROCESSING', 'FAILED']

export default {
  name: 'NotifyTraceManage',
  data() {
    return {
      loading: false,
      total: 0,
      tableData: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        bizNo: '',
        templateCode: '',
        channelType: '',
        status: '',
        timeRange: []
      },
      eventDrawer: {
        visible: false,
        loading: false,
        data: null
      },
      dispatchDrawer: {
        visible: false,
        loading: false,
        data: null
      },
      statusOptions: [
        { label: '新建 / NEW', value: 'NEW' },
        { label: '待发送 / PENDING', value: 'PENDING' },
        { label: '处理中 / PROCESSING', value: 'PROCESSING' },
        { label: '成功 / SUCCESS', value: 'SUCCESS' },
        { label: '失败 / FAILED', value: 'FAILED' },
        { label: '跳过 / SKIPPED', value: 'SKIPPED' },
        { label: '死信 / DEAD', value: 'DEAD' },
        { label: '已读 / READ', value: 'READ' },
        { label: '已处理 / DONE', value: 'DONE' },
        { label: '已失效 / INVALID', value: 'INVALID' }
      ]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      getNotifyTracePage(this.buildQueryParams()).then(res => {
        if (!res) return
        const data = res.data || {}
        this.tableData = data.records || []
        this.total = data.total || 0
      }).finally(() => {
        this.loading = false
      })
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
        bizNo: '',
        templateCode: '',
        channelType: '',
        status: '',
        timeRange: []
      }
      this.getList()
    },
    buildQueryParams() {
      const params = Object.assign({}, this.queryParams)
      const timeRange = params.timeRange || []
      delete params.timeRange
      if (timeRange.length === 2) {
        params.beginTime = timeRange[0]
        params.endTime = timeRange[1]
      }
      return params
    },
    openEventDetail(eventId) {
      if (!eventId) {
        this.$message.warning('当前记录没有事件ID')
        return
      }
      this.eventDrawer.visible = true
      this.loadEventDetail(eventId)
    },
    loadEventDetail(eventId) {
      this.eventDrawer.loading = true
      getNotifyTraceEvent(eventId).then(res => {
        if (!res) return
        this.eventDrawer.data = res.data || null
      }).finally(() => {
        this.eventDrawer.loading = false
      })
    },
    openDispatchDetail(dispatchId) {
      if (!dispatchId) {
        this.$message.warning('当前记录没有分发ID')
        return
      }
      this.dispatchDrawer.visible = true
      this.loadDispatchDetail(dispatchId)
    },
    loadDispatchDetail(dispatchId) {
      this.dispatchDrawer.loading = true
      getNotifyTraceDispatch(dispatchId).then(res => {
        if (!res) return
        this.dispatchDrawer.data = res.data || null
      }).finally(() => {
        this.dispatchDrawer.loading = false
      })
    },
    handleRowRetry(row) {
      const target = this.resolveRowTarget(row)
      this.confirmRetry(target.type, target.id)
    },
    handleRowDead(row) {
      const target = this.resolveRowTarget(row)
      this.confirmDead(target.type, target.id)
    },
    confirmRetry(type, id) {
      if (!id) {
        this.$message.warning('未获取到可操作记录ID')
        return
      }
      const label = type === 'dispatch' ? '分发任务' : '通知事件'
      this.$confirm(`确认将该${label}重新放回待处理队列吗？`, '重试确认', { type: 'warning' })
        .then(() => this.retryTarget(type, id))
        .catch(() => {})
    },
    retryTarget(type, id) {
      const api = type === 'dispatch' ? retryNotifyTraceDispatch : retryNotifyTraceEvent
      api(id).then(res => {
        if (!res) return
        this.$message.success('重试已提交')
        this.refreshAfterAction(type, id)
      })
    },
    confirmDead(type, id) {
      if (!id) {
        this.$message.warning('未获取到可操作记录ID')
        return
      }
      const label = type === 'dispatch' ? '分发任务' : '通知事件'
      this.$prompt(`请填写该${label}标记不再处理的原因`, '死信确认', {
        confirmButtonText: '确认标记',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPattern: /\S+/,
        inputErrorMessage: '处理原因不能为空'
      }).then(({ value }) => {
        // 死信会终止自动消费或发送，原因必须交给后端留痕，便于后续区分人工终止和自动失败。
        this.deadTarget(type, id, value)
      }).catch(() => {})
    },
    deadTarget(type, id, reason) {
      const api = type === 'dispatch' ? deadNotifyTraceDispatch : deadNotifyTraceEvent
      api(id, { reason }).then(res => {
        if (!res) return
        this.$message.success('已标记不再处理')
        this.refreshAfterAction(type, id)
      })
    },
    refreshAfterAction(type, id) {
      this.getList()
      if (type === 'event' && this.eventDrawer.visible && this.eventDrawer.data && this.eventDrawer.data.id === id) {
        this.loadEventDetail(id)
      }
      if (type === 'dispatch' && this.dispatchDrawer.visible && this.dispatchDrawer.data && this.dispatchDrawer.data.id === id) {
        this.loadDispatchDetail(id)
      }
      if (type === 'dispatch' && this.eventDrawer.visible && this.eventDrawer.data && this.eventDrawer.data.id) {
        this.loadEventDetail(this.eventDrawer.data.id)
      }
    },
    resolveRowTarget(row) {
      if (row.dispatchId) {
        return { type: 'dispatch', id: row.dispatchId, status: row.dispatchStatus || row.status }
      }
      return { type: 'event', id: row.eventId, status: row.eventStatus || row.status }
    },
    canRetryRow(row) {
      const target = this.resolveRowTarget(row)
      return Boolean(target.id && RETRY_STATUS.includes(target.status))
    },
    canDeadRow(row) {
      const target = this.resolveRowTarget(row)
      if (!target.id) return false
      if (target.type === 'dispatch') {
        return DISPATCH_DEAD_STATUS.includes(target.status)
      }
      return EVENT_DEAD_STATUS.includes(target.status)
    },
    canRetryEventDetail(detail) {
      return Boolean(detail && detail.id && RETRY_STATUS.includes(detail.status))
    },
    canDeadEventDetail(detail) {
      return Boolean(detail && detail.id && EVENT_DEAD_STATUS.includes(detail.status))
    },
    canRetryDispatchDetail(detail) {
      return Boolean(detail && detail.id && RETRY_STATUS.includes(detail.dispatchStatus))
    },
    canDeadDispatchDetail(detail) {
      return Boolean(detail && detail.id && DISPATCH_DEAD_STATUS.includes(detail.dispatchStatus))
    },
    templateLabel(code) {
      const map = {
        WORK_ORDER_ASSIGNED: '工单派单待办',
        WORK_ORDER_EVALUATION_INVITE: '客户评价邀请'
      }
      return map[code] || code || '-'
    },
    channelLabel(code, messageId) {
      if (!code && messageId) return '站内消息'
      const map = {
        IN_APP: '站内消息',
        MP_SUBSCRIBE: '小程序订阅消息',
        SMS: '短信',
        EMAIL: '邮件'
      }
      return map[code] || code || '-'
    },
    receiverTypeLabel(code) {
      const map = {
        CUSTOMER: '客户',
        SYS_USER: '系统用户'
      }
      return map[code] || code || '-'
    },
    formatReceiver(row) {
      if (!row.receiverType && !row.receiverId) return '-'
      return `${this.receiverTypeLabel(row.receiverType)} / ${row.receiverId || '-'}`
    },
    statusLabel(status) {
      const map = {
        NEW: '新建',
        PENDING: '待处理',
        PROCESSING: '处理中',
        SUCCESS: '成功',
        FAILED: '失败',
        SKIPPED: '跳过',
        DEAD: '死信',
        READ: '已读',
        DONE: '已处理',
        INVALID: '已失效'
      }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = {
        SUCCESS: 'success',
        DONE: 'success',
        READ: 'primary',
        FAILED: 'danger',
        DEAD: 'danger',
        INVALID: 'info',
        SKIPPED: 'warning',
        PROCESSING: 'warning',
        NEW: 'info',
        PENDING: 'info'
      }
      return map[status] || 'info'
    },
    prettyJson(value) {
      if (!value) return '-'
      if (typeof value === 'object') {
        return JSON.stringify(value, null, 2)
      }
      try {
        return JSON.stringify(JSON.parse(value), null, 2)
      } catch (e) {
        return String(value)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.table-toolbar { margin-bottom: 12px; }
.muted-code {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
.danger-action { color: #F56C6C; }
.drawer-body {
  padding: 0 20px 24px;
}
.drawer-toolbar {
  margin-bottom: 12px;
  text-align: right;
}
.section-title {
  margin: 18px 0 8px;
  font-weight: 600;
  color: #303133;
}
.json-view {
  min-height: 90px;
  max-height: 320px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #F7F8FA;
  color: #303133;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-word;
}
.error-text {
  color: #F56C6C;
}
</style>
