<script setup lang="ts">
/**
 * 顶栏用户区：未登录显示登录/注册；已登录下拉含个人中心与退出（带确认）。
 */
import { Modal } from 'ant-design-vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

defineOptions({
  name: 'UserAvatar'
});

const authStore = useAuthStore();
const { routerPushByKey, toLogin } = useRouterPush();

/** 跳转登录页（或注册入口由路由决定） */
function loginOrRegister() {
  toLogin();
}

/**
 * 退出登录前二次确认。
 *
 * 关键时序约束：
 * 1. 弹确认框前先 `Modal.destroyAll()`，把页面上可能残留的其它 imperative Modal（如业务弹窗）清理掉。
 *    这一刀只针对「打开退出确认」这一刻执行，不会和「退出确认」自身的 leave 过渡竞态。
 * 2. `onOk` 返回 logout 的 Promise，由 ant-design-vue 在 logout 完成后再触发 leave 过渡 → 自然卸载，
 *    避免在 leave 过渡期间再调 destroyAll 导致 useScrollLocker 计数错乱，进而 `<body>` 的 overflow 锁屏样式残留、整页点不动。
 */
function logout() {
  // 清理可能存在的其它 imperative Modal，避免它们叠在退出流程上造成遮罩残留
  Modal.destroyAll();

  Modal.confirm({
    title: $t('common.tip'),
    content: $t('common.logoutConfirm'),
    okText: $t('common.confirm'),
    cancelText: $t('common.cancel'),
    async onOk() {
      // 返回 Promise 让 Modal 自身的 visible 状态在 logout 完成后再切换，由 ant-design-vue 接管 leave 过渡与卸载
      await authStore.logout();
    }
  });
}
</script>

<template>
  <AButton v-if="!authStore.isLogin" @click="loginOrRegister">{{ $t('page.login.common.loginOrRegister') }}</AButton>
  <ADropdown v-else placement="bottomRight" trigger="click">
    <ButtonIcon>
      <SvgIcon icon="ph:user-circle" class="text-icon-large" />
      <span class="text-16px font-medium">{{ authStore.userInfo.userName }}</span>
    </ButtonIcon>
    <template #overlay>
      <AMenu>
        <AMenuItem @click="routerPushByKey('user-center')">
          <div class="flex-center gap-8px">
            <SvgIcon icon="ph:user-circle" class="text-icon" />
            {{ $t('common.userCenter') }}
          </div>
        </AMenuItem>
        <AMenuDivider />
        <AMenuItem @click="logout">
          <div class="flex-center gap-8px">
            <SvgIcon icon="ph:sign-out" class="text-icon" />
            {{ $t('common.logout') }}
          </div>
        </AMenuItem>
      </AMenu>
    </template>
  </ADropdown>
</template>

<style scoped></style>
