<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="工单号" prop="bizNo">
          <el-input v-model="queryParams.bizNo" placeholder="请输入工单号" clearable />
        </el-form-item>
        <el-form-item label="通知场景" prop="sceneCode">
          <el-input v-model="queryParams.sceneCode" placeholder="请输入通知场景编码" clearable />
        </el-form-item>
        <el-form-item label="通知目标" prop="targetType">
          <el-select v-model="queryParams.targetType" placeholder="全部" clearable>
            <el-option v-for="item in targetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件状态" prop="eventStatus">
          <el-select v-model="queryParams.eventStatus" placeholder="全部" clearable>
            <el-option v-for="item in eventStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分发状态" prop="dispatchStatus">
          <el-select v-model="queryParams.dispatchStatus" placeholder="全部" clearable>
            <el-option v-for="item in dispatchStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column label="通知场景" min-width="220" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <div>{{ sceneLabel(row.sceneCode, row.sceneName) }}</div>
            <div class="muted-code">{{ row.sceneCode || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="事件状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="statusTagType(row.eventStatus)">
              {{ eventStatusLabel(row.eventStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="站内产物" min-width="320">
          <template slot-scope="{ row }">
            <div v-if="row.messageCount" class="summary-block">
              <div class="summary-count">共 {{ row.messageCount }} 条</div>
              <div v-for="summary in row.messageTargetSummaries || []" :key="`message-${summary.targetType}`" class="summary-item">
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <el-tag size="mini" :type="statusTagType(summary.highlightStatus)">
                    {{ summary.highlightStatusDesc || '-' }}
                  </el-tag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
                <div class="summary-item__tags">
                  <el-tag
                    v-for="item in summary.statusCounts || []"
                    :key="`${summary.targetType}-${item.status}`"
                    size="mini"
                    :type="statusTagType(item.status)"
                  >
                    {{ item.statusDesc }} {{ item.count }}
                  </el-tag>
                </div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="外部分发" min-width="320">
          <template slot-scope="{ row }">
            <div v-if="row.dispatchCount" class="summary-block">
              <div class="summary-count">共 {{ row.dispatchCount }} 条</div>
              <div v-for="summary in row.dispatchTargetSummaries || []" :key="`dispatch-${summary.targetType}`" class="summary-item">
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <el-tag size="mini" :type="statusTagType(summary.highlightStatus)">
                    {{ summary.highlightStatusDesc || '-' }}
                  </el-tag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
                <div class="summary-item__tags">
                  <el-tag
                    v-for="item in summary.statusCounts || []"
                    :key="`${summary.targetType}-${item.status}`"
                    size="mini"
                    :type="statusTagType(item.status)"
                  >
                    {{ item.statusDesc }} {{ item.count }}
                  </el-tag>
                </div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="最近错误" prop="eventErrorMessage" min-width="240" show-overflow-tooltip />
        <el-table-column label="重试次数" prop="eventRetryCount" width="90" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTrace:view']" @click="openEventDetail(row.eventId)">
              事件详情
            </el-button>
            <el-button
              v-if="canRetryEventStatus(row.eventStatus)"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTrace:retry']"
              @click="confirmRetry('event', row.eventId)"
            >
              重试事件
            </el-button>
            <el-button
              v-if="canDeadEventStatus(row.eventStatus)"
              type="text"
              size="mini"
              class="danger-action"
              v-hasPerms="['system:notifyTrace:dead']"
              @click="confirmDead('event', row.eventId)"
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
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-drawer
      :visible.sync="eventDrawer.visible"
      title="事件详情"
      size="900px"
      append-to-body
    >
      <div class="drawer-body" v-loading="eventDrawer.loading">
        <div class="drawer-toolbar" v-if="eventDrawer.data && eventDrawer.data.id">
          <el-button
            v-if="canRetryEventStatus(eventDrawer.data.status)"
            type="primary"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:retry']"
            @click="confirmRetry('event', eventDrawer.data.id)"
          >
            重试事件
          </el-button>
          <el-button
            v-if="canDeadEventStatus(eventDrawer.data.status)"
            type="danger"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:dead']"
            @click="confirmDead('event', eventDrawer.data.id)"
          >
            标记死信
          </el-button>
        </div>
        <el-descriptions v-if="eventDrawer.data" :column="2" border size="small">
          <el-descriptions-item label="事件ID">{{ eventDrawer.data.id }}</el-descriptions-item>
          <el-descriptions-item label="事件状态">
            <el-tag size="mini" :type="statusTagType(eventDrawer.data.status)">
              {{ eventStatusLabel(eventDrawer.data.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="通知场景">
            {{ sceneLabel(eventDrawer.data.sceneCode, eventDrawer.data.sceneName) }}
          </el-descriptions-item>
          <el-descriptions-item label="场景编码">{{ eventDrawer.data.sceneCode || '-' }}</el-descriptions-item>
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

        <div class="section-title">目标产物概览</div>
        <div class="overview-grid">
          <div class="overview-card">
            <div class="overview-card__title">站内消息 / 站内待办</div>
            <div v-if="eventDrawer.data && eventDrawer.data.messageTargetSummaries && eventDrawer.data.messageTargetSummaries.length">
              <div
                v-for="summary in eventDrawer.data.messageTargetSummaries"
                :key="`event-message-${summary.targetType}`"
                class="summary-item"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <el-tag size="mini" :type="statusTagType(summary.highlightStatus)">
                    {{ summary.highlightStatusDesc || '-' }}
                  </el-tag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
                <div class="summary-item__tags">
                  <el-tag
                    v-for="item in summary.statusCounts || []"
                    :key="`event-message-${summary.targetType}-${item.status}`"
                    size="mini"
                    :type="statusTagType(item.status)"
                  >
                    {{ item.statusDesc }} {{ item.count }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div v-else class="empty-text">当前事件未生成站内产物</div>
          </div>
          <div class="overview-card">
            <div class="overview-card__title">外部分发任务</div>
            <div v-if="eventDrawer.data && eventDrawer.data.dispatchTargetSummaries && eventDrawer.data.dispatchTargetSummaries.length">
              <div
                v-for="summary in eventDrawer.data.dispatchTargetSummaries"
                :key="`event-dispatch-${summary.targetType}`"
                class="summary-item"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <el-tag size="mini" :type="statusTagType(summary.highlightStatus)">
                    {{ summary.highlightStatusDesc || '-' }}
                  </el-tag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
                <div class="summary-item__tags">
                  <el-tag
                    v-for="item in summary.statusCounts || []"
                    :key="`event-dispatch-${summary.targetType}-${item.status}`"
                    size="mini"
                    :type="statusTagType(item.status)"
                  >
                    {{ item.statusDesc }} {{ item.count }}
                  </el-tag>
                </div>
              </div>
            </div>
            <div v-else class="empty-text">当前事件未生成外部分发任务</div>
          </div>
        </div>

        <div class="section-title">事件载荷 payload_json</div>
        <pre class="json-view">{{ prettyJson(eventDrawer.data && eventDrawer.data.payloadJson) }}</pre>

        <div class="section-title">关联站内产物</div>
        <el-table :data="eventDrawer.data && eventDrawer.data.messages || []" size="mini" border>
          <el-table-column label="消息ID" prop="id" width="90" />
          <el-table-column label="目标" min-width="130">
            <template slot-scope="{ row }">
              <div>{{ targetTypeLabel(row.targetType, row.targetTypeDesc) }}</div>
              <div class="muted-code">{{ row.targetType || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="接收人" min-width="160" show-overflow-tooltip>
            <template slot-scope="{ row }">
              {{ row.receiverName || '-' }} / {{ row.receiverId || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="statusTagType(row.todoStatus)">
                {{ inAppStatusLabel(row.todoStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失效原因" prop="invalidReason" min-width="160" show-overflow-tooltip />
          <el-table-column label="创建时间" prop="createTime" width="170" />
        </el-table>

        <div class="section-title">关联外部分发任务</div>
        <el-table :data="eventDrawer.data && eventDrawer.data.dispatches || []" size="mini" border>
          <el-table-column label="分发ID" prop="id" width="90" />
          <el-table-column label="目标" min-width="130">
            <template slot-scope="{ row }">
              <div>{{ targetTypeLabel(row.targetType, row.targetTypeDesc) }}</div>
              <div class="muted-code">{{ row.targetType || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="渠道" width="120">
            <template slot-scope="{ row }">{{ targetTypeLabel(row.channelType) }}</template>
          </el-table-column>
          <el-table-column label="接收地址" prop="receiverAddress" min-width="170" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="statusTagType(row.dispatchStatus)">
                {{ dispatchStatusLabel(row.dispatchStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结果编码" prop="resultCode" min-width="160" show-overflow-tooltip />
          <el-table-column label="结果说明" prop="resultMessage" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="220">
            <template slot-scope="{ row }">
              <el-button type="text" size="mini" @click="openDispatchDetail(row.id)">查看</el-button>
              <el-button
                v-if="canRetryDispatchStatus(row.dispatchStatus)"
                type="text"
                size="mini"
                v-hasPerms="['system:notifyTrace:retry']"
                @click="confirmRetry('dispatch', row.id)"
              >
                重试
              </el-button>
              <el-button
                v-if="canDeadDispatchStatus(row.dispatchStatus)"
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
      size="860px"
      append-to-body
    >
      <div class="drawer-body" v-loading="dispatchDrawer.loading">
        <div class="drawer-toolbar" v-if="dispatchDrawer.data && dispatchDrawer.data.id">
          <el-button
            v-if="canRetryDispatchStatus(dispatchDrawer.data.dispatchStatus)"
            type="primary"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:retry']"
            @click="confirmRetry('dispatch', dispatchDrawer.data.id)"
          >
            重试分发
          </el-button>
          <el-button
            v-if="canDeadDispatchStatus(dispatchDrawer.data.dispatchStatus)"
            type="danger"
            plain
            size="small"
            v-hasPerms="['system:notifyTrace:dead']"
            @click="confirmDead('dispatch', dispatchDrawer.data.id)"
          >
            标记死信
          </el-button>
        </div>
        <el-descriptions v-if="dispatchDrawer.data" :column="2" border size="small">
          <el-descriptions-item label="分发ID">{{ dispatchDrawer.data.id }}</el-descriptions-item>
          <el-descriptions-item label="来源事件ID">{{ dispatchDrawer.data.eventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知场景">
            {{ sceneLabel(dispatchDrawer.data.sceneCode, dispatchDrawer.data.sceneName) }}
          </el-descriptions-item>
          <el-descriptions-item label="场景编码">{{ dispatchDrawer.data.sceneCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知目标">
            {{ targetTypeLabel(dispatchDrawer.data.targetType, dispatchDrawer.data.targetTypeDesc) }}
          </el-descriptions-item>
          <el-descriptions-item label="目标编码">{{ dispatchDrawer.data.targetType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ targetTypeLabel(dispatchDrawer.data.channelType) }}</el-descriptions-item>
          <el-descriptions-item label="接收对象">{{ dispatchDrawer.data.receiverId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收地址">{{ dispatchDrawer.data.receiverAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务编号">{{ dispatchDrawer.data.bizNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务ID">{{ dispatchDrawer.data.bizId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分发状态">
            <el-tag size="mini" :type="statusTagType(dispatchDrawer.data.dispatchStatus)">
              {{ dispatchStatusLabel(dispatchDrawer.data.dispatchStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="重试次数">{{ dispatchDrawer.data.retryCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="结果编码">{{ dispatchDrawer.data.resultCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送成功时间">{{ dispatchDrawer.data.sentTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ dispatchDrawer.data.processingTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次重试">{{ dispatchDrawer.data.nextRetryTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ dispatchDrawer.data.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ dispatchDrawer.data.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近错误 / 结果说明" :span="2">
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
      targetTypeOptions: [
        { label: '站内消息', value: 'IN_APP_MESSAGE' },
        { label: '站内待办', value: 'IN_APP_TODO' },
        { label: '小程序订阅消息(B端)', value: 'MP_SUBSCRIBE_B' },
        { label: '小程序订阅消息(C端)', value: 'MP_SUBSCRIBE_C' }
      ],
      eventStatusOptions: [
        { label: '新建 / NEW', value: 'NEW' },
        { label: '处理中 / PROCESSING', value: 'PROCESSING' },
        { label: '成功 / SUCCESS', value: 'SUCCESS' },
        { label: '失败 / FAILED', value: 'FAILED' },
        { label: '死信 / DEAD', value: 'DEAD' }
      ],
      dispatchStatusOptions: [
        { label: '待发送 / PENDING', value: 'PENDING' },
        { label: '处理中 / PROCESSING', value: 'PROCESSING' },
        { label: '成功 / SUCCESS', value: 'SUCCESS' },
        { label: '失败 / FAILED', value: 'FAILED' },
        { label: '跳过 / SKIPPED', value: 'SKIPPED' },
        { label: '死信 / DEAD', value: 'DEAD' }
      ],
      queryParams: this.createDefaultQueryParams(),
      eventDrawer: {
        visible: false,
        loading: false,
        data: null
      },
      dispatchDrawer: {
        visible: false,
        loading: false,
        data: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    createDefaultQueryParams() {
      return {
        pageNum: 1,
        pageSize: 10,
        bizNo: '',
        sceneCode: '',
        targetType: '',
        eventStatus: '',
        dispatchStatus: '',
        timeRange: []
      }
    },
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
      this.queryParams = this.createDefaultQueryParams()
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
    handleSizeChange(value) {
      this.queryParams.pageSize = value
      this.getList()
    },
    handleCurrentChange(value) {
      this.queryParams.pageNum = value
      this.getList()
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
      this.$prompt(`请填写该${label}标记死信的原因`, '死信确认', {
        confirmButtonText: '确认标记',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPattern: /\S+/,
        inputErrorMessage: '处理原因不能为空'
      }).then(({ value }) => {
        this.deadTarget(type, id, value)
      }).catch(() => {})
    },
    deadTarget(type, id, reason) {
      const api = type === 'dispatch' ? deadNotifyTraceDispatch : deadNotifyTraceEvent
      api(id, { reason }).then(res => {
        if (!res) return
        this.$message.success('已标记死信')
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
    canRetryEventStatus(status) {
      return RETRY_STATUS.includes(status)
    },
    canDeadEventStatus(status) {
      return EVENT_DEAD_STATUS.includes(status)
    },
    canRetryDispatchStatus(status) {
      return RETRY_STATUS.includes(status)
    },
    canDeadDispatchStatus(status) {
      return DISPATCH_DEAD_STATUS.includes(status)
    },
    sceneLabel(sceneCode, sceneName) {
      return sceneName || sceneCode || '-'
    },
    targetTypeLabel(code, desc) {
      if (desc) return desc
      const map = {
        IN_APP_MESSAGE: '站内消息',
        IN_APP_TODO: '站内待办',
        MP_SUBSCRIBE_B: '小程序订阅消息(B端)',
        MP_SUBSCRIBE_C: '小程序订阅消息(C端)',
        SMS: '短信',
        EMAIL: '邮件'
      }
      return map[code] || code || '-'
    },
    eventStatusLabel(status) {
      const map = {
        NEW: '新建',
        PROCESSING: '处理中',
        SUCCESS: '成功',
        FAILED: '失败',
        DEAD: '死信'
      }
      return map[status] || status || '-'
    },
    dispatchStatusLabel(status) {
      const map = {
        PENDING: '待发送',
        PROCESSING: '处理中',
        SUCCESS: '成功',
        FAILED: '失败',
        SKIPPED: '已跳过',
        DEAD: '死信'
      }
      return map[status] || status || '-'
    },
    inAppStatusLabel(status) {
      const map = {
        PENDING: '待处理',
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
.summary-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-count {
  color: #606266;
  font-size: 12px;
}
.summary-item {
  padding: 10px 12px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #FAFBFC;
}
.summary-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  color: #303133;
  font-weight: 600;
}
.summary-item__text {
  color: #606266;
  font-size: 12px;
  line-height: 18px;
}
.summary-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.overview-card {
  padding: 12px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #fff;
}
.overview-card__title {
  margin-bottom: 10px;
  color: #303133;
  font-weight: 600;
}
.empty-text {
  color: #909399;
  font-size: 12px;
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
