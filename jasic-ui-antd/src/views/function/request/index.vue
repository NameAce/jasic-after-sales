<script setup lang="ts">
/**
 * 菜单演示 — 请求异常：触发业务错误码与登出提示，用于联调全局错误处理。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { fetchCustomBackendError } from '@/service/api';
import { $t } from '@/locales';

/**
 * 作用：触发自定义业务码 8888，演示全局登出提示。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function logout() {
  await fetchCustomBackendError('8888', $t('request.logoutMsg'));
}

/**
 * 作用：触发 7777 演示带 Modal 的登出。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function logoutWithModal() {
  await fetchCustomBackendError('7777', $t('request.logoutWithModalMsg'));
}

/**
 * 作用：触发 9999 演示 Token 过期刷新流程。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function refreshToken() {
  await fetchCustomBackendError('9999', $t('request.tokenExpired'));
}

/**
 * 作用：并发相同错误码请求，演示 Message 合并去重。
 * @param 无
 * @returns 返回 Promise，全部请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleRepeatedMessageError() {
  await Promise.all([
    fetchCustomBackendError('2222', $t('page.function.request.repeatedErrorMsg1')),
    fetchCustomBackendError('2222', $t('page.function.request.repeatedErrorMsg1')),
    fetchCustomBackendError('2222', $t('page.function.request.repeatedErrorMsg1')),
    fetchCustomBackendError('3333', $t('page.function.request.repeatedErrorMsg2')),
    fetchCustomBackendError('3333', $t('page.function.request.repeatedErrorMsg2')),
    fetchCustomBackendError('3333', $t('page.function.request.repeatedErrorMsg2'))
  ]);
}

/**
 * 作用：并发 Modal 类错误，演示弹窗合并策略。
 * @param 无
 * @returns 返回 Promise，全部请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleRepeatedModalError() {
  await Promise.all([
    fetchCustomBackendError('7777', $t('request.logoutWithModalMsg')),
    fetchCustomBackendError('7777', $t('request.logoutWithModalMsg')),
    fetchCustomBackendError('7777', $t('request.logoutWithModalMsg'))
  ]);
}
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <!-- 功能演示页：request/index.vue -->
    <ACard :title="$t('request.logout')" :bordered="false" size="small" class="card-wrapper">
      <AButton @click="logout">{{ $t('common.trigger') }}</AButton>
    </ACard>
    <ACard :title="$t('request.logoutWithModal')" :bordered="false" size="small" class="card-wrapper">
      <AButton @click="logoutWithModal">{{ $t('common.trigger') }}</AButton>
    </ACard>
    <ACard :title="$t('request.refreshToken')" :bordered="false" size="small" class="card-wrapper">
      <AButton @click="refreshToken">{{ $t('common.trigger') }}</AButton>
    </ACard>
    <ACard
      :title="$t('page.function.request.repeatedErrorOccurOnce')"
      :bordered="false"
      size="small"
      class="card-wrapper"
    >
      <AButton @click="handleRepeatedMessageError">{{ $t('page.function.request.repeatedError') }}(Message)</AButton>
      <AButton class="ml-12px" @click="handleRepeatedModalError">
        {{ $t('page.function.request.repeatedError') }}(Modal)
      </AButton>
    </ACard>
  </ASpace>
</template>

<style scoped></style>
