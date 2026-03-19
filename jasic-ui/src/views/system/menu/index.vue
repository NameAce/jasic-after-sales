<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" size="small">
        <el-form-item label="主体类型">
          <el-select v-model="querySubjectType" placeholder="请选择" clearable @change="handleQuery">
            <el-option v-for="t in subjectTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:menu:add']" @click="handleAdd">新增</el-button>
        <el-button type="success" icon="el-icon-document-copy" size="small" v-hasPerms="['system:menu:add']" @click="handleCopy">菜单拷贝</el-button>
        <el-button icon="el-icon-sort" size="small" @click="toggleExpand">{{ isExpandAll ? '折叠' : '展开' }}</el-button>
      </div>
      <el-table
        v-if="refreshTable"
        v-loading="loading"
        :data="menuList"
        row-key="id"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children' }"
        border
      >
        <el-table-column label="菜单名称" prop="menuName" width="200" />
        <el-table-column label="图标" prop="icon" width="80" align="center">
          <template slot-scope="{ row }">
            <i :class="row.icon" v-if="row.icon" />
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="orderNum" width="70" align="center" />
        <el-table-column label="权限标识" prop="perms" width="200" />
        <el-table-column label="组件路径" prop="component" width="200" />
        <el-table-column label="类型" prop="menuType" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.menuType === 'M'" size="mini">目录</el-tag>
            <el-tag v-else-if="row.menuType === 'C'" type="success" size="mini">菜单</el-tag>
            <el-tag v-else-if="row.menuType === 'F'" type="info" size="mini">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可见" prop="isVisible" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.isVisible === 1 ? '' : 'info'" size="mini">
              {{ row.isVisible === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:menu:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:menu:add']" @click="handleAdd(row)">新增子级</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:menu:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-cascader
            v-model="form.parentId"
            :options="menuOptions"
            :props="{ value: 'id', label: 'menuName', children: 'children', checkStrictly: true, emitPath: false }"
            clearable
            placeholder="无上级（顶级菜单）"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio label="M">目录</el-radio>
            <el-radio label="C">菜单</el-radio>
            <el-radio label="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主体类型" prop="subjectType">
          <el-select v-model="form.subjectType" placeholder="请选择">
            <el-option v-for="t in subjectTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 user、role" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'M'" label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="图标">
          <el-input v-model="form.icon" placeholder="如 el-icon-setting" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.orderNum" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="form.menuType !== 'F'" label="是否可见">
          <el-radio-group v-model="form.isVisible">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 菜单拷贝弹窗 -->
    <el-dialog title="菜单拷贝" :visible.sync="copyDialogVisible" width="560px" append-to-body @open="onCopyDialogOpen">
      <el-form ref="copyForm" :model="copyForm" label-width="100px">
        <el-form-item label="源主体类型" prop="sourceSubjectType">
          <el-select v-model="copyForm.sourceSubjectType" placeholder="请选择" style="width: 100%;" @change="loadCopySourceTree">
            <el-option v-for="t in subjectTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标主体类型" prop="targetSubjectType">
          <el-select v-model="copyForm.targetSubjectType" placeholder="请选择" style="width: 100%;">
            <el-option v-for="t in subjectTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择菜单">
          <div class="copy-tree-tip">不勾选则拷贝全部；勾选后拷贝选中节点、其子级及所有父级目录（保证结构完整）</div>
          <el-tree
            ref="copyMenuTree"
            v-loading="copyTreeLoading"
            :data="copySourceTree"
            :props="{ label: 'menuName', children: 'children' }"
            show-checkbox
            node-key="id"
            style="max-height: 320px; overflow-y: auto; margin-top: 8px;"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="copyDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="copySubmitLoading" @click="submitCopy">开始拷贝</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { menuTree, getMenu, addMenu, updateMenu, deleteMenu, copyMenus } from '@/api/system'

export default {
  name: 'MenuManage',
  data() {
    return {
      loading: false,
      menuList: [],
      querySubjectType: 'PLATFORM',
      subjectTypeOptions: [
        { value: 'PLATFORM', label: '平台' },
        { value: 'HQ', label: '总部' },
        { value: 'SERVICE', label: '服务网点' }
      ],
      isExpandAll: true,
      refreshTable: true,
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      menuOptions: [],
      submitLoading: false,
      rules: {
        menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
        menuType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }]
      },
      copyDialogVisible: false,
      copyForm: {
        sourceSubjectType: 'PLATFORM',
        targetSubjectType: 'HQ'
      },
      copySourceTree: [],
      copyTreeLoading: false,
      copySubmitLoading: false
    }
  },
  created() {
    this.handleQuery()
  },
  methods: {
    handleQuery() {
      this.loading = true
      menuTree(this.querySubjectType).then(res => {
        if (!res) return
        this.menuList = res.data || []
      }).finally(() => { this.loading = false })
    },
    toggleExpand() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => { this.refreshTable = true })
    },
    handleAdd(row) {
      this.dialogTitle = '新增菜单'
      this.form = {
        parentId: row && row.id ? row.id : 0,
        menuType: 'M',
        subjectType: this.querySubjectType || 'PLATFORM',
        status: 1,
        isVisible: 1,
        orderNum: 0
      }
      this.loadMenuOptions()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑菜单'
      getMenu(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.loadMenuOptions()
        this.dialogVisible = true
      })
    },
    loadMenuOptions() {
      menuTree(this.form.subjectType || this.querySubjectType).then(res => {
        if (!res) return
        this.menuOptions = [{ id: 0, menuName: '顶级菜单', children: res.data || [] }]
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateMenu : addMenu
        api(this.form).then(res => {
          if (!res) return
          this.$message.success('操作成功')
          this.dialogVisible = false
          this.handleQuery()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除菜单"${row.menuName}"？`, '提示', { type: 'warning' }).then(() => {
        deleteMenu(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.handleQuery()
        })
      }).catch(() => {})
    },
    handleCopy() {
      this.copyForm = { sourceSubjectType: 'PLATFORM', targetSubjectType: 'HQ' }
      this.copySourceTree = []
      this.copyDialogVisible = true
    },
    onCopyDialogOpen() {
      this.loadCopySourceTree()
    },
    loadCopySourceTree() {
      if (!this.copyForm.sourceSubjectType) return
      this.copyTreeLoading = true
      menuTree(this.copyForm.sourceSubjectType).then(res => {
        if (!res) return
        this.copySourceTree = res.data || []
        if (this.$refs.copyMenuTree) {
          this.$refs.copyMenuTree.setCheckedKeys([])
        }
      }).finally(() => { this.copyTreeLoading = false })
    },
    submitCopy() {
      if (this.copyForm.sourceSubjectType === this.copyForm.targetSubjectType) {
        this.$message.warning('源主体与目标主体不能相同')
        return
      }
      let menuIds = []
      if (this.$refs.copyMenuTree) {
        // 仅取勾选节点，不取半选父节点（半选是勾选子节点时的 UI 状态，非用户意图）
        menuIds = this.$refs.copyMenuTree.getCheckedKeys()
      }
      this.copySubmitLoading = true
        copyMenus({
          sourceSubjectType: this.copyForm.sourceSubjectType,
          targetSubjectType: this.copyForm.targetSubjectType,
          menuIds: menuIds.length > 0 ? menuIds : null
        }).then(res => {
          if (!res) return
          this.$message.success(`拷贝成功，共 ${res.data || 0} 个菜单`)
          this.copyDialogVisible = false
          this.querySubjectType = this.copyForm.targetSubjectType
          this.handleQuery()
        }).finally(() => { this.copySubmitLoading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
.copy-tree-tip { font-size: 12px; color: #909399; margin-bottom: 4px; }
</style>
