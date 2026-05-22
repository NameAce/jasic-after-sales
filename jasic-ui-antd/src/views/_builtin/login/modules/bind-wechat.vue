<script setup lang="ts">
/**
 * 内置页：bind-wechat。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { reactive } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'BindWechat'
});

// 路由跳转、绑定票据表单与认证会话
const { toggleLoginModule } = useRouterPush();
const authStore = useAuthStore();
const { formRef, validate } = useAntdForm();

interface FormModel {
  bindTicket: string;
  code: string;
}

const model: FormModel = reactive({
  bindTicket: '',
  code: ''
});

const { defaultRequiredRule } = useFormRules();

const rules: Record<keyof FormModel, App.Global.FormRule[]> = {
  bindTicket: [defaultRequiredRule],
  code: [defaultRequiredRule]
};

/**
 * 作用：校验后确认微信绑定并完成登录。
 * @param 无
 * @returns 返回 Promise，登录结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleSubmit() {
  await validate();
  await authStore.confirmWechatBindAndLogin(model);
}
</script>

<template>
  <!-- 内置页：login/modules/bind-wechat.vue -->
  <AForm ref="formRef" :model="model" :rules="rules" @keyup.enter="handleSubmit">
    <AFormItem name="bindTicket" required>
      <AInput v-model:value="model.bindTicket" size="large" placeholder="请输入绑定票据 bindTicket" />
    </AFormItem>
    <AFormItem name="code" required>
      <AInput v-model:value="model.code" size="large" placeholder="请输入微信登录 code" />
    </AFormItem>
    <ASpace direction="vertical" size="large" class="w-full">
      <AButton type="primary" block size="large" shape="round" :loading="authStore.loginLoading" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </AButton>
      <AButton block size="large" shape="round" @click="toggleLoginModule('pwd-login')">
        {{ $t('page.login.common.back') }}
      </AButton>
    </ASpace>
  </AForm>
</template>

<style scoped></style>
