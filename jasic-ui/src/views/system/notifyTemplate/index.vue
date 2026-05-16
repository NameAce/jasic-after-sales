<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="通知场景" prop="sceneCode">
          <el-select v-model="queryParams.sceneCode" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in sceneOptions"
              :key="item.sceneCode"
              :label="item.sceneName"
              :value="item.sceneCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知类型" prop="notifyType">
          <el-select v-model="queryParams.notifyType" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in notifyTypeOptions"
              :key="item.code"
              :label="item.desc"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div class="table-toolbar">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="small"
          v-hasPerms="['system:notifyTemplate:add']"
          @click="handleAdd"
        >
          新增模板
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="通知场景" min-width="240" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <div>{{ row.sceneName || getSceneName(row.sceneCode) || row.sceneCode || '-' }}</div>
            <div class="muted-text">{{ row.sceneCode || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="通知类型" min-width="160" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.notifyTypeDesc || getNotifyTypeDesc(row.sceneCode) || row.notifyType || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="接收对象" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <div>{{ row.receiverTypeDesc || getReceiverTypeDesc(row.sceneCode) || '-' }}</div>
            <div class="muted-text">{{ row.receiverDesc || getReceiverDesc(row.sceneCode) || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="模板名称" prop="templateName" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" width="320" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:view']" @click="handleView(row)">
              查看
            </el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:update']" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:notifyTemplate:preview']" @click="handlePreviewRow(row)">
              预览
            </el-button>
            <el-button
              v-if="supportsChannelConfig(row)"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTemplate:view']"
              @click="handleChannels(row)"
            >
              渠道配置
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="text"
              size="mini"
              v-hasPerms="['system:notifyTemplate:remove']"
              @click="handleStatusChange(row, 1)"
            >
              启用
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="text"
              size="mini"
              class="danger-action"
              v-hasPerms="['system:notifyTemplate:remove']"
              @click="handleStatusChange(row, 0)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handlePageNumChange"
      />
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="980px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <div class="section-title">基础信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="通知场景" prop="sceneCode">
              <el-select
                v-model="form.sceneCode"
                :disabled="dialogReadonly"
                placeholder="请选择通知场景"
                filterable
                style="width: 100%;"
                @change="handleSceneChange"
              >
                <el-option
                  v-for="item in sceneOptions"
                  :key="item.sceneCode"
                  :label="item.sceneName"
                  :value="item.sceneCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" :disabled="dialogReadonly" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态" prop="status">
              <el-radio-group v-model="form.status" :disabled="dialogReadonly">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" :disabled="dialogReadonly" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">辅助信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="通知类型">
              <el-input :value="currentSceneMeta ? currentSceneMeta.notifyTypeDesc : '-'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接收对象">
              <el-input :value="buildReceiverDisplay(currentSceneMeta)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="触发说明">
              <div class="read-only-block">{{ currentTriggerHint }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">模板内容</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题模板">
              <el-input v-model="form.titleTemplate" :disabled="dialogReadonly" placeholder="请输入标题模板" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="跳转类型">
              <el-select v-model="form.routeType" :disabled="dialogReadonly" placeholder="请选择跳转类型" clearable style="width: 100%;">
                <el-option
                  v-for="item in routeTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容模板">
              <el-input
                v-model="form.contentTemplate"
                :disabled="dialogReadonly"
                type="textarea"
                :rows="4"
                placeholder="请输入内容模板"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跳转值模板">
              <el-input
                v-model="form.routeValueTemplate"
                :disabled="dialogReadonly"
                placeholder="例如 ${workOrderId}"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">可用变量</div>
        <el-table :data="currentVariableList" size="mini" border>
          <el-table-column label="变量名" prop="name" min-width="180" />
          <el-table-column label="说明" prop="desc" min-width="220" />
          <el-table-column label="示例值" prop="example" min-width="220" show-overflow-tooltip />
        </el-table>

        <div class="section-title">预览测试</div>
        <div class="preview-entry">
          <div class="preview-entry__desc">
            使用当前模板内容和场景示例变量，快速校验标题、内容和跳转值的渲染结果。
          </div>
          <el-button
            type="primary"
            plain
            size="small"
            v-hasPerms="['system:notifyTemplate:preview']"
            @click="handlePreviewForm"
          >
            打开预览测试
          </el-button>
        </div>

        <div class="section-title">渠道配置</div>
        <div class="preview-entry">
          <div class="preview-entry__desc">
            <template v-if="currentSceneSupportsChannel">
              小程序订阅消息的渠道参数单独按通知场景维护。请先保存模板，再进入渠道配置页维护微信模板 ID、跳转页面模板和字段映射。
            </template>
            <template v-else>
              当前场景为站内待办，不展示渠道配置入口。
            </template>
          </div>
          <el-button
            v-if="currentSceneSupportsChannel"
            plain
            size="small"
            :disabled="!form.id"
            v-hasPerms="['system:notifyTemplate:view']"
            @click="handleChannels(form)"
          >
            打开渠道配置
          </el-button>
        </div>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">{{ dialogReadonly ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!dialogReadonly"
          type="primary"
          :loading="submitLoading"
          @click="submitForm"
        >
          确定
        </el-button>
      </div>
    </el-dialog>

    <el-dialog :title="channelDialog.title" :visible.sync="channelDialog.visible" width="960px" append-to-body>
      <div v-if="!channelDialog.supported" class="empty-tip">
        当前通知场景不支持外部渠道配置。
      </div>

      <template v-else>
        <el-form :model="channelDialog.form" label-width="120px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="通知场景">
                <el-input :value="channelDialog.sceneName || '-'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="通知类型">
                <el-input :value="channelDialog.notifyTypeDesc || '-'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="渠道类型">
                <el-input :value="channelDialog.channelTypeDesc || channelDialog.channelType || '-'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="渠道启用状态">
                <el-radio-group v-model="channelDialog.form.channelEnabled" :disabled="channelDialog.readonly">
                  <el-radio :label="1">启用</el-radio>
                  <el-radio :label="0">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="微信模板 ID">
                <el-input
                  v-model="channelDialog.form.templateId"
                  :disabled="channelDialog.readonly"
                  placeholder="请输入小程序订阅消息模板 ID"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发送小程序">
                <el-select
                  v-model="channelDialog.form.channelScene"
                  :disabled="channelDialog.readonly"
                  placeholder="请选择发送小程序"
                  clearable
                  style="width: 100%;"
                >
                  <el-option
                    v-for="item in channelSceneOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="跳转页面模板">
                <el-input
                  v-model="channelDialog.form.pagePathTemplate"
                  :disabled="channelDialog.readonly"
                  placeholder="例如 pages/order/evaluate?workOrderId=${workOrderId}"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="字段映射">
            <div class="mapping-toolbar" v-if="!channelDialog.readonly">
              <el-button type="primary" plain size="mini" @click="addFieldMapping(channelDialog.form)">新增字段</el-button>
            </div>
            <el-table :data="channelDialog.form.fieldMapping" size="mini" border>
              <el-table-column label="模板字段" min-width="180">
                <template slot-scope="{ row: mapping }">
                  <el-input v-model="mapping.field" :disabled="channelDialog.readonly" placeholder="例如 thing1" />
                </template>
              </el-table-column>
              <el-table-column label="变量表达式" min-width="260">
                <template slot-scope="{ row: mapping }">
                  <el-input v-model="mapping.value" :disabled="channelDialog.readonly" placeholder="例如 ${orderNo}" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template slot-scope="{ $index: mappingIndex }">
                  <el-button
                    v-if="!channelDialog.readonly"
                    type="text"
                    size="mini"
                    class="danger-action"
                    @click="removeFieldMapping(channelDialog.form, mappingIndex)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="field-tip">字段映射优先通过表格维护，避免直接编辑 JSON 造成格式错误。</div>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="channelDialog.form.remark" :disabled="channelDialog.readonly" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
      </template>

      <div slot="footer">
        <el-button @click="channelDialog.visible = false">关闭</el-button>
        <el-button
          v-if="channelDialog.supported && !channelDialog.readonly"
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
        <el-form-item label="通知场景">
          <el-input :value="previewPayload.sceneName || '-'" disabled />
        </el-form-item>
        <el-form-item label="示例变量 JSON">
          <el-input v-model="previewPayload.variablesText" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <div class="preview-toolbar">
        <el-button type="primary" :loading="previewLoading" @click="loadPreview">执行预览</el-button>
      </div>
      <el-descriptions v-if="previewResult" :column="1" border size="small">
        <el-descriptions-item label="标题">{{ previewResult.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ previewResult.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路由类型">{{ formatRouteType(previewResult.routeType) }}</el-descriptions-item>
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
  createNotifyTemplate,
  getNotifyTemplate,
  getNotifyTemplateOptions,
  listNotifyTemplate,
  listNotifyTemplateChannels,
  previewNotifyTemplate,
  saveNotifyTemplateChannels,
  updateNotifyTemplate,
  updateNotifyTemplateStatus
} from '@/api/system'

const MP_SUBSCRIBE = 'MP_SUBSCRIBE'

const ROUTE_TYPE_OPTIONS = [
  { label: '工单详情', value: 'WORK_ORDER_DETAIL' },
  { label: '工单评价', value: 'WORK_ORDER_EVALUATE' }
]

const CHANNEL_SCENE_OPTIONS = [
  { label: 'B端小程序', value: 'B' },
  { label: 'C端小程序', value: 'C' }
]

const SCENE_TRIGGER_HINT_MAP = {
  WORK_ORDER_ASSIGNED_TODO: '工单派单后，系统会给当前维修员生成一条站内待办。',
  WORK_ORDER_EVALUATION_INVITE_MP_C: '工单完成后，系统会给 C 端客户发送一条评价邀请小程序订阅消息。'
}

function buildEmptyTemplateOptions() {
  return {
    sceneOptions: [],
    channelTypeOptions: []
  }
}

function buildDefaultQuery() {
  return {
    pageNum: 1,
    pageSize: 10,
    sceneCode: '',
    notifyType: '',
    status: '',
    templateName: ''
  }
}

function buildEmptyTemplateForm() {
  return {
    id: null,
    sceneCode: '',
    sceneName: '',
    templateName: '',
    titleTemplate: '',
    contentTemplate: '',
    routeType: '',
    routeValueTemplate: '',
    status: 1,
    remark: ''
  }
}

function createFieldMapping() {
  return {
    field: '',
    value: ''
  }
}

function buildEmptyChannelForm(channelType) {
  return {
    id: null,
    targetType: '',
    channelType: channelType || MP_SUBSCRIBE,
    channelEnabled: 1,
    templateId: '',
    channelScene: '',
    pagePathTemplate: '',
    fieldMapping: [createFieldMapping()],
    remark: ''
  }
}

function buildEmptyChannelDialog() {
  return {
    visible: false,
    readonly: false,
    loading: false,
    supported: false,
    title: '渠道配置',
    sceneCode: '',
    sceneName: '',
    notifyTypeDesc: '',
    channelType: '',
    channelTypeDesc: '',
    form: buildEmptyChannelForm()
  }
}

export default {
  name: 'NotifyTemplateManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      previewLoading: false,
      optionsLoading: false,
      optionsLoaded: false,
      total: 0,
      tableData: [],
      queryParams: buildDefaultQuery(),
      templateOptions: buildEmptyTemplateOptions(),
      routeTypeOptions: ROUTE_TYPE_OPTIONS,
      dialogVisible: false,
      dialogMode: 'view',
      dialogTitle: '查看通知模板配置',
      form: buildEmptyTemplateForm(),
      previewVisible: false,
      previewPayload: {
        sceneCode: '',
        sceneName: '',
        titleTemplate: '',
        contentTemplate: '',
        routeType: '',
        routeValueTemplate: '',
        variablesText: ''
      },
      previewResult: null,
      channelDialog: buildEmptyChannelDialog(),
      rules: {
        sceneCode: [{ required: true, message: '请选择通知场景', trigger: 'change' }],
        templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogReadonly() {
      return this.dialogMode === 'view'
    },
    sceneOptions() {
      return this.templateOptions.sceneOptions || []
    },
    notifyTypeOptions() {
      const optionMap = new Map()
      this.sceneOptions.forEach(item => {
        if (!item || !item.notifyType) {
          return
        }
        if (!optionMap.has(item.notifyType)) {
          optionMap.set(item.notifyType, {
            code: item.notifyType,
            desc: item.notifyTypeDesc || item.notifyType
          })
        }
      })
      return Array.from(optionMap.values())
    },
    channelSceneOptions() {
      return CHANNEL_SCENE_OPTIONS
    },
    currentSceneMeta() {
      return this.getSceneMeta(this.form.sceneCode)
    },
    currentVariableList() {
      return this.currentSceneMeta && Array.isArray(this.currentSceneMeta.variables)
        ? this.currentSceneMeta.variables
        : []
    },
    currentTriggerHint() {
      return this.getTriggerHint(this.form.sceneCode)
    },
    currentSceneSupportsChannel() {
      return Boolean(this.currentSceneMeta && this.currentSceneMeta.channelType)
    }
  },
  created() {
    this.loadOptions()
    this.getList()
  },
  methods: {
    /**
     * 查询模板列表。
     *
     * 列表筛选已经收口为通知场景、通知类型、状态和模板名称，
     * 这里不再向后端提交任何旧组合字段。
     */
    getList() {
      this.loading = true
      listNotifyTemplate(this.buildQueryParams()).then(res => {
        if (!res) {
          return
        }
        const data = res.data || {}
        this.tableData = data.records || []
        this.total = Number(data.total || 0)
      }).finally(() => {
        this.loading = false
      })
    },
    /**
     * 读取页面元数据。
     *
     * 通知场景、通知类型说明、接收对象说明和变量清单统一来自后端注册表，
     * 前端只做只读展示和默认值回填。
     */
    loadOptions() {
      this.optionsLoading = true
      return getNotifyTemplateOptions().then(res => {
        if (!res) {
          return
        }
        this.templateOptions = Object.assign(buildEmptyTemplateOptions(), res.data || {})
        this.optionsLoaded = true
      }).finally(() => {
        this.optionsLoading = false
      })
    },
    ensureTemplateOptions() {
      if (this.optionsLoaded) {
        return Promise.resolve()
      }
      return this.loadOptions()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = buildDefaultQuery()
      this.getList()
    },
    handlePageSizeChange(pageSize) {
      this.queryParams.pageSize = pageSize
      this.getList()
    },
    handlePageNumChange(pageNum) {
      this.queryParams.pageNum = pageNum
      this.getList()
    },
    buildQueryParams() {
      const params = Object.assign({}, this.queryParams)
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] == null) {
          delete params[key]
        }
      })
      return params
    },
    handleAdd() {
      this.ensureTemplateOptions().then(() => {
        this.dialogMode = 'add'
        this.dialogTitle = '新增通知模板配置'
        this.form = buildEmptyTemplateForm()
        this.dialogVisible = true
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate()
          }
        })
      })
    },
    handleView(row) {
      this.openDetailDialog(row.id, 'view', '查看通知模板配置')
    },
    handleEdit(row) {
      this.openDetailDialog(row.id, 'edit', '编辑通知模板配置')
    },
    /**
     * 打开详情弹窗。
     *
     * 详情接口只返回模板主数据，场景辅助信息依旧以 sceneCode 为索引
     * 从本页元数据里补齐，避免接口重新回填旧字段。
     */
    openDetailDialog(id, mode, title) {
      this.ensureTemplateOptions().then(() => {
        getNotifyTemplate(id).then(res => {
          if (!res) {
            return
          }
          this.dialogMode = mode
          this.dialogTitle = title
          this.form = this.normalizeTemplateForm(res.data)
          this.dialogVisible = true
          this.$nextTick(() => {
            if (this.$refs.form) {
              this.$refs.form.clearValidate()
            }
          })
        })
      })
    },
    normalizeTemplateForm(data) {
      const form = Object.assign(buildEmptyTemplateForm(), {
        id: data && data.id ? data.id : null,
        sceneCode: data && data.sceneCode ? data.sceneCode : '',
        sceneName: data && data.sceneName ? data.sceneName : '',
        templateName: data && data.templateName ? data.templateName : '',
        titleTemplate: data && data.titleTemplate ? data.titleTemplate : '',
        contentTemplate: data && data.contentTemplate ? data.contentTemplate : '',
        routeType: data && data.routeType ? data.routeType : '',
        routeValueTemplate: data && data.routeValueTemplate ? data.routeValueTemplate : '',
        status: data && typeof data.status === 'number' ? data.status : 1,
        remark: data && data.remark ? data.remark : ''
      })
      const sceneMeta = this.getSceneMeta(form.sceneCode)
      if (sceneMeta && !form.sceneName) {
        form.sceneName = sceneMeta.sceneName
      }
      return form
    },
    /**
     * 处理通知场景切换。
     *
     * 新增场景时需要把默认模板名称、默认标题、默认内容和默认跳转配置回填进表单，
     * 让维护人员围绕场景直接修改，不再手工拼旧组合字段。
     */
    handleSceneChange(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      if (!sceneMeta) {
        return
      }
      this.form.sceneName = sceneMeta.sceneName

      if (this.dialogMode === 'add') {
        this.form.templateName = sceneMeta.defaultTemplateName || ''
        this.form.titleTemplate = sceneMeta.defaultTitleTemplate || ''
        this.form.contentTemplate = sceneMeta.defaultContentTemplate || ''
        this.form.routeType = sceneMeta.defaultRouteType || ''
        this.form.routeValueTemplate = sceneMeta.defaultRouteValueTemplate || ''
        return
      }

      if (!this.form.templateName) {
        this.form.templateName = sceneMeta.defaultTemplateName || ''
      }
      if (!this.form.titleTemplate) {
        this.form.titleTemplate = sceneMeta.defaultTitleTemplate || ''
      }
      if (!this.form.contentTemplate) {
        this.form.contentTemplate = sceneMeta.defaultContentTemplate || ''
      }
      if (!this.form.routeType) {
        this.form.routeType = sceneMeta.defaultRouteType || ''
      }
      if (!this.form.routeValueTemplate) {
        this.form.routeValueTemplate = sceneMeta.defaultRouteValueTemplate || ''
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        const request = this.form.id ? updateNotifyTemplate : createNotifyTemplate
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
    },
    buildSubmitPayload() {
      return {
        id: this.form.id,
        sceneCode: this.trimValue(this.form.sceneCode),
        templateName: this.trimValue(this.form.templateName),
        titleTemplate: this.trimValue(this.form.titleTemplate),
        contentTemplate: this.trimValue(this.form.contentTemplate),
        routeType: this.trimValue(this.form.routeType),
        routeValueTemplate: this.trimValue(this.form.routeValueTemplate),
        status: this.form.status,
        remark: this.trimValue(this.form.remark)
      }
    },
    handleStatusChange(row, status) {
      const actionText = status === 1 ? '启用' : '停用'
      this.$confirm(`确认${actionText}模板“${row.templateName}”吗？`, '提示', { type: 'warning' })
        .then(() => {
          // 模板启停单独走状态接口，避免编辑接口顺带切状态造成操作语义不清。
          return updateNotifyTemplateStatus(row.id, { status })
        })
        .then(res => {
          if (!res) {
            return
          }
          this.$message.success(`${actionText}成功`)
          this.getList()
        })
        .catch(() => {})
    },
    handlePreviewRow(row) {
      this.ensureTemplateOptions().then(() => {
        this.openPreviewDialog(this.normalizeTemplateForm(row))
      })
    },
    handlePreviewForm() {
      this.ensureTemplateOptions().then(() => {
        this.openPreviewDialog(this.form)
      })
    },
    /**
     * 打开预览弹窗。
     *
     * 预览变量默认取当前场景注册表中的 example，先让维护人员看到标准渲染结果，
     * 再按需要覆盖 JSON 进行补充验证。
     */
    openPreviewDialog(template) {
      const previewTemplate = this.normalizeTemplateForm(template)
      if (!previewTemplate.sceneCode) {
        this.$message.error('请先选择通知场景')
        return
      }
      this.previewPayload = {
        sceneCode: previewTemplate.sceneCode,
        sceneName: previewTemplate.sceneName || this.getSceneName(previewTemplate.sceneCode),
        titleTemplate: previewTemplate.titleTemplate,
        contentTemplate: previewTemplate.contentTemplate,
        routeType: previewTemplate.routeType,
        routeValueTemplate: previewTemplate.routeValueTemplate,
        variablesText: this.buildPreviewVariablesText(previewTemplate.sceneCode)
      }
      this.previewResult = null
      this.previewVisible = true
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
        sceneCode: this.previewPayload.sceneCode,
        titleTemplate: this.trimValue(this.previewPayload.titleTemplate),
        contentTemplate: this.trimValue(this.previewPayload.contentTemplate),
        routeType: this.trimValue(this.previewPayload.routeType),
        routeValueTemplate: this.trimValue(this.previewPayload.routeValueTemplate),
        variables
      }).then(res => {
        if (!res) {
          return
        }
        this.previewResult = res.data || null
      }).finally(() => {
        this.previewLoading = false
      })
    },
    buildPreviewVariablesText(sceneCode) {
      const previewVariables = {}
      this.getSceneVariables(sceneCode).forEach(item => {
        if (!item || !item.name) {
          return
        }
        previewVariables[item.name] = item.example || ''
      })
      return JSON.stringify(previewVariables, null, 2)
    },
    /**
     * 打开渠道配置弹窗。
     *
     * 渠道配置已经按 sceneCode 维护，且一个场景当前只允许维护声明过的外部渠道。
     * 因此弹窗只展示单场景、单渠道的维护表单，不再暴露旧的渠道场景维度。
     */
    handleChannels(template) {
      this.ensureTemplateOptions().then(() => {
        const templateInfo = this.normalizeTemplateForm(template)
        const sceneMeta = this.getSceneMeta(templateInfo.sceneCode)
        const readonly = !this.hasPermission('system:notifyTemplate:update')
        const supported = Boolean(sceneMeta && sceneMeta.channelType)

        this.channelDialog = Object.assign(buildEmptyChannelDialog(), {
          visible: true,
          readonly,
          supported,
          title: `渠道配置 - ${templateInfo.templateName || templateInfo.sceneName || ''}`,
          sceneCode: templateInfo.sceneCode,
          sceneName: templateInfo.sceneName || (sceneMeta ? sceneMeta.sceneName : ''),
          notifyTypeDesc: sceneMeta ? sceneMeta.notifyTypeDesc : '',
          channelType: sceneMeta ? sceneMeta.channelType : '',
          channelTypeDesc: sceneMeta ? sceneMeta.channelTypeDesc : '',
          form: buildEmptyChannelForm(sceneMeta ? sceneMeta.channelType : '')
        })

        if (!supported) {
          return
        }

        listNotifyTemplateChannels(templateInfo.sceneCode).then(res => {
          if (!res) {
            return
          }
          const rows = res.data || []
          const currentRow = rows.length ? rows[0] : null
          this.channelDialog.form = this.normalizeChannelForm(currentRow, sceneMeta)
        })
      })
    },
    normalizeChannelForm(data, sceneMeta) {
      const form = buildEmptyChannelForm(sceneMeta ? sceneMeta.channelType : '')
      if (!data) {
        form.channelScene = sceneMeta && sceneMeta.channelScene ? sceneMeta.channelScene : ''
        return form
      }
      return Object.assign(form, {
        id: data.id || null,
        targetType: data.targetType || '',
        channelType: data.channelType || form.channelType,
        channelEnabled: typeof data.channelEnabled === 'number' ? data.channelEnabled : 1,
        templateId: data.templateId || '',
        channelScene: data.channelScene || (sceneMeta && sceneMeta.channelScene ? sceneMeta.channelScene : ''),
        pagePathTemplate: data.pagePathTemplate || '',
        fieldMapping: Array.isArray(data.fieldMapping) && data.fieldMapping.length
          ? data.fieldMapping.map(item => ({
            field: item.field || '',
            value: item.value || ''
          }))
          : [createFieldMapping()],
        remark: data.remark || ''
      })
    },
    addFieldMapping(form) {
      if (!form.fieldMapping) {
        this.$set(form, 'fieldMapping', [])
      }
      form.fieldMapping.push(createFieldMapping())
    },
    removeFieldMapping(form, index) {
      form.fieldMapping.splice(index, 1)
      if (!form.fieldMapping.length) {
        form.fieldMapping.push(createFieldMapping())
      }
    },
    saveChannels() {
      if (!this.validateChannelForm()) {
        return
      }
      this.channelDialog.loading = true

      // 渠道接口按 sceneCode 全量覆盖保存，这里始终提交当前弹窗里的完整单渠道快照。
      saveNotifyTemplateChannels(
        this.channelDialog.sceneCode,
        [this.buildChannelPayload(this.channelDialog.form)]
      ).then(res => {
        if (!res) {
          return
        }
        this.$message.success('渠道配置保存成功')
        this.channelDialog.visible = false
      }).finally(() => {
        this.channelDialog.loading = false
      })
    },
    /**
     * 校验渠道表单。
     *
     * 当前阶段只有小程序订阅消息进入渠道配置链路。
     * 只要渠道启用，就必须提前拦住模板 ID、跳转页面模板和字段映射缺失。
     */
    validateChannelForm() {
      const form = this.channelDialog.form
      if (!form || !this.trimValue(form.channelType)) {
        this.$message.error('缺少渠道类型信息')
        return false
      }
      if (form.channelEnabled !== 0 && form.channelEnabled !== 1) {
        this.$message.error('渠道状态只允许为启用或停用')
        return false
      }
      const fieldMapping = (form.fieldMapping || []).filter(item => this.trimValue(item.field) || this.trimValue(item.value))
      if (form.channelEnabled === 1) {
        if (!this.trimValue(form.templateId)) {
          this.$message.error('启用渠道时必须填写微信模板 ID')
          return false
        }
        if (!this.trimValue(form.channelScene)) {
          this.$message.error('启用渠道时必须选择发送小程序')
          return false
        }
        if (!this.trimValue(form.pagePathTemplate)) {
          this.$message.error('启用渠道时必须填写跳转页面模板')
          return false
        }
        if (!fieldMapping.length) {
          this.$message.error('启用渠道时必须至少配置一条字段映射')
          return false
        }
      }
      const invalidMapping = fieldMapping.find(item => !this.trimValue(item.field) || !this.trimValue(item.value))
      if (invalidMapping) {
        this.$message.error('字段映射中的模板字段和值都不能为空')
        return false
      }
      return true
    },
    buildChannelPayload(form) {
      return {
        id: form.id,
        targetType: this.trimValue(form.targetType),
        channelType: this.trimValue(form.channelType),
        channelEnabled: form.channelEnabled,
        templateId: this.trimValue(form.templateId),
        channelScene: this.trimValue(form.channelScene),
        pagePathTemplate: this.trimValue(form.pagePathTemplate),
        fieldMapping: (form.fieldMapping || [])
          .filter(item => this.trimValue(item.field) || this.trimValue(item.value))
          .map(item => ({
            field: this.trimValue(item.field),
            value: this.trimValue(item.value)
          })),
        remark: this.trimValue(form.remark)
      }
    },
    supportsChannelConfig(record) {
      const sceneMeta = this.getSceneMeta(record.sceneCode)
      return Boolean((sceneMeta && sceneMeta.channelType) || record.channelType)
    },
    getSceneMeta(sceneCode) {
      if (!sceneCode) {
        return null
      }
      return this.sceneOptions.find(item => item.sceneCode === sceneCode) || null
    },
    getSceneName(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      return sceneMeta ? sceneMeta.sceneName : ''
    },
    getNotifyTypeDesc(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      return sceneMeta ? sceneMeta.notifyTypeDesc : ''
    },
    getReceiverTypeDesc(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      return sceneMeta ? sceneMeta.receiverTypeDesc : ''
    },
    getReceiverDesc(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      return sceneMeta ? sceneMeta.receiverDesc : ''
    },
    getSceneVariables(sceneCode) {
      const sceneMeta = this.getSceneMeta(sceneCode)
      return sceneMeta && Array.isArray(sceneMeta.variables) ? sceneMeta.variables : []
    },
    getTriggerHint(sceneCode) {
      if (!sceneCode) {
        return '请选择通知场景后查看触发说明。'
      }
      return SCENE_TRIGGER_HINT_MAP[sceneCode] || '当前场景按后端注册表定义的发送链路触发。'
    },
    buildReceiverDisplay(sceneMeta) {
      if (!sceneMeta) {
        return '-'
      }
      const receiverParts = [sceneMeta.receiverTypeDesc, sceneMeta.receiverDesc].filter(Boolean)
      return receiverParts.length ? receiverParts.join(' / ') : '-'
    },
    formatRouteType(routeType) {
      if (!routeType) {
        return '-'
      }
      const matched = this.routeTypeOptions.find(item => item.value === routeType)
      return matched ? matched.label : routeType
    },
    hasPermission(permission) {
      const perms = (this.$store && this.$store.getters && this.$store.getters.perms) || []
      return perms.includes(permission)
    },
    trimValue(value) {
      if (value == null) {
        return null
      }
      const text = String(value).trim()
      return text ? text : null
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
}

.list-card {
  margin-top: 12px;
}

.table-toolbar {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}

.muted-text {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.section-title {
  margin: 18px 0 12px;
  font-weight: 600;
  color: #303133;
}

.field-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.read-only-block {
  min-height: 40px;
  padding: 9px 12px;
  color: #606266;
  line-height: 22px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.preview-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.preview-entry__desc {
  margin-right: 16px;
  color: #606266;
  line-height: 20px;
}

.preview-toolbar {
  margin-bottom: 12px;
}

.preview-error {
  color: #f56c6c;
  line-height: 22px;
}

.mapping-toolbar {
  margin-bottom: 8px;
}

.empty-tip {
  padding: 24px 12px;
  color: #909399;
  text-align: center;
  line-height: 22px;
}

.danger-action {
  color: #f56c6c;
}
</style>
