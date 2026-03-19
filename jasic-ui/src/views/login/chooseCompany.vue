<template>
  <div class="choose-container">
    <div class="choose-card">
      <h2>选择操作公司</h2>
      <p class="tip">您在多个公司中拥有账号，请选择要操作的公司</p>
      <div class="company-list">
        <div
          v-for="c in companies"
          :key="c.id"
          class="company-item"
          :class="{ active: selected === c.id }"
          @click="selected = c.id"
        >
          <i class="el-icon-office-building" />
          <div class="company-info">
            <span class="name">{{ c.companyName }}</span>
            <span class="type">{{ c.typeName || '' }}</span>
          </div>
          <i v-if="selected === c.companyId" class="el-icon-check" />
        </div>
      </div>
      <el-button
        type="primary"
        :disabled="!selected"
        :loading="loading"
        style="width: 100%; margin-top: 20px;"
        @click="handleChoose"
      >
        确认进入
      </el-button>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'ChooseCompany',
  data() {
    return {
      selected: null,
      loading: false
    }
  },
  computed: {
    ...mapGetters(['companies'])
  },
  created() {
    if (!this.companies || this.companies.length === 0) {
      this.$router.push('/login')
    }
  },
  methods: {
    handleChoose() {
      this.loading = true
      this.$store.dispatch('user/chooseCompany', this.selected)
        .then(() => {
          this.$router.push('/').catch(() => {})
        })
        .catch(() => {})
        .finally(() => { this.loading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
.choose-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1d3557 0%, #457b9d 50%, #a8dadc 100%);

  .choose-card {
    width: 460px;
    padding: 36px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);

    h2 {
      margin: 0 0 8px;
      font-size: 20px;
      color: #303133;
      text-align: center;
    }

    .tip {
      text-align: center;
      color: #909399;
      font-size: 13px;
      margin-bottom: 24px;
    }

    .company-list {
      max-height: 320px;
      overflow-y: auto;

      .company-item {
        display: flex;
        align-items: center;
        padding: 14px 16px;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        margin-bottom: 10px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover { border-color: #409EFF; background: #f5f7fa; }
        &.active { border-color: #409EFF; background: #ecf5ff; }

        > .el-icon-office-building {
          font-size: 24px;
          color: #409EFF;
          margin-right: 12px;
        }

        .company-info {
          flex: 1;
          .name { display: block; font-size: 14px; color: #303133; font-weight: 500; }
          .type { display: block; font-size: 12px; color: #909399; margin-top: 2px; }
        }

        .el-icon-check {
          color: #409EFF;
          font-size: 18px;
          font-weight: bold;
        }
      }
    }
  }
}
</style>
