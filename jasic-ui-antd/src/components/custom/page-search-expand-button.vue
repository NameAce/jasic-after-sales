<script setup lang="ts">
/**
 * 列表页搜索工具栏：在「重置」旁展开/收起筛选项。
 * 仅当父级 `v-if="…showSearchFilterExpandToggle"`（筛选项多于 4 个）时渲染本组件。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { DownOutlined, UpOutlined } from '@ant-design/icons-vue';

defineOptions({
  name: 'PageSearchExpandButton'
});

defineProps<{
  expanded: boolean;
}>();

const emit = defineEmits<{
  click: [];
}>();
</script>

<template>
  <ATooltip placement="top" :title="expanded ? '收起筛选' : '展开筛选'">
    <!-- 通用组件：page-search-expand-button -->
    <AButton
      type="text"
      shape="circle"
      size="small"
      class="page-search-expand-trigger"
      :aria-expanded="expanded"
      :aria-label="expanded ? '收起筛选条件' : '展开更多筛选条件'"
      @click="emit('click')"
    >
      <DownOutlined v-if="!expanded" />
      <UpOutlined v-else />
    </AButton>
  </ATooltip>
</template>

<style scoped>
.page-search-expand-trigger {
  flex-shrink: 0;
  width: 32px;
  min-width: 32px !important;
  height: 32px;
  padding: 0 !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.45));
}

.page-search-expand-trigger:hover,
.page-search-expand-trigger:focus-visible {
  color: var(--ant-color-primary, #1677ff);
  background: var(--ant-color-fill-tertiary, rgba(0, 0, 0, 0.04)) !important;
}

.page-search-expand-trigger :deep(.anticon) {
  font-size: 14px;
}
</style>
