<script lang="ts" setup>
/**
 * 403/404/500 异常占位页：大图 + 可选接口文案（query.msg）+ 回首页。
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
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

const route = useRoute();
const { routerPushByKey } = useRouterPush();

/** 请求拦截跳转时通过 query.msg 传入的后端文案 */
const apiMessage = computed(() => {
  const raw = route.query.msg;
  const text = Array.isArray(raw) ? raw[0] : raw;
  return typeof text === 'string' ? text.trim() : '';
});

const iconMap: Record<ExceptionType, string> = {
  '403': 'no-permission',
  '404': 'not-found',
  '500': 'service-error'
};

// 异常类型对应本地雪碧图中的 symbol 名
const icon = computed(() => iconMap[props.type]);
</script>

<template>
  <div class="size-full min-h-520px flex-col-center gap-24px overflow-hidden px-24px">
    <div class="flex text-400px text-primary">
      <SvgIcon :local-icon="icon" />
    </div>
    <div v-if="apiMessage" class="max-w-560px break-words text-center text-16px text-gray-700 leading-relaxed">
      {{ apiMessage }}
    </div>
    <AButton type="primary" @click="routerPushByKey('root')">{{ $t('common.backToHome') }}</AButton>
  </div>
</template>

<style scoped></style>
