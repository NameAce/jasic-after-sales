<script setup lang="ts">
/**
 * BetterScroll 封装：根据父级传入 options 创建实例，尺寸变化时 refresh，并 expose `instance`。
 */
import { computed, onMounted, ref, watch } from 'vue';
import { useElementSize } from '@vueuse/core';
import BScroll from '@better-scroll/core';
import type { Options } from '@better-scroll/core';

defineOptions({ name: 'BetterScroll' });

interface Props {
  /**
   * BetterScroll options
   *
   * @link https://better-scroll.github.io/docs/zh-CN/guide/base-scroll-options.html
   */
  options: Options;
}

const props = defineProps<Props>();

const bsWrapper = ref<HTMLElement>();
const bsContent = ref<HTMLElement>();
const { width: wrapWidth } = useElementSize(bsWrapper);
const { width, height } = useElementSize(bsContent);

const instance = ref<BScroll>();
// 是否启用纵向滚动（影响容器高度计算）
const isScrollY = computed(() => Boolean(props.options.scrollY));

/** 在挂载节点上实例化 BScroll */
function initBetterScroll() {
  if (!bsWrapper.value) return;
  instance.value = new BScroll(bsWrapper.value, props.options);
}

// 包装层或内容尺寸变化时 refresh，避免滚动区域留白/卡死
watch([() => wrapWidth.value, () => width.value, () => height.value], () => {
  instance.value?.refresh();
});

onMounted(() => {
  initBetterScroll();
});

defineExpose({ instance });
</script>

<template>
  <div ref="bsWrapper" class="h-full text-left">
    <div ref="bsContent" class="inline-block" :class="{ 'h-full': !isScrollY }">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped></style>
