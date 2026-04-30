<script setup lang="ts">
/**
 * 菜单演示 — 切换登录账号：用于验证不同角色下菜单与权限展示（非生产逻辑）。
 */
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useLoading } from '@sa/hooks';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';
import { useTabStore } from '@/store/modules/tab';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

// 演示切换账号所用路由与全局状态
const route = useRoute();
const appStore = useAppStore();
const authStore = useAuthStore();
const tabStore = useTabStore();
const { hasAuth } = useAuth();
const { loading, startLoading, endLoading } = useLoading();

type AccountKey = 'super' | 'admin' | 'user';

interface Account {
  key: AccountKey;
  label: string;
  userName: string;
  password: string;
}

// 演示用快捷登录账号列表
const accounts = computed<Account[]>(() => [
  {
    key: 'super',
    label: $t('page.login.pwdLogin.superAdmin'),
    userName: 'Super',
    password: '123456'
  },
  {
    key: 'admin',
    label: $t('page.login.pwdLogin.admin'),
    userName: 'Admin',
    password: '123456'
  },
  {
    key: 'user',
    label: $t('page.login.pwdLogin.user'),
    userName: 'User',
    password: '123456'
  }
]);

// 当前正在切换的账号 key（用于按钮 loading）
const loginAccount = ref<AccountKey>('super');

/**
 * 作用：切换演示账号并重新登录、初始化 Tab 后整页刷新。
 * @param account - 目标账号配置
 * @returns 返回 Promise，登录与刷新流程结束后结束
 */
async function handleToggleAccount(account: Account) {
  loginAccount.value = account.key;

  startLoading();
  await authStore.login(account.userName, account.password, false);
  tabStore.initTabStore(route);
  endLoading();
  appStore.reloadPage();
}
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <ACard :title="$t('route.function_toggle-auth')" :bordered="false" size="small" class="card-wrapper">
      <ADescriptions layout="vertical" bordered size="small" :column="1">
        <ADescriptionsItem :label="$t('page.manage.user.userRole')">
          <ASpace>
            <ATag v-for="role in authStore.userInfo.roles" :key="role">{{ role }}</ATag>
          </ASpace>
        </ADescriptionsItem>
        <ADescriptionsItem ions-item :label="$t('page.function.toggleAuth.toggleAccount')">
          <ASpace>
            <AButton
              v-for="account in accounts"
              :key="account.key"
              :loading="loading && loginAccount === account.key"
              :disabled="loading && loginAccount !== account.key"
              @click="handleToggleAccount(account)"
            >
              {{ account.label }}
            </AButton>
          </ASpace>
        </ADescriptionsItem>
      </ADescriptions>
    </ACard>
    <ACard :title="$t('page.function.toggleAuth.authHook')" :bordered="false" size="small" class="card-wrapper">
      <ASpace>
        <AButton v-if="hasAuth('B_CODE1')">{{ $t('page.function.toggleAuth.superAdminVisible') }}</AButton>
        <AButton v-if="hasAuth('B_CODE2')">{{ $t('page.function.toggleAuth.adminVisible') }}</AButton>
        <AButton v-if="hasAuth('B_CODE3')">
          {{ $t('page.function.toggleAuth.adminOrUserVisible') }}
        </AButton>
      </ASpace>
    </ACard>
  </ASpace>
</template>

<style scoped></style>
