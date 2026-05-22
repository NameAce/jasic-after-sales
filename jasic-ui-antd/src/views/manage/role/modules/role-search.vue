<script setup lang="ts">
/**
 * 角色列表 — 搜索表单：角色名、状态等，emit reset/search。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { enableStatusOptions } from '@/constants/business';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';
import { $t } from '@/locales';

defineOptions({
  name: 'RoleSearch'
});

interface Emits {
  (e: 'reset'): void;
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const manageRoleSearchFilter = usePageSearchFilterCollapse(3);

// 查询表单双向绑定模型
const model = defineModel<Api.SystemManage.RoleSearchParams>('model', { required: true });

/**
 * 作用：触发父级重置查询条件。
 * @param 无
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function reset() {
  emit('reset');
}

/**
 * 作用：触发父级执行搜索。
 * @param 无
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function search() {
  emit('search');
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper">
    <!-- 角色列表搜索表单 -->
    <AForm :model="model" :label-width="80">
      <div class="page-search-toolbar">
        <div class="page-search-toolbar__filters">
          <ARow :gutter="[16, 16]" wrap>
            <ACol
              :span="24"
              :md="12"
              :lg="6"
              :class="{ 'page-search-toolbar__filter-col--collapsed': manageRoleSearchFilter.isSearchFilterHidden(0) }"
            >
              <AFormItem :label="$t('page.manage.role.roleName')" name="roleName" class="m-0">
                <AInput v-model:value="model.roleName" :placeholder="$t('page.manage.role.form.roleName')" />
              </AFormItem>
            </ACol>
            <ACol
              :span="24"
              :md="12"
              :lg="6"
              :class="{ 'page-search-toolbar__filter-col--collapsed': manageRoleSearchFilter.isSearchFilterHidden(1) }"
            >
              <AFormItem :label="$t('page.manage.role.roleCode')" name="roleCode" class="m-0">
                <AInput v-model:value="model.roleCode" :placeholder="$t('page.manage.role.form.roleCode')" />
              </AFormItem>
            </ACol>
            <ACol
              :span="24"
              :md="12"
              :lg="6"
              :class="{ 'page-search-toolbar__filter-col--collapsed': manageRoleSearchFilter.isSearchFilterHidden(2) }"
            >
              <AFormItem :label="$t('page.manage.role.roleStatus')" name="status" class="m-0">
                <ASelect v-model:value="model.status" :placeholder="$t('page.manage.role.form.roleStatus')" allow-clear>
                  <ASelectOption v-for="option in enableStatusOptions" :key="option.value" :value="option.value">
                    {{ $t(option.label) }}
                  </ASelectOption>
                </ASelect>
              </AFormItem>
            </ACol>
          </ARow>
        </div>
        <div class="page-search-toolbar__actions">
          <AButton type="primary" ghost @click="search">
            <template #icon>
              <icon-ic-round-search class="align-sub text-icon" />
            </template>
            <span class="ml-8px">{{ $t('common.search') }}</span>
          </AButton>
          <AButton class="ml-8px" @click="reset">
            <template #icon>
              <icon-ic-round-refresh class="align-sub text-icon" />
            </template>
            <span class="ml-8px">{{ $t('common.reset') }}</span>
          </AButton>
          <PageSearchExpandButton
            v-if="manageRoleSearchFilter.showSearchFilterExpandToggle"
            :expanded="manageRoleSearchFilter.searchFilterExpanded"
            @click="manageRoleSearchFilter.toggleSearchFilterExpand"
          />
        </div>
      </div>
    </AForm>
  </ACard>
</template>

<style scoped></style>
