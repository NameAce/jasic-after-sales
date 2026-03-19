<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" size="small">
        <el-form-item label="公司类型">
          <el-select v-model="queryTypeCode" placeholder="请选择" @change="handleQuery">
            <el-option v-for="t in typeCodeOptions" :key="t.value" :label="t.label" :value="t.value" />
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
        <el-table-column label="所属类型" prop="typeCode" width="130" />
        <el-table-column label="数据范围" prop="dataScope" width="100" />
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" fixed="right" width="300">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:roleTemplate:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:roleTemplate:update']" @click="handleAssignMenu(row)">分配菜单</el-button>
            <el-button type="text" size="mini" style="color: #E6A23C;" v-hasPerms="['system:roleTemplate:sync']" @click="handleSync(row)">同步至公司</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:roleTemplate:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：管理员" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如：admin" />
        </el-form-item>
        <el-form-item label="所属类型" prop="typeCode">
          <el-select v-model="form.typeCode" placeholder="请选择">
            <el-option v-for="t in typeCodeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" placeholder="请选择">
            <el-option label="全部数据" value="ALL" />
            <el-option label="大区数据" value="REGION" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog title="分配模板菜单" :visible.sync="menuDialogVisible" width="500px" append-to-body>
      <el-tree
        ref="menuTree"
        :data="menuTreeData"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        check-strictly
      />
      <div slot="footer">
        <el-button @click="menuDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="menuLoading" @click="submitAssignMenu">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listRoleTemplate, getRoleTemplate, addRoleTemplate, updateRoleTemplate,
  deleteRoleTemplate, syncRoleTemplate, typeCodeMenuTree
} from '@/api/system'
import { listCompanyType } from '@/api/org'

export default {
  name: 'RoleTemplateManage',
  data() {
    return {
      loading: false,
      templateList: [],
      queryTypeCode: '',
      typeCodeOptions: [],
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
      menuDialogVisible: false,
      menuTreeData: [],
      checkedMenuIds: [],
      currentTemplateId: null,
      menuLoading: false
    }
  },
  created() {
    this.loadTypeCodeOptions()
  },
  methods: {
    loadTypeCodeOptions() {
      listCompanyType().then(res => {
        if (!res) return
        this.typeCodeOptions = (res.data || []).map(t => ({ value: t.typeCode, label: t.typeName }))
        if (this.typeCodeOptions.length > 0 && !this.queryTypeCode) {
          this.queryTypeCode = this.typeCodeOptions[0].value
          this.handleQuery()
        }
      })
    },
    handleQuery() {
      if (!this.queryTypeCode) return
      this.loading = true
      listRoleTemplate(this.queryTypeCode).then(res => {
        if (!res) return
        this.templateList = res.data || []
      }).finally(() => { this.loading = false })
    },
    handleAdd() {
      this.dialogTitle = '新增模板'
      this.form = { typeCode: this.queryTypeCode, dataScope: 'SELF', roleName: '', roleKey: '' }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑模板'
      getRoleTemplate(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateRoleTemplate : addRoleTemplate
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.handleQuery()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除模板"${row.roleName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteRoleTemplate(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.handleQuery()
        })
      }).catch(() => {})
    },
    handleAssignMenu(row) {
      this.currentTemplateId = row.id
      this.checkedMenuIds = []
      typeCodeMenuTree(row.typeCode).then(res => {
        if (!res) return
        this.menuTreeData = res.data || []
        this.menuDialogVisible = true
      })
    },
    submitAssignMenu() {
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      const halfCheckedKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const menuIds = checkedKeys.concat(halfCheckedKeys)
      this.menuLoading = true
      // 复用角色模板的菜单分配API（需后端支持）
      updateRoleTemplate({ id: this.currentTemplateId, menuIds }).then(res => {
        if (!res) return
        this.$message.success('分配成功')
        this.menuDialogVisible = false
      }).finally(() => { this.menuLoading = false })
    },
    handleSync(row) {
      this.$confirm(`确认将模板"${row.roleName}"同步至所有关联公司？此操作不可撤销！`, '同步确认', { type: 'warning' }).then(() => {
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
</style>
