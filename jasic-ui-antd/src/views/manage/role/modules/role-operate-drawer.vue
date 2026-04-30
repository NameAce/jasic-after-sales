<script setup lang="ts">
/**
 * 角色列表 — 新增/编辑抽屉：基本信息与跳转菜单/按钮授权弹窗。
 */
import { computed, ref, watch } from 'vue';
import { useBoolean } from '@sa/hooks';
import { enableStatusOptions } from '@/constants/business';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';
import MenuAuthModal from './menu-auth-modal.vue';
import ButtonAuthModal from './button-auth-modal.vue';

defineOptions({
  name: 'RoleOperateDrawer'
});

interface Props {
  /** 表格操作类型 */
  operateType: AntDesign.TableOperateType;
  /** 编辑行数据 */
  rowData?: Api.SystemManage.Role | null;
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
const { bool: menuAuthVisible, setTrue: openMenuAuthModal } = useBoolean();
const { bool: buttonAuthVisible, setTrue: openButtonAuthModal } = useBoolean();

// 抽屉标题文案
const title = computed(() => {
  const titles: Record<AntDesign.TableOperateType, string> = {
    add: $t('page.manage.role.addRole'),
    edit: $t('page.manage.role.editRole')
  };
  return titles[props.operateType];
});

type Model = Pick<Api.SystemManage.Role, 'roleName' | 'roleCode' | 'roleDesc' | 'status'>;

// 角色表单模型
const model = ref(createDefaultModel());

/**
 * 作用：返回角色表单默认值。
 * @param 无
 * @returns 默认模型
 */
function createDefaultModel(): Model {
  return {
    roleName: '',
    roleCode: '',
    roleDesc: '',
    status: '1'
  };
}

type RuleKey = Exclude<keyof Model, 'roleDesc'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  roleName: defaultRequiredRule,
  roleCode: defaultRequiredRule,
  status: defaultRequiredRule
};

// 编辑模式下当前角色 id（菜单/按钮授权弹窗用）
const roleId = computed(() => props.rowData?.id || -1);

// 是否为编辑态
const isEdit = computed(() => props.operateType === 'edit');

/**
 * 作用：初始化表单模型。
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
 * 作用：校验并模拟提交（示例未接保存接口）。
 * @param 无
 * @returns 返回 Promise，校验完成后结束
 */
async function handleSubmit() {
  await validate();
  // request
  window.$message?.success($t('common.updateSuccess'));
  closeDrawer();
  emit('submitted');
}

// 打开抽屉时初始化表单并清校验
watch(visible, () => {
  if (visible.value) {
    handleInitModel();
    resetFields();
  }
});
</script>

<template>
  <ADrawer v-model:open="visible" :title="title" :width="360">
    <AForm ref="formRef" layout="vertical" :model="model" :rules="rules">
      <AFormItem :label="$t('page.manage.role.roleName')" name="roleName">
        <AInput v-model:value="model.roleName" :placeholder="$t('page.manage.role.form.roleName')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.role.roleCode')" name="roleCode">
        <AInput v-model:value="model.roleCode" :placeholder="$t('page.manage.role.form.roleCode')" />
      </AFormItem>
      <AFormItem :label="$t('page.manage.role.roleStatus')" name="status">
        <ARadioGroup v-model:value="model.status">
          <ARadio v-for="item in enableStatusOptions" :key="item.value" :value="item.value">
            {{ $t(item.label) }}
          </ARadio>
        </ARadioGroup>
      </AFormItem>
      <AFormItem :label="$t('page.manage.role.roleDesc')" name="roleDesc">
        <AInput v-model:value="model.roleDesc" :placeholder="$t('page.manage.role.form.roleDesc')" />
      </AFormItem>
    </AForm>
    <ASpace v-if="isEdit">
      <AButton @click="openMenuAuthModal">{{ $t('page.manage.role.menuAuth') }}</AButton>
      <MenuAuthModal v-model:visible="menuAuthVisible" :role-id="roleId" />
      <AButton @click="openButtonAuthModal">{{ $t('page.manage.role.buttonAuth') }}</AButton>
      <ButtonAuthModal v-model:visible="buttonAuthVisible" :role-id="roleId" />
    </ASpace>
    <template #footer>
      <div class="flex-y-center justify-end gap-12px">
        <AButton @click="closeDrawer">{{ $t('common.cancel') }}</AButton>
        <AButton type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</AButton>
      </div>
    </template>
  </ADrawer>
</template>

<style scoped></style>
