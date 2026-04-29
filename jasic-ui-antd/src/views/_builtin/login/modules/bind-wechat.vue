<script setup lang="ts">
import { reactive } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'BindWechat'
});

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

async function handleSubmit() {
  await validate();
  await authStore.confirmWechatBindAndLogin(model);
}
</script>

<template>
  <AForm ref="formRef" :model="model" :rules="rules" @keyup.enter="handleSubmit">
    <AFormItem name="bindTicket">
      <AInput v-model:value="model.bindTicket" size="large" placeholder="请输入绑定票据 bindTicket" />
    </AFormItem>
    <AFormItem name="code">
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
