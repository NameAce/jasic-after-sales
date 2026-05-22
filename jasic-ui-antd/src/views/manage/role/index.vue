<script setup lang="tsx">
/**
 * 管理端演示 — 角色管理：分页列表、搜索与角色维护抽屉（含菜单/按钮授权入口）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { Button, Popconfirm, Tag } from 'ant-design-vue';
import { tagColorEnabled } from '@/constants/list-status-tag';
import { enableStatusRecord } from '@/constants/business';
import { fetchGetRoleList } from '@/service/api';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { useTable, useTableOperate, useTableScroll } from '@/hooks/common/table';
import { $t } from '@/locales';
import RoleOperateDrawer from './modules/role-operate-drawer.vue';
import RoleSearch from './modules/role-search.vue';

// 表格滚动与 useTable（角色列与列表请求）
const { tableWrapperRef, scrollConfig } = useTableScroll();
const pageMenuTitle = useRouteMenuTitle();

const {
  columns,
  columnChecks,
  data,
  loading,
  getData,
  getDataByPage,
  mobilePagination,
  tableListLocale,
  searchParams,
  resetSearchParams
} = useTable({
  apiFn: fetchGetRoleList,
  apiParams: {
    current: 1,
    size: 10,
    status: undefined,
    roleName: undefined,
    roleCode: undefined
  },
  columns: () => [
    {
      key: 'index',
      dataIndex: 'index',
      title: $t('common.index'),
      width: 64,
      align: 'center'
    },
    {
      key: 'roleName',
      dataIndex: 'roleName',
      title: $t('page.manage.role.roleName'),
      align: 'center',
      minWidth: 120
    },
    {
      key: 'roleCode',
      dataIndex: 'roleCode',
      title: $t('page.manage.role.roleCode'),
      align: 'center',
      minWidth: 120
    },
    {
      key: 'roleDesc',
      dataIndex: 'roleDesc',
      title: $t('page.manage.role.roleDesc'),
      minWidth: 120
    },
    {
      key: 'status',
      dataIndex: 'status',
      title: $t('page.manage.role.roleStatus'),
      align: 'center',
      width: 100,
      customRender: ({ record }) => {
        if (record.status === null) {
          return null;
        }

        const label = $t(enableStatusRecord[record.status]);

        return <Tag color={tagColorEnabled(record.status === '1')}>{label}</Tag>;
      }
    },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 130,
      customRender: ({ record }) => (
        <div class="flex-center gap-8px">
          <Button type="link" size="small" class="table-action-link--primary" onClick={() => edit(record.id)}>
            {$t('common.edit')}
          </Button>
          <Popconfirm onConfirm={() => handleDelete(record.id)} title={$t('common.confirmDelete')}>
            <Button type="link" size="small" danger>
              {$t('common.delete')}
            </Button>
          </Popconfirm>
        </div>
      )
    }
  ]
});

const {
  drawerVisible,
  operateType,
  editingData,
  handleAdd,
  handleEdit,
  checkedRowKeys,
  rowSelection,
  onBatchDeleted,
  onDeleted
  // closeDrawer
} = useTableOperate(data, getData);

/**
 * 作用：批量删除角色（示例占位）。
 * @param 无
 * @returns 返回 Promise，回调结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleBatchDelete() {
  // request

  onBatchDeleted();
}

/**
 * 作用：删除单条角色（示例占位）。
 * @param id - 角色 id
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleDelete(_id: number) {
  // request

  onDeleted();
}

/**
 * 作用：按 id 打开角色编辑。
 * @param id - 角色 id
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function edit(id: number) {
  handleEdit(id);
}
</script>

<template>
  <!-- 管理端演示 — 角色列表与权限弹窗入口 -->
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <RoleSearch v-model:model="searchParams" @reset="resetSearchParams" @search="getDataByPage" />
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
        :loading="loading"
        :locale="tableListLocale"
        row-key="id"
        size="small"
        :pagination="mobilePagination"
        :scroll="scrollConfig"
        class="h-full"
      />
      <RoleOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="getDataByPage"
      />
    </ACard>
  </div>
</template>

<style scoped></style>
