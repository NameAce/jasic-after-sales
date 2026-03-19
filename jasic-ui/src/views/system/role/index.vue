<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" ref="queryForm" :inline="true" size="small">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="queryParams.roleName" placeholder="请输入" clearable />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:role:add']" @click="handleAdd">新增</el-button>
      </div>
      <el-table v-loading="loading" :data="roleList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="角色名称" prop="roleName" width="160" />
        <el-table-column label="角色标识" prop="roleKey" width="160" />
        <el-table-column label="数据范围" prop="dataScope" width="100">
          <template slot-scope="{ row }">
            <span>{{ dataScopeMap[row.dataScope] || row.dataScope }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="系统角色" prop="isSystem" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.isSystem === 1" type="warning" size="mini">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" fixed="right" width="240">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:role:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:role:update']" @click="handleAssignMenu(row)">分配菜单</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:role:remove']" @click="handleDelete(row)" :disabled="row.isSystem === 1">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 admin、editor" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" placeholder="请选择">
            <el-option label="全部数据" value="ALL" />
            <el-option label="大区数据" value="REGION" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog title="分配菜单权限" :visible.sync="menuDialogVisible" width="500px" append-to-body>
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
import { listRole, getRole, addRole, updateRole, deleteRole, assignRoleMenus, typeCodeMenuTree } from '@/api/system'

export default {
  name: 'RoleManage',
  data() {
    return {
      loading: false,
      roleList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, roleName: '', status: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
        dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
      },
      dataScopeMap: { ALL: '全部数据', REGION: '大区数据', SELF: '仅本人' },
      menuDialogVisible: false,
      menuTreeData: [],
      checkedMenuIds: [],
      currentRoleId: null,
      menuLoading: false
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listRole(this.queryParams).then(res => {
        if (!res) return
        this.roleList = res.data.records
        this.total = res.data.total
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.queryParams = { pageNum: 1, pageSize: 10, roleName: '', status: undefined }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增角色'
      this.form = { status: 1, orderNum: 0, dataScope: 'SELF' }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑角色'
      getRole(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateRole : addRole
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除角色"${row.roleName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteRole(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleAssignMenu(row) {
      this.currentRoleId = row.id
      this.checkedMenuIds = []
      const typeCode = this.$store.getters.userInfo.currentTypeCode
      typeCodeMenuTree(typeCode).then(res => {
        if (!res) return
        this.menuTreeData = res.data || []
        getRole(row.id).then(detail => {
          if (!detail) return
          this.checkedMenuIds = detail.data.menuIds || []
          this.menuDialogVisible = true
        })
      })
    },
    submitAssignMenu() {
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      const halfCheckedKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const menuIds = checkedKeys.concat(halfCheckedKeys)
      this.menuLoading = true
      assignRoleMenus(this.currentRoleId, menuIds).then(res => {
        if (!res) return
        this.$message.success('分配成功')
        this.menuDialogVisible = false
      }).finally(() => { this.menuLoading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
