<script setup lang="ts">
/**
 * 用户列表 — 搜索表单：状态、性别、关键词等，emit reset/search。
 */
import { computed } from 'vue';
import { enableStatusOptions, userGenderOptions } from '@/constants/business';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { translateOptions } from '@/utils/common';
import { $t } from '@/locales';

defineOptions({
  name: 'UserSearch'
});

interface Emits {
  (e: 'reset'): void;
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

// 查询表单重置、搜索事件向父组件传递
const { formRef, validate, resetFields } = useAntdForm();

// 双向绑定的查询条件模型
const model = defineModel<Api.SystemManage.UserSearchParams>('model', { required: true });

type RuleKey = Extract<keyof Api.SystemManage.UserSearchParams, 'userEmail' | 'userPhone'>;

// 邮箱/手机校验规则（内层使用 useFormRules 以保持语言切换响应）
const rules = computed<Record<RuleKey, App.Global.FormRule>>(() => {
  const { patternRules } = useFormRules(); // inside computed to make locale reactive

  return {
    userEmail: patternRules.email,
    userPhone: patternRules.phone
  };
});

/**
 * 作用：重置表单并通知父级执行查询重置。
 * @param 无
 * @returns 返回 Promise，重置完成后结束
 */
async function reset() {
  await resetFields();
  emit('reset');
}

/**
 * 作用：校验通过后通知父级触发搜索。
 * @param 无
 * @returns 返回 Promise，校验通过后结束
 */
async function search() {
  await validate();
  emit('search');
}
</script>

<template>
  <ACard :bordered="false" class="card-wrapper">
    <AForm
      ref="formRef"
      :model="model"
      :rules="rules"
      :label-col="{
        span: 5,
        md: 7
      }"
    >
      <div class="page-search-toolbar">
        <div class="page-search-toolbar__filters">
          <ARow :gutter="[16, 16]" wrap>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.userName')" name="userName" class="m-0">
                <AInput v-model:value="model.userName" :placeholder="$t('page.manage.user.form.userName')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.userGender')" name="userGender" class="m-0">
                <ASelect
                  v-model:value="model.userGender"
                  :placeholder="$t('page.manage.user.form.userGender')"
                  :options="translateOptions(userGenderOptions)"
                  clearable
                />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.nickName')" name="nickName" class="m-0">
                <AInput v-model:value="model.nickName" :placeholder="$t('page.manage.user.form.nickName')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.userPhone')" name="userPhone" class="m-0">
                <AInput v-model:value="model.userPhone" :placeholder="$t('page.manage.user.form.userPhone')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.userEmail')" name="userEmail" class="m-0">
                <AInput v-model:value="model.userEmail" :placeholder="$t('page.manage.user.form.userEmail')" />
              </AFormItem>
            </ACol>
            <ACol :span="24" :md="12" :lg="6">
              <AFormItem :label="$t('page.manage.user.userStatus')" name="userStatus" class="m-0">
                <ASelect
                  v-model:value="model.status"
                  :placeholder="$t('page.manage.user.form.userStatus')"
                  :options="translateOptions(enableStatusOptions)"
                  clearable
                />
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
