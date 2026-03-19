<template>
  <div class="dashboard-container">
    <el-row :gutter="16">
      <el-col :span="6">
        <div class="stat-card" style="border-left: 4px solid #409EFF;">
          <div class="stat-title">当前公司</div>
          <div class="stat-value">{{ currentCompanyName }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left: 4px solid #67C23A;">
          <div class="stat-title">当前角色</div>
          <div class="stat-value">{{ userInfo.roleName || '-' }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left: 4px solid #E6A23C;">
          <div class="stat-title">权限数量</div>
          <div class="stat-value">{{ perms.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="border-left: 4px solid #F56C6C;">
          <div class="stat-title">登录用户</div>
          <div class="stat-value">{{ userInfo.realName || userInfo.username || '-' }}</div>
        </div>
      </el-col>
    </el-row>
    <el-card class="welcome-card" shadow="never">
      <h3>欢迎使用佳士售后管理系统</h3>
      <p>本系统用于管理公司售后服务业务，包括工单管理、服务网点管理、设备管理等功能。</p>
      <p>请通过左侧菜单导航至对应功能模块。</p>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Dashboard',
  computed: {
    ...mapGetters(['userInfo', 'perms', 'companies', 'currentCompanyId']),
    currentCompanyName() {
      const found = this.companies.find(c => c.id === this.currentCompanyId)
      return found ? found.companyName : '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  .stat-card {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

    .stat-title {
      font-size: 13px;
      color: #909399;
      margin-bottom: 8px;
    }
    .stat-value {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }

  .welcome-card {
    margin-top: 8px;
    h3 { margin: 0 0 12px; color: #303133; }
    p { color: #606266; font-size: 14px; line-height: 1.8; margin: 4px 0; }
  }
}
</style>
