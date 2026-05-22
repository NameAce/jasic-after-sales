<script lang="ts" setup>
/**
 * 混合子菜单「固定/浮动」切换：图钉开为固定侧栏子区。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { $t } from '@/locales';

defineOptions({ name: 'PinToggler' });

interface Props {
  pin?: boolean;
}

const props = defineProps<Props>();

// 固定/取消固定侧栏时切换图标
const icon = computed(() => (props.pin ? 'mdi-pin-off' : 'mdi-pin'));
</script>

<template>
  <!-- 通用组件：pin-toggler -->
  <ButtonIcon :tooltip-content="pin ? $t('icon.unpin') : $t('icon.pin')" tooltip-placement="bottomLeft" trigger-parent>
    <SvgIcon :icon="icon" />
  </ButtonIcon>
</template>

<style scoped></style>
