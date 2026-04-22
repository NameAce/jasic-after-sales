<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="queryParams.templateCode" placeholder="请输入模板编码" clearable />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable />
        </el-form-item>
        <el-form-item label="模板来源" prop="templateSource">
          <el-select v-model="queryParams.templateSource" placeholder="全部" clearable>
            <el-option label="内置模板" value="BUILT_IN" />
            <el-option label="自定义模板" value="CUSTOM" />
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
          icon="el-icon-refresh"
          size="small"
          v-hasPerms="['system:notifyTemplate:refresh']"
          @click="handleRefreshCache"
        >
          刷新缓存
        </el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="模板编码" prop="templateCode" width="210" />
        <el-table-column label="模板名称" prop="templateName" min-width="180" />
        <el-table-column label="模板来源" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.templateSource === 'BUILT_IN' ? 'warning' : 'success'" size="mini">
              {{ row.templateSource === 'BUILT_IN' ? '内置' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知开关" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.notifyEnabled === 1 ? 'success' : 'info'" size="mini">
              {{ row.notifyEnabled === 1 ? '开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="覆盖开关" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.overrideEnabled === 1 ? 'primary' : 'info'" size="mini">
              {{ row.overrideEnabled === 1 ? '覆盖' : '回退' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由类型" prop="routeType" width="180" />
        <el-table-column label="标题模板" prop="titleTemplate" min-width="220" show-overflow-tooltip />
        <el-table-column label="摘要模板" prop="summaryTemplate" min-width="260" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" width="340" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:view']" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:preview']" @click="handlePreviewRow(row)">预览</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:view']" @click="handleChannels(row)">渠道配置</el-button>
            <el-button
              v-if="row.templateSource === 'BUILT_IN' && !hasCustomTemplate(row.templateCode)"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTemplate:add']"
              @click="handleAdd(row)"
            >
              新增自定义
            </el-button>
            <el-button
              v-if="row.templateSource === 'CUSTOM'"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTemplate:update']"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.templateSource === 'CUSTOM'"
              type="text"
              size="mini"
              style="color: #F56C6C;"
              v-hasPerms="['system:notifyTemplate:remove']"
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称">
              <el-input v-model="form.templateName" :disabled="formReadonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="通知开关" prop="notifyEnabled">
              <el-radio-group v-model="form.notifyEnabled" :disabled="formReadonly">
                <el-radio :label="1">开启</el-radio>
                <el-radio :label="0">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="覆盖开关" prop="overrideEnabled">
              <el-radio-group v-model="form.overrideEnabled" :disabled="formReadonly">
                <el-radio :label="1">启用覆盖</el-radio>
                <el-radio :label="0">回退内置</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="路由类型">
          <el-select v-model="form.routeType" :disabled="formReadonly" placeholder="请选择路由类型" clearable style="width: 100%;">
            <el-option v-for="item in routeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板">
          <el-input v-model="form.titleTemplate" :disabled="formReadonly" placeholder="留空则回退内置模板" />
        </el-form-item>
        <el-form-item label="摘要模板">
          <el-input
            v-model="form.summaryTemplate"
            :disabled="formReadonly"
            type="textarea"
            :rows="3"
            placeholder="留空则回退内置模板"
          />
        </el-form-item>
        <el-form-item label="路由值模板">
          <el-input v-model="form.routeValueTemplate" :disabled="formReadonly" placeholder="例如 ${bizId}" />
        </el-form-item>
        <el-form-item label="变量说明">
          <el-input :value="formatVariables(form.variablesJson)" type="textarea" :rows="6" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" :disabled="formReadonly" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="!formReadonly" @click="handlePreviewForm">预览</el-button>
        <el-button v-if="!formReadonly" type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="`渠道配置 - ${channelDialog.templateCode || ''}`" :visible.sync="channelDialog.visible" width="980px" append-to-body>
      <div class="channel-toolbar">
        <el-button
          type="primary"
          plain
          size="mini"
          v-hasPerms="['system:notifyTemplate:update']"
          @click="addChannelRow"
        >
          新增渠道
        </el-button>
      </div>
      <div v-for="(item, index) in channelDialog.rows" :key="index" class="channel-card">
        <div class="channel-card__header">
          <span>渠道 {{ index + 1 }}</span>
          <el-button
            type="text"
            size="mini"
            style="color: #F56C6C;"
            v-hasPerms="['system:notifyTemplate:update']"
            @click="removeChannelRow(index)"
          >
            删除
          </el-button>
        </div>
        <el-form :model="item" label-position="right" class="channel-form">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="渠道类型" label-width="90px">
                <el-select v-model="item.channelType" placeholder="请选择渠道" style="width: 100%;" :disabled="channelDialog.readonly">
                  <el-option label="小程序订阅消息" value="MP_SUBSCRIBE" />
                  <el-option label="短信" value="SMS" />
                  <el-option label="邮件" value="EMAIL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="渠道开关" label-width="90px">
                <el-radio-group v-model="item.channelEnabled" :disabled="channelDialog.readonly">
                  <el-radio :label="1">开启</el-radio>
                  <el-radio :label="0">关闭</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="小程序场景" label-width="90px">
                <el-select
                  v-model="item.channelScene"
                  placeholder="请选择"
                  style="width: 100%;"
                  :disabled="channelDialog.readonly || item.channelType !== 'MP_SUBSCRIBE'"
                >
                  <el-option label="B 端小程序" value="B" />
                  <el-option label="C 端小程序" value="C" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="10">
              <el-form-item label="模板 ID" label-width="90px">
                <el-input v-model="item.templateId" :disabled="channelDialog.readonly || item.channelType !== 'MP_SUBSCRIBE'" />
              </el-form-item>
            </el-col>
            <el-col :span="14">
              <el-form-item label="页面路径模板" label-width="110px">
                <el-input
                  v-model="item.pagePathTemplate"
                  :disabled="channelDialog.readonly || item.channelType !== 'MP_SUBSCRIBE'"
                  placeholder="例如 pages/order/evaluate?workOrderId=${workOrderId}"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="字段映射" label-width="90px">
            <div class="mapping-toolbar" v-if="!channelDialog.readonly && item.channelType === 'MP_SUBSCRIBE'">
              <el-button type="primary" plain size="mini" @click="addFieldMapping(item)">新增字段</el-button>
            </div>
            <el-table :data="item.fieldMapping || []" size="mini" border>
              <el-table-column label="模板字段名" min-width="180">
                <template slot-scope="{ row: mapping }">
                  <el-input v-model="mapping.field" :disabled="channelDialog.readonly || item.channelType !== 'MP_SUBSCRIBE'" placeholder="例如 thing1" />
                </template>
              </el-table-column>
              <el-table-column label="变量表达式" min-width="220">
                <template slot-scope="{ row: mapping }">
                  <el-input
                    v-model="mapping.value"
                    :disabled="channelDialog.readonly || item.channelType !== 'MP_SUBSCRIBE'"
                    placeholder="例如 ${orderNo}"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template slot-scope="{ $index: mappingIndex }">
                  <el-button
                    v-if="!channelDialog.readonly && item.channelType === 'MP_SUBSCRIBE'"
                    type="text"
                    size="mini"
                    style="color: #F56C6C;"
                    @click="removeFieldMapping(item, mappingIndex)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="channel-tip">
              可用展示变量：`orderNo`、`customerMobile`、`companyName`、`closedTime`。评价通知的页面路由变量仅支持 `workOrderId`。
            </div>
          </el-form-item>
          <el-form-item label="备注" label-width="90px">
            <el-input v-model="item.remark" :disabled="channelDialog.readonly" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer">
        <el-button @click="channelDialog.visible = false">取消</el-button>
        <el-button
          v-if="!channelDialog.readonly"
          type="primary"
          :loading="channelDialog.loading"
          v-hasPerms="['system:notifyTemplate:update']"
          @click="saveChannels"
        >
          保存
        </el-button>
      </div>
    </el-dialog>

    <el-dialog title="模板预览" :visible.sync="previewVisible" width="760px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="示例变量 JSON">
          <el-input v-model="previewPayload.variablesText" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <div class="preview-toolbar">
        <el-button type="primary" :loading="previewLoading" @click="loadPreview">执行预览</el-button>
      </div>
      <el-descriptions :column="1" border size="small" v-if="previewResult">
        <el-descriptions-item label="是否发送">{{ previewResult.notifyEnabled ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="实际来源">{{ previewResult.templateSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ previewResult.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ previewResult.summary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路由类型">{{ previewResult.routeType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路由值">{{ previewResult.routeValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">
          <div v-if="previewResult.errors && previewResult.errors.length">
            <div v-for="(item, index) in previewResult.errors" :key="index" class="preview-error">{{ item }}</div>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import {
  addNotifyTemplateCustom,
  deleteNotifyTemplateCustom,
  getNotifyTemplate,
  listNotifyTemplate,
  listNotifyTemplateChannels,
  previewNotifyTemplate,
  refreshNotifyTemplateCache,
  saveNotifyTemplateChannels,
  updateNotifyTemplateCustom
} from '@/api/system'

export default {
  name: 'NotifyTemplateManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      previewLoading: false,
      total: 0,
      tableData: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        templateCode: '',
        templateName: '',
        templateSource: ''
      },
      dialogVisible: false,
      previewVisible: false,
      dialogTitle: '',
      formReadonly: false,
      form: {},
      previewPayload: {
        variablesText: ''
      },
      previewResult: null,
      channelDialog: {
        visible: false,
        readonly: false,
        loading: false,
        templateCode: '',
        rows: []
      },
      routeTypeOptions: [
        { label: '工单详情', value: 'WORK_ORDER_DETAIL' },
        { label: '工单评价', value: 'WORK_ORDER_EVALUATE' }
      ],
      rules: {
        templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
        notifyEnabled: [{ required: true, message: '请选择通知开关', trigger: 'change' }],
        overrideEnabled: [{ required: true, message: '请选择覆盖开关', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listNotifyTemplate(this.queryParams).then(res => {
        if (!res) return
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
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
        templateCode: '',
        templateName: '',
        templateSource: ''
      }
      this.getList()
    },
    hasCustomTemplate(templateCode) {
      return this.tableData.some(item => item.templateCode === templateCode && item.templateSource === 'CUSTOM')
    },
    handleView(row) {
      this.dialogTitle = '查看通知模板'
      this.formReadonly = true
      this.dialogVisible = true
      getNotifyTemplate(row.id).then(res => {
        if (!res) return
        this.form = res.data || {}
      })
    },
    handleAdd(row) {
      this.dialogTitle = '新增自定义模板'
      this.formReadonly = false
      this.form = {
        templateCode: row.templateCode,
        templateName: row.templateName,
        notifyEnabled: 1,
        overrideEnabled: 0,
        routeType: row.routeType || '',
        titleTemplate: '',
        summaryTemplate: '',
        routeValueTemplate: '',
        remark: '',
        variablesJson: row.variablesJson || ''
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑自定义模板'
      this.formReadonly = false
      getNotifyTemplate(row.id).then(res => {
        if (!res) return
        this.form = res.data || {}
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateNotifyTemplateCustom : addNotifyTemplateCustom
        api(this.buildSubmitPayload()).then(res => {
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
      this.$confirm(`确认删除自定义模板“${row.templateName}”吗？删除后会回退到内置模板。`, '提示', { type: 'warning' })
        .then(() => {
          deleteNotifyTemplateCustom(row.id).then(res => {
            if (!res) return
            this.$message.success('删除成功')
            this.getList()
          })
        })
        .catch(() => {})
    },
    handlePreviewRow(row) {
      this.previewVisible = true
      this.previewPayload = {
        templateCode: row.templateCode,
        notifyEnabled: row.notifyEnabled,
        overrideEnabled: row.overrideEnabled,
        routeType: row.routeType,
        titleTemplate: row.titleTemplate,
        summaryTemplate: row.summaryTemplate,
        routeValueTemplate: row.routeValueTemplate,
        variablesText: this.buildPreviewVariables(row.templateCode)
      }
      this.previewResult = null
      this.loadPreview()
    },
    handlePreviewForm() {
      this.previewVisible = true
      this.previewPayload = Object.assign({}, this.buildSubmitPayload(), {
        variablesText: this.previewPayload.variablesText || this.buildPreviewVariables(this.form.templateCode)
      })
      this.previewResult = null
      this.loadPreview()
    },
    loadPreview() {
      let variables = {}
      try {
        variables = this.previewPayload.variablesText ? JSON.parse(this.previewPayload.variablesText) : {}
      } catch (e) {
        this.$message.error('示例变量 JSON 格式不正确')
        return
      }
      this.previewLoading = true
      previewNotifyTemplate({
        templateCode: this.previewPayload.templateCode,
        notifyEnabled: this.previewPayload.notifyEnabled,
        overrideEnabled: this.previewPayload.overrideEnabled,
        routeType: this.previewPayload.routeType,
        titleTemplate: this.previewPayload.titleTemplate,
        summaryTemplate: this.previewPayload.summaryTemplate,
        routeValueTemplate: this.previewPayload.routeValueTemplate,
        variables
      }).then(res => {
        if (!res) return
        this.previewResult = res.data
      }).finally(() => {
        this.previewLoading = false
      })
    },
    handleRefreshCache() {
      refreshNotifyTemplateCache().then(res => {
        if (!res) return
        this.$message.success('缓存刷新成功')
      })
    },
    handleChannels(row) {
      const openDialog = (templateCode) => {
        if (!templateCode) {
          this.$message.error('未获取到模板编码，请刷新后重试')
          return
        }
        const perms = (this.$store && this.$store.getters && this.$store.getters.perms) || []
        this.channelDialog.templateCode = templateCode
        this.channelDialog.visible = true
        this.channelDialog.readonly = !perms.includes('system:notifyTemplate:update')
        this.channelDialog.loading = false
        this.channelDialog.rows = []
        listNotifyTemplateChannels(templateCode).then(res => {
          if (!res) return
          const rows = res.data || []
          this.channelDialog.rows = rows.length ? rows.map(item => this.normalizeChannelRow(item)) : this.defaultChannelRows(templateCode)
        })
      }
      if (row && row.id) {
        getNotifyTemplate(row.id).then(res => {
          if (!res) return
          const detail = res.data || {}
          const templateCode = detail.templateCode || (row.templateCode || '')
          openDialog(templateCode)
        })
        return
      }
      const templateCode = row && row.templateCode ? row.templateCode : ''
      if (templateCode) {
        openDialog(templateCode)
        return
      }
      this.$message.error('未获取到模板信息，请刷新后重试')
    },
    saveChannels() {
      if (!this.channelDialog.templateCode) {
        this.$message.error('模板编码为空，请关闭弹窗后重新进入')
        return
      }
      this.channelDialog.loading = true
      saveNotifyTemplateChannels(this.channelDialog.templateCode, this.channelDialog.rows.map(item => this.buildChannelPayload(item)))
        .then(res => {
          if (!res) return
          this.$message.success('渠道配置保存成功')
          this.channelDialog.visible = false
        })
        .finally(() => {
          this.channelDialog.loading = false
        })
    },
    addChannelRow() {
      this.channelDialog.rows.push(this.createChannelRow(this.channelDialog.templateCode))
    },
    removeChannelRow(index) {
      this.channelDialog.rows.splice(index, 1)
    },
    addFieldMapping(item) {
      if (!item.fieldMapping) {
        this.$set(item, 'fieldMapping', [])
      }
      item.fieldMapping.push({ field: '', value: '' })
    },
    removeFieldMapping(item, index) {
      item.fieldMapping.splice(index, 1)
    },
    buildSubmitPayload() {
      return {
        id: this.form.id,
        templateCode: this.form.templateCode,
        templateName: this.form.templateName,
        notifyEnabled: this.form.notifyEnabled,
        overrideEnabled: this.form.overrideEnabled,
        routeType: this.form.routeType,
        titleTemplate: this.form.titleTemplate,
        summaryTemplate: this.form.summaryTemplate,
        routeValueTemplate: this.form.routeValueTemplate,
        remark: this.form.remark
      }
    },
    buildChannelPayload(item) {
      return {
        id: item.id,
        templateCode: this.channelDialog.templateCode,
        channelType: item.channelType,
        channelEnabled: item.channelEnabled,
        channelScene: item.channelScene,
        templateId: item.templateId,
        pagePathTemplate: item.pagePathTemplate,
        fieldMapping: (item.fieldMapping || []).map(mapping => ({
          field: mapping.field,
          value: mapping.value
        })),
        remark: item.remark
      }
    },
    normalizeChannelRow(item) {
      return {
        id: item.id,
        channelType: item.channelType || 'MP_SUBSCRIBE',
        channelEnabled: item.channelEnabled == null ? 1 : item.channelEnabled,
        channelScene: item.channelScene || '',
        templateId: item.templateId || '',
        pagePathTemplate: item.pagePathTemplate || '',
        fieldMapping: (item.fieldMapping || []).map(mapping => ({
          field: mapping.field || '',
          value: mapping.value || ''
        })),
        remark: item.remark || ''
      }
    },
    defaultChannelRows(templateCode) {
      return [this.createChannelRow(templateCode)]
    },
    createChannelRow(templateCode) {
      if (templateCode === 'WORK_ORDER_EVALUATION_INVITE') {
        return {
          channelType: 'MP_SUBSCRIBE',
          channelEnabled: 1,
          channelScene: 'C',
          templateId: '',
          pagePathTemplate: 'pages/order/evaluate?workOrderId=${workOrderId}',
          fieldMapping: [
            { field: 'thing1', value: '${orderNo}' },
            { field: 'phone_number2', value: '${customerMobile}' },
            { field: 'thing3', value: '${companyName}' }
          ],
          remark: '客户满意度评价通知默认渠道配置'
        }
      }
      return {
        channelType: 'MP_SUBSCRIBE',
        channelEnabled: 1,
        channelScene: '',
        templateId: '',
        pagePathTemplate: '',
        fieldMapping: [],
        remark: ''
      }
    },
    buildPreviewVariables(templateCode) {
      if (templateCode === 'WORK_ORDER_EVALUATION_INVITE') {
        return JSON.stringify({
          workOrderId: 10001,
          orderNo: 'WO202604210001',
          customerId: 5001,
          customerMobile: '13800138000',
          customerOpenid: 'openid-demo',
          companyId: 3001,
          companyName: '深圳南山服务网点',
          closedTime: '2026-04-21 15:30:00'
        }, null, 2)
      }
      return JSON.stringify({
        bizId: 88,
        bizNo: 'WO202604180001',
        receiverId: 1001,
        receiverName: '张三',
        operatorId: 2001,
        oldAssignedUserId: null,
        newAssignedUserId: 1001,
        assignType: 'ASSIGN',
        operationId: 'assign-op-001'
      }, null, 2)
    },
    formatVariables(variablesJson) {
      if (!variablesJson) return ''
      try {
        return JSON.stringify(JSON.parse(variablesJson), null, 2)
      } catch (e) {
        return variablesJson
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.table-toolbar { margin-bottom: 12px; }
.preview-toolbar { margin-bottom: 12px; }
.preview-error { color: #F56C6C; line-height: 22px; }
.channel-toolbar { margin-bottom: 12px; }
.channel-card {
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #FAFAFA;
}
.channel-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}
.mapping-toolbar {
  margin-bottom: 8px;
}
.channel-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}
</style>
