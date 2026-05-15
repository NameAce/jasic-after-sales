<script setup lang="tsx">
/**
 * 管理端演示 — 菜单管理：树形表格、路由页选择与菜单增删改（对接 Mock/演示接口）。
 */
import { ref } from 'vue';
import type { Ref } from 'vue';
import { Button, Popconfirm, Tag } from 'ant-design-vue';
import { useBoolean } from '@sa/hooks';
import { tagColorEnabled } from '@/constants/list-status-tag';
import { yesOrNoRecord } from '@/constants/common';
import { enableStatusRecord, menuTypeRecord } from '@/constants/business';
import { fetchGetAllPages, fetchGetMenuList } from '@/service/api';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { useTable, useTableOperate, useTableScroll } from '@/hooks/common/table';
import { $t } from '@/locales';
import SvgIcon from '@/components/custom/svg-icon.vue';
import MenuOperateModal, { type OperateType } from './modules/menu-operate-modal.vue';

// 菜单表格、弹窗显隐与路由页面选项
const { bool: visible, setTrue: openModal } = useBoolean();
const { tableWrapperRef, scrollConfig } = useTableScroll();
const pageMenuTitle = useRouteMenuTitle();

const {
  columns,
  columnChecks,
  data,
  loading,
  pagination,
  tableListLocale,
  getData,
  getDataByPage
} = useTable({
  apiFn: fetchGetMenuList,
  columns: () => [
    {
      key: 'id',
      title: $t('page.manage.menu.id'),
      align: 'center',
      dataIndex: 'id'
    },
    {
      key: 'menuType',
      title: $t('page.manage.menu.menuType'),
      align: 'center',
      width: 80,
      customRender: ({ record }) => {
        const tagMap: Record<Api.SystemManage.MenuType, string> = {
          1: 'default',
          2: 'processing'
        };

        const label = $t(menuTypeRecord[record.menuType]);

        return <Tag color={tagMap[record.menuType]}>{label}</Tag>;
      }
    },
    {
      key: 'menuName',
      title: $t('page.manage.menu.menuName'),
      align: 'center',
      minWidth: 120,
      customRender: ({ record }) => {
        const { i18nKey, menuName } = record;

        const label = i18nKey ? $t(i18nKey) : menuName;

        return <span>{label}</span>;
      }
    },
    {
      key: 'icon',
      title: $t('page.manage.menu.icon'),
      align: 'center',
      width: 60,
      customRender: ({ record }) => {
        const icon = record.iconType === '1' ? record.icon : undefined;

        const localIcon = record.iconType === '2' ? record.icon : undefined;

        return (
          <div class="flex-center">
            <SvgIcon icon={icon} localIcon={localIcon} class="text-icon" />
          </div>
        );
      }
    },
    {
      key: 'routeName',
      title: $t('page.manage.menu.routeName'),
      align: 'center',
      dataIndex: 'routeName',
      minWidth: 120
    },
    {
      key: 'routePath',
      title: $t('page.manage.menu.routePath'),
      align: 'center',
      dataIndex: 'routePath',
      minWidth: 120
    },
    {
      key: 'status',
      title: $t('page.manage.menu.menuStatus'),
      align: 'center',
      width: 80,
      customRender: ({ record }) => {
        if (record.status === null) {
          return null;
        }

        const label = $t(enableStatusRecord[record.status]);

        return <Tag color={tagColorEnabled(record.status === '1')}>{label}</Tag>;
      }
    },
    {
      key: 'hideInMenu',
      title: $t('page.manage.menu.hideInMenu'),
      dataIndex: 'hideInMenu',
      align: 'center',
      width: 80,
      customRender: ({ record }) => {
        const hide: CommonType.YesOrNo = record.hideInMenu ? 'Y' : 'N';

        const tagMap: Record<CommonType.YesOrNo, string> = {
          Y: 'error',
          N: 'default'
        };

        const label = $t(yesOrNoRecord[hide]);

        return <Tag color={tagMap[hide]}>{label}</Tag>;
      }
    },
    {
      key: 'parentId',
      dataIndex: 'parentId',
      title: $t('page.manage.menu.parentId'),
      width: 90,
      align: 'center'
    },
    {
      key: 'order',
      dataIndex: 'order',
      title: $t('page.manage.menu.order'),
      align: 'center',
      width: 60
    },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 230,
      customRender: ({ record }) => (
        <div class="flex-center justify-end gap-8px">
          {record.menuType === '1' && (
            <Button
              type="link"
              size="small"
              class="table-action-link--processing"
              onClick={() => handleAddChildMenu(record)}
            >
              {$t('page.manage.menu.addChildMenu')}
            </Button>
          )}
          <Button type="link" size="small" class="table-action-link--primary" onClick={() => handleEdit(record)}>
            {$t('common.edit')}
          </Button>
          <Popconfirm title={$t('common.confirmDelete')} onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>
              {$t('common.delete')}
            </Button>
          </Popconfirm>
        </div>
      )
    }
  ]
});

const { checkedRowKeys, rowSelection, onBatchDeleted, onDeleted } = useTableOperate(data, getData);

// 菜单操作弹窗类型
const operateType = ref<OperateType>('add');
// 传入菜单编辑抽屉的当前行（新增子级时为父节点）
const editingData: Ref<Api.SystemManage.Menu | null> = ref(null);

/**
 * 作用：打开新增菜单弹窗。
 * @param 无
 * @returns {void} 无
 */
function handleAdd() {
  operateType.value = 'add';
  openModal();
}

/**
 * 作用：批量删除菜单（示例占位）。
 * @param 无
 * @returns 返回 Promise，回调结束后结束
 */
async function handleBatchDelete() {
  // request

  onBatchDeleted();
}

/**
 * 作用：删除单条菜单（示例占位）。
 * @param id - 菜单 id
 * @returns {void} 无
 */
function handleDelete(id: number) {
  // request
  console.log(id);

  onDeleted();
}

/**
 * 作用：打开编辑菜单弹窗。
 * @param item - 菜单行
 * @returns {void} 无
 */
function handleEdit(item: Api.SystemManage.Menu) {
  operateType.value = 'edit';
  editingData.value = { ...item };

  openModal();
}

/**
 * 作用：为指定父级打开新增子菜单弹窗。
 * @param item - 父菜单
 * @returns {void} 无
 */
function handleAddChildMenu(item: Api.SystemManage.Menu) {
  operateType.value = 'addChild';

  editingData.value = { ...item };

  openModal();
}

// 系统内全部页面路径（菜单表单选择用）
const allPages = ref<string[]>([]);

/**
 * 作用：拉取全部页面名称列表。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getAllPages() {
  const { data: pages } = await fetchGetAllPages();
  allPages.value = pages || [];
}

/**
 * 作用：初始化页面：加载全部路由页面选项。
 * @param 无
 * @returns {void} 无
 */
function init() {
  getAllPages();
}

// init
init();
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard
      :title="pageMenuTitle"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @refresh="getData"
        />
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="data"
        :row-selection="rowSelection"
        size="small"
        :loading="loading"
        :locale="tableListLocale"
        row-key="id"
        :scroll="scrollConfig"
        :pagination="pagination"
        class="h-full"
      />
      <MenuOperateModal
        v-model:visible="visible"
        :operate-type="operateType"
        :row-data="editingData"
        :all-pages="allPages"
        @submitted="getDataByPage"
      />
    </ACard>
  </div>
</template>

<style scoped></style>
