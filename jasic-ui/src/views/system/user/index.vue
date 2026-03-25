<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="queryParams.username" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="queryParams.realName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="请输入" clearable />
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
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['system:user:add']" @click="handleAdd">新增</el-button>
      </div>
      <el-table v-loading="loading" :data="userList" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户名" prop="username" width="120" />
        <el-table-column label="姓名" prop="realName" width="120" />
        <el-table-column label="手机号" prop="phone" width="130" />
        <el-table-column label="状态" prop="status" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" fixed="right" width="360">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" v-hasPerms="['system:user:update']" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:user:update']" @click="handleAssignRole(row)">分配角色</el-button>
            <el-button
              v-if="canBindRegion"
              type="text"
              size="mini"
              @click="handleBindRegion(row)"
            >
              绑定大区
            </el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:user:resetPwd']" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="text" size="mini" v-hasPerms="['system:user:kickout']" @click="handleKickout(row)">强制下线</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" v-hasPerms="['system:user:remove']" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="分配角色" :visible.sync="roleDialogVisible" width="500px" append-to-body>
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id">
          {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <div slot="footer">
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="submitAssignRole">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="绑定大区" :visible.sync="regionDialogVisible" width="500px" append-to-body>
      <div class="dialog-tip">仅绑定当前总部公司下的大区，保存时不会影响该用户在其他总部下的绑定。</div>
      <el-empty v-if="regionOptions.length === 0" description="当前总部下暂无可绑定的大区" :image-size="80" />
      <el-checkbox-group v-else v-model="selectedRegionIds">
        <el-checkbox v-for="region in regionOptions" :key="region.id" :label="region.id">
          {{ region.regionName }}<span v-if="region.regionCode">（{{ region.regionCode }}）</span>
        </el-checkbox>
      </el-checkbox-group>
      <div slot="footer">
        <el-button @click="regionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="regionLoading" @click="submitBindRegion">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addUser,
  assignUserRegions,
  assignUserRoles,
  deleteUser,
  getUser,
  getUserRegions,
  kickoutUser,
  listRegion,
  listUser,
  resetPwd,
  roleOptions as fetchRoleOptions,
  updateUser
} from '@/api/system'

export default {
  name: 'UserManage',
  data() {
    return {
      loading: false,
      userList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, username: '', realName: '', phone: '', status: undefined },
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      submitLoading: false,
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      roleDialogVisible: false,
      roleOptions: [],
      selectedRoleIds: [],
      currentUserId: null,
      roleLoading: false,
      regionDialogVisible: false,
      regionOptions: [],
      selectedRegionIds: [],
      currentRegionUserId: null,
      regionLoading: false
    }
  },
  computed: {
    isCurrentHq() {
      return this.$store.getters.userInfo.currentSubjectType === 'HQ'
    },
    canBindRegion() {
      const perms = this.$store.getters.perms || []
      return this.isCurrentHq && perms.includes('system:region:list') && perms.includes('system:region:assign')
    },
    currentCompanyId() {
      return this.$store.getters.currentCompanyId
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listUser(this.queryParams).then(res => {
        if (!res) return
        this.userList = res.data.records
        this.total = res.data.total
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
      this.queryParams = { pageNum: 1, pageSize: 10, username: '', realName: '', phone: '', status: undefined }
      this.getList()
    },
    handleAdd() {
      this.dialogTitle = '新增用户'
      this.form = { status: 1 }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.dialogTitle = '编辑用户'
      getUser(row.id).then(res => {
        if (!res) return
        this.form = res.data
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const api = this.form.id ? updateUser : addUser
        api(this.form).then(res => {
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
      this.$confirm(`确认删除用户“${row.username}”吗？`, '提示', { type: 'warning' }).then(() => {
        deleteUser(row.id).then(res => {
          if (!res) return
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    handleResetPwd(row) {
      this.$prompt('请输入新密码', '重置密码', { inputType: 'password' }).then(({ value }) => {
        resetPwd({ userId: row.id, newPassword: value }).then(res => {
          if (!res) return
          this.$message.success('重置成功')
        })
      }).catch(() => {})
    },
    handleKickout(row) {
      this.$confirm(`确认强制下线用户“${row.username}”吗？`, '提示', { type: 'warning' }).then(() => {
        kickoutUser(row.id).then(res => {
          if (!res) return
          this.$message.success('已强制下线')
        })
      }).catch(() => {})
    },
    handleAssignRole(row) {
      this.currentUserId = row.id
      this.selectedRoleIds = []
      Promise.all([fetchRoleOptions(), getUser(row.id)]).then(([roleRes, userRes]) => {
        if (!roleRes) return
        this.roleOptions = roleRes.data || []
        if (userRes && userRes.data && userRes.data.roles && userRes.data.roles.length) {
          this.selectedRoleIds = userRes.data.roles.map(role => role.id)
        }
        this.roleDialogVisible = true
      })
    },
    submitAssignRole() {
      this.roleLoading = true
      assignUserRoles(this.currentUserId, this.selectedRoleIds).then(res => {
        if (!res) return
        this.$message.success('分配成功')
        this.roleDialogVisible = false
      }).finally(() => {
        this.roleLoading = false
      })
    },
    handleBindRegion(row) {
      if (!this.currentCompanyId) {
        this.$message.error('当前公司为空，无法绑定大区')
        return
      }
      this.currentRegionUserId = row.id
      this.selectedRegionIds = []
      Promise.all([
        listRegion(this.currentCompanyId),
        getUserRegions(row.id)
      ]).then(([regionRes, selectedRes]) => {
        if (!regionRes || !selectedRes) return
        this.regionOptions = regionRes.data || []
        this.selectedRegionIds = selectedRes.data || []
        this.regionDialogVisible = true
      })
    },
    submitBindRegion() {
      this.regionLoading = true
      assignUserRegions(this.currentRegionUserId, this.selectedRegionIds).then(res => {
        if (!res) return
        this.$message.success('绑定成功')
        this.regionDialogVisible = false
      }).finally(() => {
        this.regionLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 0; }
.search-card { margin-bottom: 0; }
.table-toolbar { margin-bottom: 12px; }
.dialog-tip { margin-bottom: 12px; color: #909399; font-size: 12px; line-height: 1.6; }
</style>
