<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';

defineOptions({ name: 'ChooseCompany' });

const router = useRouter();
const authStore = useAuthStore();
const { toHome } = useRouterPush();

const selected = ref<Api.Common.IdLike | null>(null);
const loading = ref(false);

onMounted(async () => {
  if (!authStore.needChooseCompany) {
    await router.replace({ name: 'root' }).catch(() => {});
    return;
  }
  if (!authStore.companyOptions.length) {
    await authStore.initUserInfo();
    if (!authStore.needChooseCompany) {
      await router.replace({ name: 'root' }).catch(() => {});
      return;
    }
    if (!authStore.companyOptions.length) {
      await router.push({ name: 'login', query: { module: 'pwd-login' } }).catch(() => {});
    }
  }
});

async function handleChoose() {
  if (selected.value === null) return;
  loading.value = true;
  try {
    const pass = await authStore.chooseCompany(selected.value);
    if (!pass) return;
    // 与 jasic-ui chooseCompany.vue：登录后选公司完毕固定进入 `/`
    await toHome();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="choose-container">
    <div class="choose-card">
      <h2>选择操作公司</h2>
      <p class="tip">您在多个公司中拥有账号，请选择要操作的公司</p>
      <div class="company-list">
        <div
          v-for="c in authStore.companyOptions"
          :key="String(c.id)"
          class="company-item"
          :class="{ active: selected === c.id }"
          @click="selected = c.id"
        >
          <span class="i-carbon-building text-24px text-primary" />
          <div class="company-info">
            <span class="name">{{ c.companyName }}</span>
            <span class="type">{{ c.typeName || '' }}</span>
          </div>
          <span v-if="selected === c.id" class="i-carbon-checkmark text-18px text-primary font-bold" />
        </div>
      </div>
      <AButton
        type="primary"
        :disabled="selected === null"
        :loading="loading"
        block
        class="mt-20px"
        @click="handleChoose"
      >
        确认进入
      </AButton>
    </div>
  </div>
</template>

<style scoped>
.choose-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1d3557 0%, #457b9d 50%, #a8dadc 100%);
}

.choose-card {
  width: 460px;
  padding: 36px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.choose-card h2 {
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
}

.company-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.company-item:hover {
  border-color: #1890ff;
  background: #f5f7fa;
}

.company-item.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.company-info {
  flex: 1;
  margin-left: 12px;
}

.company-info .name {
  display: block;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.company-info .type {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
