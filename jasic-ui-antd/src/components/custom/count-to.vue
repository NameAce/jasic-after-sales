<script setup lang="ts">
/**
 * 数字递增动画展示：基于 @vueuse/useTransition，支持千分位与小数位格式化。
 */
import { computed, nextTick, ref, watch } from 'vue';
import { TransitionPresets, useTransition } from '@vueuse/core';

defineOptions({
  name: 'CountTo'
});

interface Props {
  startValue?: number;
  endValue?: number;
  duration?: number;
  autoplay?: boolean;
  decimals?: number;
  prefix?: string;
  suffix?: string;
  separator?: string;
  decimal?: string;
  useEasing?: boolean;
  transition?: keyof typeof TransitionPresets;
}

const props = withDefaults(defineProps<Props>(), {
  startValue: 0,
  endValue: 2021,
  duration: 1500,
  autoplay: true,
  decimals: 0,
  prefix: '',
  suffix: '',
  separator: ',',
  decimal: '.',
  useEasing: true,
  transition: 'linear'
});

const source = ref(props.startValue);

// useTransition 使用的缓动曲线（可关闭 easing）
const transition = computed(() => (props.useEasing ? TransitionPresets[props.transition] : undefined));

const outputValue = useTransition(source, {
  disabled: false,
  duration: props.duration,
  transition: transition.value
});

// 过渡中的数值格式化为带千分位的展示字符串
const value = computed(() => formatValue(outputValue.value));

/** 按 props 中的前后缀、小数点与千分位规则格式化数值 */
function formatValue(num: number) {
  const { decimals, decimal, separator, suffix, prefix } = props;

  let number = num.toFixed(decimals);
  number = String(number);

  const x = number.split('.');
  let x1 = x[0];
  const x2 = x.length > 1 ? decimal + x[1] : '';
  const rgx = /(\d+)(\d{3})/;
  if (separator) {
    while (rgx.test(x1)) {
      x1 = x1.replace(rgx, `$1${separator}$2`);
    }
  }

  return prefix + x1 + x2 + suffix;
}

/** 将动画源值设到 endValue，触发展示过渡 */
async function start() {
  await nextTick();
  source.value = props.endValue;
}

// 起止值变化且 autoplay 时重新跑递增动画
watch(
  [() => props.startValue, () => props.endValue],
  () => {
    if (props.autoplay) {
      start();
    }
  },
  { immediate: true }
);
</script>

<template>
  <span>{{ value }}</span>
</template>

<style scoped></style>
