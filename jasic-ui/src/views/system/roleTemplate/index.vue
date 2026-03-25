<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" size="small">
        <el-form-item label="公司类型">
          <el-select v-model="queryTypeCode" placeholder="请选择" @change="handleQuery">
            <el-option label="全部" value="" />
            <el-option v-for="item in typeCodeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:roleTemplate:add']" @click="handleAdd">新增模板</el-button>
      </div>
      <el-table v-loading="loading" :data="templateList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="角色名称" prop="roleName" width="180" />
        <el-table-column label="角色标识" prop="roleKey" width="160" />
        <el-table-column label="所属类型" prop="typeCode" width="140">
          <template slot-scope="{ row }">
            <span>{{ typeCodeLabelMap[row.typeCode] || row.typeCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="管理员" prop="isAdmin" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.isAdmin === 1" type="danger" size="mini">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="数据范围" prop="dataScope" min-width="140">
          <template slot-scope="{ row }">
            <span>{{ getDataScopeLabel(row.typeCode, row.dataScope) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" fixed="right" width="300">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:roleTemplate:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:roleTemplate:update']" @click="handleAssignMenu(row)">分配菜单</el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #E6A23C;"
              v-hasPerms="['system:roleTemplate:sync']"
              @click="handleSync(row)"
            >
              同步至公司
            </el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #F56C6C;"
              v-hasPerms="['system:roleTemplate:remove']"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：管理员" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如：admin" />
        </el-form-item>
        <el-form-item label="所属类型" prop="typeCode">
          <el-select v-model="form.typeCode" placeholder="请选择" @change="handleTypeCodeChange">
            <el-option v-for="item in typeCodeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" placeholder="请选择">
            <el-option
              v-for="option in formDataScopeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
              :disabled="option.disabled === true"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员模板" prop="isAdmin">
          <el-switch v-model="form.isAdmin" :active-value="1" :inactive-value="0" />
          <span class="form-tip">每种公司类型最多保留一个管理员模板，用于初始化公司管理员角色。</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="分配模板菜单" :visible.sync="menuDialogVisible" width="500px" append-to-body>
      <el-tree
        ref="menuTree"
        :data="menuTreeData"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :check-strictly="menuCheckStrictly"
      />
      <div slot="footer">
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="menuLoading" @click="submitAssignMenu">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCompanyType } from '@/api/org'
import {
  addRoleTemplate,
  deleteRoleTemplate,
  getRoleTemplate,
  listRoleTemplate,
  roleTemplateDataScopeOptionMap,
  roleTemplateDataScopeOptions,
  syncRoleTemplate,
  typeCodeMenuTree,
  updateRoleTemplate
} from '@/api/system'

export default {
  name: 'RoleTemplateManage',
  data() {
    return {
      loading: false,
      templateList: [],
      queryTypeCode: '',
      typeCodeOptions: [],
      typeCodeLabelMap: {},
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
        typeCode: [{ required: true, message: '请选择所属类型', trigger: 'change' }],
        dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
      },
      dataScopeOptionMap: {},
      dataScopeOptions: [],
      formDataScopeOptions: [],
      menuDialogVisible: false,
      menuTreeData: [],
      checkedMenuIds: [],
      currentAssignRow: null,
      menuLoading: false,
      menuCheckStrictly: true
    }
  },
  created() {
    this.initPage()
  },
  methods: {
    initPage() {
      Promise.all([
        this.loadTypeCodeOptions(),
        this.loadDataScopeOptionMap()
      ]).then(() => {
        this.handleQuery()
      })
    },
    loadTypeCodeOptions() {
      return listCompanyType().then(res => {
        if (!res) return
        this.typeCodeOptions = (res.data || []).map(item => ({
          value: item.typeCode,
          label: item.typeName
        }))
        this.typeCodeLabelMap = this.typeCodeOptions.reduce((map, item) => {
          map[item.value] = item.label
          return map
        }, {})
      })
    },
    loadDataScopeOptionMap() {
      return roleTemplateDataScopeOptionMap().then(res => {
        if (!res) return
        this.dataScopeOptionMap = res.data || {}
      })
    },
    loadFormDataScopeOptions(typeCode, keepCurrentValue = false) {
      if (!typeCode) {
        this.dataScopeOptions = []
        this.formDataScopeOptions = []
        return Promise.resolve()
      }
      return roleTemplateDataScopeOptions(typeCode).then(res => {
        if (!res) return
        const options = res.data || []
        this.dataScopeOptions = options
        this.$set(this.dataScopeOptionMap, typeCode, options)

        const valid = options.some(option => option.value === this.form.dataScope)
        if (!keepCurrentValue && (!this.form.dataScope || !valid)) {
          this.form.dataScope = this.getDefaultDataScope(typeCode, options)
        }

        this.formDataScopeOptions = this.mergeLegacyOption(
          options,
          keepCurrentValue ? this.form.dataScope : this.form.dataScope
        )
      })
    },
    mergeLegacyOption(options, currentValue) {
      const result = [...(options || [])]
      if (currentValue && !result.some(option => option.value === currentValue)) {
        result.push({
          value: currentValue,
          label: `${currentValue}（历史值）`,
          disabled: true
        })
      }
      return result
    },
    getDefaultDataScope(typeCode, options) {
      const targetOptions = options || this.dataScopeOptionMap[typeCode] || []
      const defaultOption = targetOptions.find(option => option.defaultOption)
      if (defaultOption) {
        return defaultOption.value
      }
      return targetOptions.length > 0 ? targetOptions[0].value : 'SELF'
    },
    getDataScopeLabel(typeCode, dataScope) {
      const options = this.dataScopeOptionMap[typeCode] || []
      const matched = options.find(option => option.value === dataScope)
      return matched ? matched.label : dataScope
    },
    isValidDataScope(dataScope) {
      return (this.dataScopeOptions || []).some(option => option.value === dataScope)
    },
    handleQuery() {
      this.loading = true
      listRoleTemplate(this.queryTypeCode).then(res => {
        if (!res) return
        this.templateList = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleTypeCodeChange(typeCode) {
      this.loadFormDataScopeOptions(typeCode)
    },
    handleAdd() {
      const typeCode = this.queryTypeCode || (this.typeCodeOptions[0] && this.typeCodeOptions[0].value) || ''
      this.dialogTitle = '新增模板'
      this.form = {
        typeCode,
        dataScope: '',
        isAdmin: 0,
        roleName: '',
        roleKey: ''
      }
      this.loadFormDataScopeOptions(typeCode).then(() => {
        this.dialogVisible = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    handleEdit(row) {
      this.dialogTitle = '编辑模板'
      getRoleTemplate(row.id).then(res => {
        if (!res) return
        const data = res.data
        this.form = {
          ...data,
          isAdmin: data.isAdmin === 1 ? 1 : 0
        }
        this.loadFormDataScopeOptions(this.form.typeCode, true).then(() => {
          this.dialogVisible = true
          this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
        })
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.isValidDataScope(this.form.dataScope)) {
          this.$message.error('请选择当前公司类型允许的数据范围')
          return
        }
        this.submitLoading = true
        const payload = {
          ...this.form,
          isAdmin: this.form.isAdmin === 1 ? 1 : 0
        }
        const api = this.form.id ? updateRoleTemplate : addRoleTemplate
        api(payload).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.handleQuery()
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除模板“${row.roleName}”吗？`, '提示', { type: 'warning' }).then(() => {
        deleteRoleTemplate(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.handleQuery()
        })
      }).catch(() => {})
    },
    handleAssignMenu(row) {
      this.currentAssignRow = row
      this.checkedMenuIds = row.menuIds || []
      this.menuCheckStrictly = true
      typeCodeMenuTree(row.typeCode).then(res => {
        if (!res) return
        this.menuTreeData = res.data || []
        this.menuDialogVisible = true
        this.$nextTick(() => {
          if (this.$refs.menuTree) {
            this.$refs.menuTree.setCheckedKeys(this.checkedMenuIds)
            this.$nextTick(() => {
              this.menuCheckStrictly = false
            })
          }
        })
      })
    },
    submitAssignMenu() {
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      const halfCheckedKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const menuIds = checkedKeys.concat(halfCheckedKeys)
      const row = this.currentAssignRow
      if (!row) return
      this.menuLoading = true
      updateRoleTemplate({
        id: row.id,
        typeCode: row.typeCode,
        roleName: row.roleName,
        roleKey: row.roleKey,
        dataScope: row.dataScope,
        isAdmin: row.isAdmin === 1 ? 1 : 0,
        orderNum: row.orderNum,
        remark: row.remark,
        menuIds
      }).then(res => {
        if (!res) return
        this.$message.success('分配成功')
        this.menuDialogVisible = false
        this.handleQuery()
      }).finally(() => {
        this.menuLoading = false
      })
    },
    handleSync(row) {
      this.$confirm(`确认将模板“${row.roleName}”同步到所有关联公司吗？此操作不可撤销。`, '同步确认', { type: 'warning' }).then(() => {
        syncRoleTemplate(row.id).then(res => {
          if (!res) return
          this.$message.success('同步成功')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
