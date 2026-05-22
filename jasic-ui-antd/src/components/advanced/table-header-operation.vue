<script setup lang="ts">
/**
 * 列表页顶栏常用操作区：新增、批量删除、刷新与列设置（`columns` 为 v-model 与 TableColumnSetting 联动）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { $t } from '@/locales';

defineOptions({
  name: 'TableHeaderOperation'
});

interface Props {
  disabledDelete?: boolean;
  loading?: boolean;
}

defineProps<Props>();

interface Emits {
  (e: 'add'): void;
  (e: 'delete'): void;
  (e: 'refresh'): void;
}

const emit = defineEmits<Emits>();

// 表格列显隐与排序状态，交给 TableColumnSetting 编辑
const columns = defineModel<AntDesign.TableColumnCheck[]>('columns', {
  default: () => []
});

/**
 * 触发新增（由父级打开表单或跳转）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function add() {
  emit('add');
}

/**
 * 批量删除：由父级在 Popconfirm 确认后执行接口
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function batchDelete() {
  emit('delete');
}

/**
 * 刷新列表数据
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function refresh() {
  emit('refresh');
}
</script>

<template>
  <div class="flex flex-wrap justify-end gap-x-12px gap-y-8px lt-sm:(w-200px py-12px)">
    <!-- 通用组件：table-header-operation -->
    <slot name="prefix"></slot>
    <slot name="default">
      <AButton size="small" ghost type="primary" @click="add">
        <template #icon>
          <icon-ic-round-plus class="align-sub text-icon" />
        </template>
        <span class="ml-8px">{{ $t('common.add') }}</span>
      </AButton>
      <APopconfirm :title="$t('common.confirmDelete')" :disabled="disabledDelete" @confirm="batchDelete">
        <AButton size="small" danger :disabled="disabledDelete">
          <template #icon>
            <icon-ic-round-delete class="align-sub text-icon" />
          </template>

          <span class="ml-8px">{{ $t('common.batchDelete') }}</span>
        </AButton>
      </APopconfirm>
    </slot>
    <AButton size="small" @click="refresh">
      <template #icon>
        <icon-mdi-refresh class="align-sub text-icon" :class="{ 'animate-spin': loading }" />
      </template>
      <span class="ml-8px">{{ $t('common.refresh') }}</span>
    </AButton>
    <TableColumnSetting v-model:columns="columns" />
    <slot name="suffix"></slot>
  </div>
</template>

<style scoped></style>
