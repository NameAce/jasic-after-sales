<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" :inline="true" size="small">
        <el-form-item label="场景名称">
          <el-input v-model="queryParams.sceneName" placeholder="请输入场景名称" clearable />
        </el-form-item>
        <el-form-item label="场景编码">
          <el-input v-model="queryParams.sceneCode" placeholder="请输入场景编码" clearable />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="queryParams.bizType" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in bizTypeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知目标">
          <el-select v-model="queryParams.targetType" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in targetTypeOptions"
              :key="item.code"
              :label="item.desc"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="list-card">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column label="场景名称" min-width="220" show-overflow-tooltip prop="sceneName" />
        <el-table-column label="场景编码" min-width="220" show-overflow-tooltip prop="sceneCode" />
        <el-table-column label="业务类型" min-width="140" show-overflow-tooltip prop="bizType" />
        <el-table-column label="已启用目标" min-width="220">
          <template slot-scope="{ row }">
            <div v-if="row.enabledTargetTypeDescs && row.enabledTargetTypeDescs.length">
              <el-tag
                v-for="item in row.enabledTargetTypeDescs"
                :key="item"
                class="tag-item"
                size="mini"
                type="success"
              >
                {{ item }}
              </el-tag>
            </div>
            <span v-else class="muted-text">未启用</span>
          </template>
        </el-table-column>
        <el-table-column label="场景状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" prop="updateTime" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              v-hasPerms="['system:notifyScene:view']"
              @click="openDetailDialog(row.sceneCode, 'view')"
            >
              查看
            </el-button>
            <el-button
              type="text"
              size="mini"
              v-hasPerms="['system:notifyScene:update']"
              @click="openDetailDialog(row.sceneCode, 'edit')"
            >
              配置
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

    <el-dialog
      :title="dialogMode === 'view' ? '查看通知场景配置' : '编辑通知场景配置'"
      :visible.sync="dialogVisible"
      width="1100px"
      append-to-body
    >
      <el-form v-if="dialogForm" :model="dialogForm" label-width="120px">
        <div class="section-title">基础信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="场景名称">
              <el-input :value="dialogForm.sceneName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景编码">
              <el-input :value="dialogForm.sceneCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型">
              <el-input :value="dialogForm.bizType" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事件编码">
              <el-input :value="dialogForm.eventCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景状态">
              <el-radio-group v-model="dialogForm.status" :disabled="dialogReadonly">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景备注">
              <el-input v-model="dialogForm.remark" :disabled="dialogReadonly" placeholder="请输入场景备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">通知目标配置</div>
        <el-form-item label="启用目标">
          <el-checkbox-group v-model="checkedTargetTypes" :disabled="dialogReadonly" @change="handleCheckedTargetsChange">
            <el-checkbox
              v-for="item in currentSceneTargetMetas"
              :key="item.targetType"
              :label="item.targetType"
            >
              {{ item.targetTypeDesc }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div
          v-for="targetMeta in currentSceneTargetMetas"
          v-show="checkedTargetTypes.includes(targetMeta.targetType)"
          :key="targetMeta.targetType"
          class="target-card"
        >
          <div class="target-card__header">
            <div>
              <div class="target-card__title">{{ targetMeta.targetTypeDesc }}</div>
              <div class="target-card__sub">
                {{ buildReceiverDesc(targetMeta) }}
              </div>
            </div>
            <div class="target-card__actions">
              <el-switch
                v-model="getTargetForm(targetMeta.targetType).enabled"
                :active-value="1"
                :inactive-value="0"
                :disabled="dialogReadonly"
                active-text="启用"
                inactive-text="停用"
              />
              <el-button
                type="primary"
                plain
                size="mini"
                v-hasPerms="['system:notifyScene:preview']"
                @click="openPreviewDialog(targetMeta)"
              >
                预览
              </el-button>
            </div>
          </div>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="标题模板">
                <el-input
                  v-model="getTargetForm(targetMeta.targetType).titleTemplate"
                  :disabled="dialogReadonly"
                  placeholder="请输入标题模板"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="跳转类型">
                <el-select
                  v-model="getTargetForm(targetMeta.targetType).routeType"
                  :disabled="dialogReadonly"
                  placeholder="请选择跳转类型"
                  clearable
                  style="width: 100%;"
                >
                  <el-option
                    v-for="item in routeTypeOptions"
                    :key="item.code"
                    :label="item.desc"
                    :value="item.code"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="内容模板">
                <el-input
                  v-model="getTargetForm(targetMeta.targetType).contentTemplate"
                  :disabled="dialogReadonly"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入内容模板"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="跳转值模板">
                <el-input
                  v-model="getTargetForm(targetMeta.targetType).routeValueTemplate"
                  :disabled="dialogReadonly"
                  placeholder="例如 ${workOrderId}"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <template v-if="targetMeta.targetType === 'MP_SUBSCRIBE'">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="模板ID">
                  <el-input
                    v-model="getTargetForm(targetMeta.targetType).templateId"
                    :disabled="dialogReadonly"
                    placeholder="请输入小程序订阅消息模板ID"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="发送小程序">
                  <el-select
                    v-model="getTargetForm(targetMeta.targetType).channelScene"
                    :disabled="dialogReadonly"
                    placeholder="请选择发送小程序"
                    clearable
                    style="width: 100%;"
                  >
                    <el-option
                      v-for="item in channelSceneOptions"
                      :key="item.code"
                      :label="item.desc"
                      :value="item.code"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="页面路径模板">
                  <el-input
                    v-model="getTargetForm(targetMeta.targetType).pagePathTemplate"
                    :disabled="dialogReadonly"
                    placeholder="例如 pages/order/detail?workOrderId=${workOrderId}"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="字段映射">
              <div class="mapping-toolbar" v-if="!dialogReadonly">
                <el-button type="primary" plain size="mini" @click="addFieldMapping(targetMeta.targetType)">新增字段</el-button>
              </div>
              <el-table :data="getTargetForm(targetMeta.targetType).fieldMapping" size="mini" border>
                <el-table-column label="模板字段" min-width="180">
                  <template slot-scope="{ row }">
                    <el-input v-model="row.field" :disabled="dialogReadonly" placeholder="例如 thing1" />
                  </template>
                </el-table-column>
                <el-table-column label="变量表达式" min-width="240">
                  <template slot-scope="{ row }">
                    <el-input v-model="row.value" :disabled="dialogReadonly" placeholder="例如 ${orderNo}" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="90" align="center">
                  <template slot-scope="{ $index }">
                    <el-button
                      v-if="!dialogReadonly"
                      type="text"
                      size="mini"
                      class="danger-action"
                      @click="removeFieldMapping(targetMeta.targetType, $index)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="field-tip">`MP_SUBSCRIBE` 的渠道参数统一落在 `config_json`，页面仅维护结构化字段，不直接编辑原始 JSON。</div>
            </el-form-item>
          </template>

          <el-form-item label="目标备注">
            <el-input
              v-model="getTargetForm(targetMeta.targetType).remark"
              :disabled="dialogReadonly"
              type="textarea"
              :rows="2"
              placeholder="请输入目标备注"
            />
          </el-form-item>
        </div>

        <div class="section-title">可用变量</div>
        <el-table :data="dialogForm.variables || []" size="mini" border>
          <el-table-column label="变量名" prop="name" min-width="180" />
          <el-table-column label="说明" prop="desc" min-width="220" />
          <el-table-column label="示例值" prop="example" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">{{ dialogReadonly ? '关闭' : '取消' }}</el-button>
        <el-button
          v-if="!dialogReadonly"
          type="primary"
          :loading="submitLoading"
          @click="submitSceneConfig"
        >
          保存
        </el-button>
      </div>
    </el-dialog>

    <el-dialog title="目标配置预览" :visible.sync="previewDialog.visible" width="820px" append-to-body>
      <el-form label-width="120px">
        <el-form-item label="场景">
          <el-input :value="previewDialog.sceneName" disabled />
        </el-form-item>
        <el-form-item label="通知目标">
          <el-input :value="previewDialog.targetTypeDesc" disabled />
        </el-form-item>
        <el-form-item label="示例变量 JSON">
          <el-input v-model="previewDialog.variablesText" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <div class="preview-toolbar">
        <el-button type="primary" :loading="previewDialog.loading" @click="loadPreview">执行预览</el-button>
      </div>

      <el-descriptions v-if="previewDialog.result" :column="1" border size="small">
        <el-descriptions-item label="标题">{{ previewDialog.result.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ previewDialog.result.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跳转类型">{{ formatRouteType(previewDialog.result.routeType) }}</el-descriptions-item>
        <el-descriptions-item label="跳转值">{{ previewDialog.result.routeValue || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="previewDialog.result.targetType === 'MP_SUBSCRIBE'" label="页面路径">
          {{ previewDialog.result.pagePath || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="previewDialog.result && previewDialog.result.fieldMapping && previewDialog.result.fieldMapping.length" class="preview-mapping">
        <div class="section-title">字段映射预览</div>
        <el-table :data="previewDialog.result.fieldMapping" size="mini" border>
          <el-table-column label="模板字段" prop="field" min-width="160" />
          <el-table-column label="值模板" prop="valueTemplate" min-width="220" />
          <el-table-column label="渲染结果" prop="value" min-width="220" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getNotifyScene,
  getNotifySceneOptions,
  listNotifyScene,
  previewNotifyScene,
  updateNotifyScene
} from '@/api/system'

function buildDefaultQuery() {
  return {
    pageNum: 1,
    pageSize: 10,
    sceneName: '',
    sceneCode: '',
    bizType: '',
    targetType: ''
  }
}

function buildFieldMapping() {
  return {
    field: '',
    value: ''
  }
}

function buildEmptyPreviewDialog() {
  return {
    visible: false,
    loading: false,
    sceneCode: '',
    sceneName: '',
    targetType: '',
    targetTypeDesc: '',
    channelScene: '',
    variablesText: '{}',
    result: null
  }
}

export default {
  name: 'NotifySceneConfig',
  data() {
    return {
      loading: false,
      optionsLoading: false,
      optionsLoaded: false,
      total: 0,
      tableData: [],
      queryParams: buildDefaultQuery(),
      options: {
        sceneOptions: [],
        targetTypeOptions: [],
        channelSceneOptions: [],
        routeTypeOptions: []
      },
      dialogVisible: false,
      dialogMode: 'view',
      dialogForm: null,
      checkedTargetTypes: [],
      submitLoading: false,
      previewDialog: buildEmptyPreviewDialog()
    }
  },
  computed: {
    dialogReadonly() {
      return this.dialogMode !== 'edit'
    },
    sceneOptions() {
      return Array.isArray(this.options.sceneOptions) ? this.options.sceneOptions : []
    },
    targetTypeOptions() {
      return Array.isArray(this.options.targetTypeOptions) ? this.options.targetTypeOptions : []
    },
    channelSceneOptions() {
      return Array.isArray(this.options.channelSceneOptions) ? this.options.channelSceneOptions : []
    },
    routeTypeOptions() {
      return Array.isArray(this.options.routeTypeOptions) ? this.options.routeTypeOptions : []
    },
    bizTypeOptions() {
      const items = this.sceneOptions.map(item => item.bizType).filter(Boolean)
      return Array.from(new Set(items))
    },
    currentSceneTargetMetas() {
      if (!this.dialogForm || !this.dialogForm.sceneCode) {
        return []
      }
      const sceneMeta = this.getSceneMeta(this.dialogForm.sceneCode)
      return sceneMeta && Array.isArray(sceneMeta.targetMetas) ? sceneMeta.targetMetas : []
    }
  },
  created() {
    this.loadOptions().then(() => {
      this.getList()
    })
  },
  methods: {
    /**
     * 读取页面元数据。
     *
     * 所有场景、目标类型和默认目标配置都由后端注册表统一提供，
     * 前端只负责按元数据回填表单，不自行硬编码业务场景。
     */
    loadOptions() {
      this.optionsLoading = true
      return getNotifySceneOptions().then(res => {
        if (!res) {
          return
        }
        this.options = Object.assign({
          sceneOptions: [],
          targetTypeOptions: [],
          channelSceneOptions: [],
          routeTypeOptions: []
        }, res.data || {})
        this.optionsLoaded = true
      }).finally(() => {
        this.optionsLoading = false
      })
    },
    ensureOptionsLoaded() {
      if (this.optionsLoaded) {
        return Promise.resolve()
      }
      return this.loadOptions()
    },
    getList() {
      this.loading = true
      return listNotifyScene(this.buildQueryParams()).then(res => {
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
    openDetailDialog(sceneCode, mode) {
      this.ensureOptionsLoaded().then(() => {
        getNotifyScene(sceneCode).then(res => {
          if (!res) {
            return
          }
          this.dialogMode = mode
          this.dialogForm = this.normalizeDetailForm(res.data || {})
          this.checkedTargetTypes = this.dialogForm.targetConfigs
            .filter(item => item.enabled === 1)
            .map(item => item.targetType)
          this.dialogVisible = true
        })
      })
    },
    /**
     * 详情数据会把已保存配置和注册表默认值一起合并成完整目标列表。
     *
     * 前端这里继续做一次兜底规整，避免某个目标字段为空时表单直接出现 undefined。
     */
    normalizeDetailForm(data) {
      const form = Object.assign({
        sceneCode: data.sceneCode || '',
        sceneName: data.sceneName || '',
        bizType: data.bizType || '',
        eventCode: data.eventCode || '',
        status: typeof data.status === 'number' ? data.status : 1,
        remark: data.remark || '',
        variables: Array.isArray(data.variables) ? data.variables : [],
        targetConfigs: []
      })
      form.targetConfigs = (Array.isArray(data.targetConfigs) ? data.targetConfigs : []).map(item => Object.assign({
        targetType: '',
        targetTypeDesc: '',
        receiverType: '',
        receiverTypeDesc: '',
        receiverDesc: '',
        channelType: '',
        channelTypeDesc: '',
        enabled: 0,
        titleTemplate: '',
        contentTemplate: '',
        routeType: '',
        routeValueTemplate: '',
        templateId: '',
        channelScene: '',
        channelSceneDesc: '',
        pagePathTemplate: '',
        fieldMapping: [buildFieldMapping()],
        remark: ''
      }, item || {}, {
        fieldMapping: Array.isArray(item && item.fieldMapping) && item.fieldMapping.length
          ? item.fieldMapping.map(mapping => ({
            field: mapping.field || '',
            value: mapping.value || ''
          }))
          : [buildFieldMapping()]
      }))
      return form
    },
    getSceneMeta(sceneCode) {
      return this.sceneOptions.find(item => item.sceneCode === sceneCode) || null
    },
    getTargetForm(targetType) {
      let targetForm = this.dialogForm.targetConfigs.find(item => item.targetType === targetType)
      if (!targetForm) {
        const targetMeta = this.currentSceneTargetMetas.find(item => item.targetType === targetType)
        targetForm = this.buildTargetFormByMeta(targetMeta)
        this.dialogForm.targetConfigs.push(targetForm)
      }
      return targetForm
    },
    buildTargetFormByMeta(targetMeta) {
      return {
        targetType: targetMeta ? targetMeta.targetType : '',
        targetTypeDesc: targetMeta ? targetMeta.targetTypeDesc : '',
        receiverType: targetMeta ? targetMeta.receiverType : '',
        receiverTypeDesc: targetMeta ? targetMeta.receiverTypeDesc : '',
        receiverDesc: targetMeta ? targetMeta.receiverDesc : '',
        channelType: targetMeta ? targetMeta.channelType : '',
        channelTypeDesc: targetMeta ? targetMeta.channelTypeDesc : '',
        enabled: targetMeta && typeof targetMeta.defaultEnabled === 'number' ? targetMeta.defaultEnabled : 0,
        titleTemplate: targetMeta ? (targetMeta.defaultTitleTemplate || '') : '',
        contentTemplate: targetMeta ? (targetMeta.defaultContentTemplate || '') : '',
        routeType: targetMeta ? (targetMeta.defaultRouteType || '') : '',
        routeValueTemplate: targetMeta ? (targetMeta.defaultRouteValueTemplate || '') : '',
        templateId: targetMeta ? (targetMeta.templateId || '') : '',
        channelScene: targetMeta ? (targetMeta.channelScene || '') : '',
        channelSceneDesc: targetMeta ? (targetMeta.channelSceneDesc || '') : '',
        pagePathTemplate: targetMeta ? (targetMeta.pagePathTemplate || '') : '',
        fieldMapping: targetMeta && Array.isArray(targetMeta.fieldMapping) && targetMeta.fieldMapping.length
          ? targetMeta.fieldMapping.map(item => ({
            field: item.field || '',
            value: item.value || ''
          }))
          : [buildFieldMapping()],
        remark: ''
      }
    },
    handleCheckedTargetsChange(targetTypes) {
      // 勾选项用于控制当前页面展示哪些目标配置，并顺带把未勾选目标标记为停用。
      this.currentSceneTargetMetas.forEach(targetMeta => {
        const targetForm = this.getTargetForm(targetMeta.targetType)
        if (targetTypes.includes(targetMeta.targetType)) {
          if (targetForm.enabled !== 1 && targetForm.enabled !== 0) {
            targetForm.enabled = 0
          }
          this.applyDefaultValuesIfBlank(targetMeta, targetForm)
        } else {
          targetForm.enabled = 0
        }
      })
    },
    applyDefaultValuesIfBlank(targetMeta, targetForm) {
      if (!targetForm.titleTemplate) {
        targetForm.titleTemplate = targetMeta.defaultTitleTemplate || ''
      }
      if (!targetForm.contentTemplate) {
        targetForm.contentTemplate = targetMeta.defaultContentTemplate || ''
      }
      if (!targetForm.routeType) {
        targetForm.routeType = targetMeta.defaultRouteType || ''
      }
      if (!targetForm.routeValueTemplate) {
        targetForm.routeValueTemplate = targetMeta.defaultRouteValueTemplate || ''
      }
      if (targetMeta.targetType === 'MP_SUBSCRIBE') {
        if (!targetForm.templateId) {
          targetForm.templateId = targetMeta.templateId || ''
        }
        if (!targetForm.channelScene) {
          targetForm.channelScene = targetMeta.channelScene || ''
        }
        if (!targetForm.pagePathTemplate) {
          targetForm.pagePathTemplate = targetMeta.pagePathTemplate || ''
        }
        if ((!targetForm.fieldMapping || !targetForm.fieldMapping.length) && Array.isArray(targetMeta.fieldMapping)) {
          targetForm.fieldMapping = targetMeta.fieldMapping.map(item => ({
            field: item.field || '',
            value: item.value || ''
          }))
        }
      }
    },
    addFieldMapping(targetType) {
      this.getTargetForm(targetType).fieldMapping.push(buildFieldMapping())
    },
    removeFieldMapping(targetType, index) {
      const fieldMapping = this.getTargetForm(targetType).fieldMapping
      fieldMapping.splice(index, 1)
      if (!fieldMapping.length) {
        fieldMapping.push(buildFieldMapping())
      }
    },
    submitSceneConfig() {
      const payload = this.buildSubmitPayload()
      if (!payload) {
        return
      }
      this.submitLoading = true
      updateNotifyScene(this.dialogForm.sceneCode, payload).then(res => {
        if (!res) {
          return
        }
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.getList()
      }).finally(() => {
        this.submitLoading = false
      })
    },
    /**
     * 页面按“整场景一次性保存”组装请求体，避免目标配置拆开保存后出现状态不一致。
     */
    buildSubmitPayload() {
      if (!this.dialogForm) {
        return null
      }
      const targetConfigs = []
      for (const targetMeta of this.currentSceneTargetMetas) {
        const targetForm = this.getTargetForm(targetMeta.targetType)
        const enabled = this.checkedTargetTypes.includes(targetMeta.targetType)
          ? Number(targetForm.enabled === 1 ? 1 : 0)
          : 0
        if (targetMeta.targetType === 'MP_SUBSCRIBE' && enabled === 1 && !this.trimValue(targetForm.channelScene)) {
          this.$message.error(`${targetMeta.targetTypeDesc}启用时必须选择发送小程序`)
          return null
        }
        const fieldMapping = Array.isArray(targetForm.fieldMapping) && targetForm.fieldMapping.length
          ? targetForm.fieldMapping.map(item => ({
            field: this.trimValue(item.field) || '',
            value: this.trimValue(item.value) || ''
          }))
          : []
        targetConfigs.push({
          targetType: targetMeta.targetType,
          enabled,
          titleTemplate: this.trimValue(targetForm.titleTemplate),
          contentTemplate: this.trimValue(targetForm.contentTemplate),
          routeType: this.trimValue(targetForm.routeType),
          routeValueTemplate: this.trimValue(targetForm.routeValueTemplate),
          templateId: this.trimValue(targetForm.templateId),
          channelScene: this.trimValue(targetForm.channelScene),
          pagePathTemplate: this.trimValue(targetForm.pagePathTemplate),
          fieldMapping,
          remark: this.trimValue(targetForm.remark)
        })
      }
      return {
        status: this.dialogForm.status,
        remark: this.trimValue(this.dialogForm.remark),
        targetConfigs
      }
    },
    openPreviewDialog(targetMeta) {
      const targetForm = this.getTargetForm(targetMeta.targetType)
      this.previewDialog = Object.assign(buildEmptyPreviewDialog(), {
        visible: true,
        sceneCode: this.dialogForm.sceneCode,
        sceneName: this.dialogForm.sceneName,
        targetType: targetMeta.targetType,
        targetTypeDesc: targetMeta.targetTypeDesc,
        result: null,
        variablesText: this.buildPreviewVariablesText()
      })
      this.previewDialog.titleTemplate = targetForm.titleTemplate
      this.previewDialog.contentTemplate = targetForm.contentTemplate
      this.previewDialog.routeType = targetForm.routeType
      this.previewDialog.routeValueTemplate = targetForm.routeValueTemplate
      this.previewDialog.templateId = targetForm.templateId
      this.previewDialog.channelScene = targetForm.channelScene
      this.previewDialog.pagePathTemplate = targetForm.pagePathTemplate
      this.previewDialog.fieldMapping = (targetForm.fieldMapping || []).map(item => ({
        field: item.field || '',
        value: item.value || ''
      }))
    },
    buildPreviewVariablesText() {
      const variables = {}
      ;(this.dialogForm.variables || []).forEach(item => {
        if (!item || !item.name) {
          return
        }
        variables[item.name] = item.example || ''
      })
      return JSON.stringify(variables, null, 2)
    },
    loadPreview() {
      let variables = {}
      try {
        variables = this.previewDialog.variablesText ? JSON.parse(this.previewDialog.variablesText) : {}
      } catch (e) {
        this.$message.error('示例变量 JSON 格式不正确')
        return
      }
      this.previewDialog.loading = true
      previewNotifyScene({
        sceneCode: this.previewDialog.sceneCode,
        targetType: this.previewDialog.targetType,
        titleTemplate: this.trimValue(this.previewDialog.titleTemplate),
        contentTemplate: this.trimValue(this.previewDialog.contentTemplate),
        routeType: this.trimValue(this.previewDialog.routeType),
        routeValueTemplate: this.trimValue(this.previewDialog.routeValueTemplate),
        templateId: this.trimValue(this.previewDialog.templateId),
        channelScene: this.trimValue(this.previewDialog.channelScene),
        pagePathTemplate: this.trimValue(this.previewDialog.pagePathTemplate),
        fieldMapping: (this.previewDialog.fieldMapping || []).map(item => ({
          field: this.trimValue(item.field),
          value: this.trimValue(item.value)
        })),
        variables
      }).then(res => {
        if (!res) {
          return
        }
        this.previewDialog.result = res.data || null
      }).finally(() => {
        this.previewDialog.loading = false
      })
    },
    buildReceiverDesc(targetMeta) {
      const parts = [targetMeta.receiverTypeDesc, targetMeta.receiverDesc].filter(Boolean)
      return parts.length ? parts.join(' / ') : '-'
    },
    formatRouteType(routeType) {
      if (!routeType) {
        return '-'
      }
      const matched = this.routeTypeOptions.find(item => item.code === routeType)
      return matched ? matched.desc : routeType
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

.pagination {
  margin-top: 16px;
  text-align: right;
}

.section-title {
  margin: 18px 0 12px;
  font-weight: 600;
  color: #303133;
}

.muted-text {
  color: #909399;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 4px;
}

.target-card {
  margin-bottom: 16px;
  padding: 16px 16px 4px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.target-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.target-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.target-card__sub {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.target-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mapping-toolbar {
  margin-bottom: 8px;
}

.field-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.preview-toolbar {
  margin-bottom: 12px;
}

.preview-mapping {
  margin-top: 16px;
}

.danger-action {
  color: #f56c6c;
}
</style>
