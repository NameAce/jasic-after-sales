<script setup lang="ts">
/**
 * 角色授权 — 按钮权限弹窗：按角色勾选接口级/按钮级权限树。
 */
import { computed, shallowRef } from 'vue';
import type { DataNode } from 'ant-design-vue/es/tree';
import { $t } from '@/locales';

defineOptions({
  name: 'ButtonAuthModal'
});

interface Props {
  /** 角色 id */
  roleId: number;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', {
  default: false
});

/**
 * 作用：关闭按钮授权抽屉。
 * @param 无
 * @returns {void} 无
 */
function closeModal() {
  visible.value = false;
}

// 抽屉标题
const title = computed(() => $t('common.edit') + $t('page.manage.role.buttonAuth'));

// 按钮权限树（示例 Mock）
const tree = shallowRef<DataNode[]>([]);

/**
 * 作用：加载全部按钮节点（示例 Mock）。
 * @param 无
 * @returns 返回 Promise，本地树更新后结束
 */
async function getAllButtons() {
  // request
  tree.value = [
    { key: 1, title: 'button1', code: 'code1' },
    { key: 2, title: 'button2', code: 'code2' },
    { key: 3, title: 'button3', code: 'code3' },
    { key: 4, title: 'button4', code: 'code4' },
    { key: 5, title: 'button5', code: 'code5' },
    { key: 6, title: 'button6', code: 'code6' },
    { key: 7, title: 'button7', code: 'code7' },
    { key: 8, title: 'button8', code: 'code8' },
    { key: 9, title: 'button9', code: 'code9' },
    { key: 10, title: 'button10', code: 'code10' }
  ];
}

// 已勾选按钮节点 id
const checks = shallowRef<number[]>([]);

/**
 * 作用：加载角色已选按钮（示例 Mock）。
 * @param 无
 * @returns 返回 Promise，本地状态更新后结束
 */
async function getChecks() {
  if (!Number.isFinite(props.roleId)) {
    checks.value = [];
    return;
  }
  // request
  checks.value = [1, 2, 3, 4, 5];
}

/**
 * 作用：提交按钮勾选（示例仅提示）。
 * @param 无
 * @returns {void} 无
 */
function handleSubmit() {
  if (!Number.isFinite(props.roleId)) {
    return;
  }
  // request

  window.$message?.success?.($t('common.modifySuccess'));

  closeModal();
}

/**
 * 作用：初始化按钮树与勾选状态。
 * @param 无
 * @returns {void} 无
 */
function init() {
  getAllButtons();
  getChecks();
}

// 组件创建时拉取示例数据
init();
</script>

<template>
  <ADrawer v-model:open="visible" :title="title" :width="480">
    <ATree v-model:checked-keys="checks" :tree-data="tree" checkable :height="280" class="h-280px" />
    <template #footer>
      <AButton size="small" class="mt-16px" @click="closeModal">
        {{ $t('common.cancel') }}
      </AButton>
      <AButton type="primary" size="small" class="mt-16px" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </AButton>
    </template>
  </ADrawer>
</template>

<style scoped></style>
