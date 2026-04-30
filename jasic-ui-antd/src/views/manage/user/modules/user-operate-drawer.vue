<script setup lang="ts">
/**
 * 用户列表 — 新增/编辑抽屉：表单校验、角色选择与提交。
 */
import { computed, ref, watch } from 'vue';
import { enableStatusOptions, userGenderOptions } from '@/constants/business';
import { fetchGetAllRoles } from '@/service/api';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'UserOperateDrawer'
});

interface Props {
  /** 表格操作类型：新增或编辑 */
  operateType: AntDesign.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.SystemManage.User | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, resetFields } = useAntdForm();
const { defaultRequiredRule } = useFormRules();

// 抽屉标题（随操作类型切换）
const title = computed(() => {
  const titles: Record<AntDesign.TableOperateType, string> = {
    add: $t('page.manage.user.addUser'),
    edit: $t('page.manage.user.editUser')
  };
  return titles[props.operateType];
});

type Model = Pick<
  Api.SystemManage.User,
  'userName' | 'userGender' | 'nickName' | 'userPhone' | 'userEmail' | 'userRoles' | 'status'
>;

// 抽屉内表单模型
const model = ref(createDefaultModel());

/**
 * 作用：创建用户新增/编辑表单项的默认值。
 * @param 无
 * @returns 默认模型
 */
function createDefaultModel(): Model {
  return {
    userName: '',
    userGender: '1',
    nickName: '',
    userPhone: '',
    userEmail: '',
    userRoles: [],
    status: '1'
  };
}

type RuleKey = Extract<keyof Model, 'userName' | 'status'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  userName: defaultRequiredRule,
  status: defaultRequiredRule
};

/** 角色多选下拉的数据源 */
const roleOptions = ref<CommonType.Option<string>[]>([]);

/**
 * 作用：请求全部角色并合并当前用户已有角色为选项。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getRoleOptions() {
  const { error, data } = await fetchGetAllRoles();

  if (!error) {
    const options = data.map(item => ({
      label: item.roleName,
      value: item.roleCode
    }));

    // the mock data does not have the roleCode, so fill it
    // if the real request, remove the following code
    const userRoleOptions = model.value.userRoles.map(item => ({
      label: item,
      value: item
    }));
    // end

    roleOptions.value = [...userRoleOptions, ...options];
  }
}

/**
 * 作用：按操作类型初始化表单（编辑时合并行数据）。
 * @param 无
 * @returns {void} 无
 */
function handleInitModel() {
  model.value = createDefaultModel();

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, props.rowData);
  }
}

/**
 * 作用：关闭抽屉。
 * @param 无
 * @returns {void} 无
 */
function closeDrawer() {
  visible.value = false;
}

/**
 * 作用：校验表单并模拟提交成功（示例页未接真实接口）。
 * @param 无
 * @returns 返回 Promise，校验与提示完成后结束
 */
async function handleSubmit() {
  await validate();
  // request
  window.$message?.success($t('common.updateSuccess'));
  closeDrawer();
  emit('submitted');
}

// 打开抽屉时重置表单并拉取角色选项
watch(visible, () => {
  if (visible.value) {
    handleInitModel();
    resetFields();
    getRoleOptions();
  }
});
</script>

<template>
  <ADrawer v-model:open="visible" :title="title" :width="360">
    <AForm ref="formRef" layout="vertical" :model="model" :rules="rules">
      <AFormItem :label="$t('page.manage.user.userName')" name="userName">
        <AInput v-model:value="model.userName" :placeholder="$t('page.manage.user.form.userName')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.userGender')" name="userGender">
        <ARadioGroup v-model:value="model.userGender">
          <ARadio v-for="item in userGenderOptions" :key="item.value" :value="item.value">
            {{ $t(item.label) }}
          </ARadio>
        </ARadioGroup>
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.nickName')" name="nickName">
        <AInput v-model:value="model.nickName" :placeholder="$t('page.manage.user.form.nickName')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.userPhone')" name="userPhone">
        <AInput v-model:value="model.userPhone" :placeholder="$t('page.manage.user.form.userPhone')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.userEmail')" name="email">
        <AInput v-model:value="model.userEmail" :placeholder="$t('page.manage.user.form.userEmail')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.userStatus')" name="status">
        <ARadioGroup v-model:value="model.status">
          <ARadio v-for="item in enableStatusOptions" :key="item.value" :value="item.value">
            {{ $t(item.label) }}
          </ARadio>
        </ARadioGroup>
      </AFormItem>
      <AFormItem :label="$t('page.manage.user.userRole')" name="roles">
        <ASelect
          v-model:value="model.userRoles"
          multiple
          :options="roleOptions"
          :placeholder="$t('page.manage.user.form.userRole')"
        />
      </AFormItem>
    </AForm>
    <template #footer>
      <ASpace :size="16">
        <AButton @click="closeDrawer">{{ $t('common.cancel') }}</AButton>
        <AButton type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</AButton>
      </ASpace>
    </template>
  </ADrawer>
</template>

<style scoped></style>
