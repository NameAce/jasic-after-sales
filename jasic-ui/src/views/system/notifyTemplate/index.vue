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
        <el-button icon="el-icon-refresh" size="small" v-hasPerms="['system:notifyTemplate:refresh']" @click="handleRefreshCache">刷新缓存</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="模板编码" prop="templateCode" width="190" />
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
        <el-table-column label="跳转类型" prop="routeType" width="160" />
        <el-table-column label="标题模板" prop="titleTemplate" min-width="220" show-overflow-tooltip />
        <el-table-column label="摘要模板" prop="summaryTemplate" min-width="260" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:view']" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:preview']" @click="handlePreviewRow(row)">预览</el-button>
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
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
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
            <el-form-item label="通知总开关" prop="notifyEnabled">
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
        <el-form-item label="跳转类型">
          <el-select v-model="form.routeType" :disabled="formReadonly" placeholder="请选择跳转类型" clearable style="width: 100%;">
            <el-option v-for="item in routeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板">
          <el-input v-model="form.titleTemplate" :disabled="formReadonly" placeholder="为空则回退内置模板" />
        </el-form-item>
        <el-form-item label="摘要模板">
          <el-input v-model="form.summaryTemplate" :disabled="formReadonly" type="textarea" :rows="3" placeholder="为空则回退内置模板" />
        </el-form-item>
        <el-form-item label="跳转值模板">
          <el-input v-model="form.routeValueTemplate" :disabled="formReadonly" placeholder="例如 ${bizId}" />
        </el-form-item>
        <el-form-item label="变量说明">
          <el-input :value="formatVariables(form.variablesJson)" type="textarea" :rows="5" disabled />
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

    <el-dialog title="模板预览" :visible.sync="previewVisible" width="760px" append-to-body>
      <el-form label-width="110px">
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
        <el-descriptions-item label="跳转类型">{{ previewResult.routeType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跳转值">{{ previewResult.routeValue || '-' }}</el-descriptions-item>
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
  previewNotifyTemplate,
  refreshNotifyTemplateCache,
  updateNotifyTemplateCustom
} from '@/api/system'

const DEFAULT_PREVIEW_VARIABLES = JSON.stringify({
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
        variablesText: DEFAULT_PREVIEW_VARIABLES
      },
      previewResult: null,
      routeTypeOptions: [
        { label: '工单详情', value: 'WORK_ORDER_DETAIL' }
      ],
      rules: {
        templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
        notifyEnabled: [{ required: true, message: '请选择通知总开关', trigger: 'change' }],
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
      this.$confirm(`确认删除自定义模板"${row.templateName}"吗？删除后将回退到内置模板。`, '提示', { type: 'warning' }).then(() => {
        deleteNotifyTemplateCustom(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
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
        variablesText: DEFAULT_PREVIEW_VARIABLES
      }
      this.previewResult = null
      this.loadPreview()
    },
    handlePreviewForm() {
      this.previewVisible = true
      this.previewPayload = Object.assign({}, this.buildSubmitPayload(), {
        variablesText: this.previewPayload.variablesText || DEFAULT_PREVIEW_VARIABLES
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
</style>
