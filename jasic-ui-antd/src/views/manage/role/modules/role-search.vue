<script setup lang="ts">
import { enableStatusOptions } from '@/constants/business';
import { $t } from '@/locales';

defineOptions({
  name: 'RoleSearch'
});

interface Emits {
  (e: 'reset'): void;
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const model = defineModel<Api.SystemManage.RoleSearchParams>('model', { required: true });

function reset() {
  emit('reset');
}

function search() {
  emit('search');
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper">
    <AForm :model="model" :label-width="80">
      <div class="page-search-toolbar">
        <div class="page-search-toolbar__filters">
          <ARow :gutter="[16, 16]" wrap>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.role.roleName')" name="roleName" class="m-0">
                <AInput v-model:value="model.roleName" :placeholder="$t('page.manage.role.form.roleName')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.role.roleCode')" name="roleCode" class="m-0">
                <AInput v-model:value="model.roleCode" :placeholder="$t('page.manage.role.form.roleCode')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
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
        </div>
      </div>
    </AForm>
  </ACard>
</template>

<style scoped></style>
