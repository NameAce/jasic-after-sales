<script lang="ts" setup>
/**
 * 403/404/500 异常占位页：大图 + 回首页，配合内置错误路由使用。
 */
import { computed } from 'vue';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

defineOptions({ name: 'ExceptionBase' });

type ExceptionType = '403' | '404' | '500';

interface Props {
  /**
   * Exception type
   *
   * - 403: no permission
   * - 404: not found
   * - 500: service error
   */
  type: ExceptionType;
}

const props = defineProps<Props>();

const { routerPushByKey } = useRouterPush();

const iconMap: Record<ExceptionType, string> = {
  '403': 'no-permission',
  '404': 'not-found',
  '500': 'service-error'
};

// 异常类型对应本地雪碧图中的 symbol 名
const icon = computed(() => iconMap[props.type]);
</script>

<template>
  <div class="size-full min-h-520px flex-col-center gap-24px overflow-hidden">
    <div class="flex text-400px text-primary">
      <SvgIcon :local-icon="icon" />
    </div>
    <AButton type="primary" @click="routerPushByKey('root')">{{ $t('common.backToHome') }}</AButton>
  </div>
</template>

<style scoped></style>
