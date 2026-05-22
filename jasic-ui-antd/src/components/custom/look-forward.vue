<script setup lang="ts">
/**
 * 「敬请期待」占位页：用于未完成菜单或演示路由，可通过默认插槽替换标题。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { $t } from '@/locales';

defineOptions({
  name: 'LookForward'
});
</script>

<template>
  <div class="size-full min-h-520px flex-col-center gap-24px overflow-hidden">
    <!-- 通用组件：look-forward -->
    <div class="flex text-400px text-primary">
      <SvgIcon local-icon="expectation" />
    </div>
    <slot>
      <h3 class="text-28px text-primary font-500">{{ $t('common.lookForward') }}</h3>
    </slot>
  </div>
</template>

<style scoped></style>
