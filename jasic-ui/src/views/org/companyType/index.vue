<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['org:companyType:add']" @click="handleAdd">新增类型</el-button>
      </div>
      <el-table v-loading="loading" :data="typeList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="类型编码" prop="typeCode" width="140" />
        <el-table-column label="类型名称" prop="typeName" width="180" />
        <el-table-column label="主体类型" prop="subjectType" width="120">
          <template slot-scope="{ row }">
            <el-tag v-if="row.subjectType === 'PLATFORM'" size="mini">平台</el-tag>
            <el-tag v-else-if="row.subjectType === 'HQ'" type="warning" size="mini">总部</el-tag>
            <el-tag v-else type="success" size="mini">服务网点</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="orderNum" width="80" align="center" />
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['org:companyType:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:menu:update']" @click="handleAssignMenu(row)">分配菜单</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['org:companyType:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="form.typeCode" placeholder="如 HQ_A、FIRST" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="如 总部A" />
        </el-form-item>
        <el-form-item label="主体类型" prop="subjectType">
          <el-select v-model="form.subjectType" placeholder="请选择">
            <el-option label="平台" value="PLATFORM" />
            <el-option label="总部" value="HQ" />
            <el-option label="服务网点" value="SERVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
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
    <el-dialog :title="menuDialogTitle" :visible.sync="menuDialogVisible" width="560px" append-to-body>
      <el-alert
        :closable="false"
        type="info"
        show-icon
        style="margin-bottom: 16px;"
      >
        <template slot="title">
          从「{{ subjectTypeLabel }}」的菜单全集中勾选该公司类型可使用的菜单，角色模板和角色分配菜单时只能从此范围选择。
        </template>
      </el-alert>
      <el-tree
        ref="menuTree"
        v-loading="menuTreeLoading"
        :data="menuTreeData"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :check-strictly="menuCheckStrictly"
        default-expand-all
      />
      <div slot="footer">
        <el-button @click="menuDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="menuSaveLoading" @click="submitAssignMenu">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCompanyType, addCompanyType, updateCompanyType, deleteCompanyType } from '@/api/org'
import { menuTree, typeCodeMenuIds, assignTypeCodeMenus } from '@/api/system'

const SUBJECT_TYPE_MAP = { PLATFORM: '平台', HQ: '总部', SERVICE: '服务网点' }

export default {
  name: 'CompanyTypeManage',
  data() {
    return {
      loading: false,
      typeList: [],
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
        typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
        subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }]
      },
      menuDialogVisible: false,
      menuDialogTitle: '',
      menuTreeData: [],
      checkedMenuIds: [],
      menuTreeLoading: false,
      menuSaveLoading: false,
      currentAssignTypeCode: '',
      currentAssignSubjectType: '',
      subjectTypeLabel: '',
      menuCheckStrictly: true
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listCompanyType().then(res => {
        if (!res) return
        this.typeList = res.data || []
      }).finally(() => { this.loading = false })
    },
    handleAdd() {
      this.dialogTitle = '新增公司类型'
      this.form = { orderNum: 0 }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑公司类型'
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateCompanyType : addCompanyType
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除类型"${row.typeName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteCompanyType(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleAssignMenu(row) {
      this.currentAssignTypeCode = row.typeCode
      this.currentAssignSubjectType = row.subjectType
      this.subjectTypeLabel = SUBJECT_TYPE_MAP[row.subjectType] || row.subjectType
      this.menuDialogTitle = `分配菜单 - ${row.typeName}（${row.typeCode}）`
      this.checkedMenuIds = []
      this.menuTreeData = []
      this.menuTreeLoading = true
      this.menuCheckStrictly = true
      this.menuDialogVisible = true
      Promise.all([
        menuTree(row.subjectType),
        typeCodeMenuIds(row.typeCode)
      ]).then(([treeRes, idsRes]) => {
        if (!treeRes || !idsRes) return
        this.menuTreeData = treeRes.data || []
        this.checkedMenuIds = idsRes.data || []
        this.$nextTick(() => {
          if (this.$refs.menuTree) {
            this.$refs.menuTree.setCheckedKeys(this.checkedMenuIds)
            this.$nextTick(() => {
              this.menuCheckStrictly = false
            })
          }
        })
      }).finally(() => { this.menuTreeLoading = false })
    },
    submitAssignMenu() {
      if (!this.$refs.menuTree) return
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      const halfCheckedKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const menuIds = checkedKeys.concat(halfCheckedKeys)
      this.menuSaveLoading = true
      assignTypeCodeMenus(this.currentAssignTypeCode, menuIds).then(res => {
        if (!res) return
        this.$message.success('菜单分配保存成功')
        this.menuDialogVisible = false
      }).finally(() => { this.menuSaveLoading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
