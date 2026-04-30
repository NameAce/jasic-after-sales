<script setup lang="ts">
/**
 * 角色授权 — 菜单权限弹窗：加载菜单树并回显角色已分配菜单。
 */
import { computed, shallowRef, watch } from 'vue';
import type { SelectProps } from 'ant-design-vue';
import type { DataNode } from 'ant-design-vue/es/tree';
import { fetchGetAllPages, fetchGetMenuTree } from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'MenuAuthModal'
});

interface Props {
  /** 当前角色主键 */
  roleId: number;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', {
  default: false
});

/**
 * 作用：关闭菜单授权抽屉。
 * @param 无
 * @returns {void} 无
 */
function closeModal() {
  visible.value = false;
}

// 抽屉标题
const title = computed(() => $t('common.edit') + $t('page.manage.role.menuAuth'));

// 登录后首页路由占位值
const home = shallowRef('');

/**
 * 作用：加载角色首页路由配置（示例为占位）。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getHome() {
  console.log(props.roleId);

  home.value = 'home';
}

/**
 * 作用：更新角色绑定首页（示例占位）。
 * @param val - 选中路由值
 * @returns 返回 Promise，更新本地状态后结束
 */
async function updateHome(val: SelectProps['value']) {
  // request

  home.value = val as string;
}

// 全部页面路由选项原始列表
const pages = shallowRef<string[]>([]);

/**
 * 作用：拉取全部页面路径列表。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getPages() {
  const { error, data } = await fetchGetAllPages();

  if (!error) {
    pages.value = data;
  }
}

// 首页下拉选项
const pageSelectOptions = computed(() => {
  const opts: CommonType.Option[] = pages.value.map(page => ({
    label: page,
    value: page
  }));

  return opts;
});

// 菜单树数据
const tree = shallowRef<DataNode[]>([]);

/**
 * 作用：请求并转换菜单树为 Tree 组件结构。
 * @param 无
 * @returns 返回 Promise，加载结束后结束
 */
async function getTree() {
  const { error, data } = await fetchGetMenuTree();

  if (!error) {
    tree.value = recursiveTransform(data);
  }
}

/**
 * 作用：递归转换后端菜单树为 Ant Design Tree 节点。
 * @param data - 菜单树
 * @returns Tree DataNode 数组
 */
function recursiveTransform(data: Api.SystemManage.MenuTree[]): DataNode[] {
  return data.map(item => {
    const { id: key, label } = item;

    if (item.children) {
      return {
        key,
        title: label,
        children: recursiveTransform(item.children)
      };
    }

    return {
      key,
      title: label
    };
  });
}

// 已勾选菜单 id
const checks = shallowRef<number[]>([]);

/**
 * 作用：加载角色已授权菜单 id（示例为 Mock）。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getChecks() {
  console.log(props.roleId);
  // request
  checks.value = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21];
}

/**
 * 作用：提交菜单勾选（示例仅提示成功）。
 * @param 无
 * @returns {void} 无
 */
function handleSubmit() {
  console.log(checks.value, props.roleId);
  // request

  window.$message?.success?.($t('common.modifySuccess'));

  closeModal();
}

/**
 * 作用：打开弹窗时并行初始化首页、页面列表、树与勾选。
 * @param 无
 * @returns 返回 Promise，初始化结束后结束
 */
async function init() {
  getHome();
  getPages();
  await getTree();
  await getChecks();
}

// visible 为 true 时拉取数据
watch(visible, val => {
  if (val) {
    init();
  }
});
</script>

<template>
  <ADrawer v-model:open="visible" :title="title" :width="480">
    <div class="flex-y-center gap-16px pb-12px">
      <div>{{ $t('page.manage.menu.home') }}</div>
      <ASelect :value="home" :options="pageSelectOptions" class="w-240px" @update:value="updateHome" />
    </div>
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
