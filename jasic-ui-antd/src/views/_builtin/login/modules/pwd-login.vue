<script setup lang="ts">
/**
 * 内置页：pwd-login。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, reactive } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'PwdLogin'
});

const authStore = useAuthStore();
const { formRef, validate } = useAntdForm();

interface FormModel {
  userName: string;
  password: string;
}

const model: FormModel = reactive({
  userName: '',
  password: ''
});

// 会话 Store、登录切换与表单校验；规则与后端 `LoginDTO`（`/auth/login`）一致 —— 用户名或手机号、`@NotBlank` 密码，不套用注册用户名的 REG_USER_NAME/REG_PWD
const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules } = useFormRules();

  return {
    userName: formRules.loginUsername,
    password: formRules.loginPassword
  };
});

/**
 * 作用：校验并调用账号密码登录。
 * @param 无
 * @returns 返回 Promise，登录流程结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleSubmit() {
  await validate();
  await authStore.login(model.userName, model.password);
}
</script>

<template>
  <AForm ref="formRef" :model="model" :rules="rules" @keyup.enter="handleSubmit">
    <!-- 内置页：login/modules/pwd-login.vue -->
    <AFormItem name="userName" required>
      <AInput v-model:value="model.userName" size="large" :placeholder="$t('page.login.common.userNamePlaceholder')" />
    </AFormItem>
    <AFormItem name="password" required>
      <AInputPassword
        v-model:value="model.password"
        size="large"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </AFormItem>
    <ASpace direction="vertical" size="large" class="w-full">
      <!-- 产品要求不展示「忘记密码」入口，重置密码模块仍保留供后续按需开放 -->
      <div class="flex-y-center">
        <ACheckbox>{{ $t('page.login.pwdLogin.rememberMe') }}</ACheckbox>
      </div>
      <AButton type="primary" block size="large" shape="round" :loading="authStore.loginLoading" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </AButton>
    </ASpace>
  </AForm>
</template>

<style scoped></style>
