<script setup lang="ts">
import { computed, reactive } from 'vue';
import { useRouterPush } from '@/hooks/common/router';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { useCaptcha } from '@/hooks/business/captcha';
import { $t } from '@/locales';

defineOptions({
  name: 'Register'
});

// 注册占位页：路由、验证码与表单
const { toggleLoginModule } = useRouterPush();
const { formRef, validate } = useAntdForm();
const { label, isCounting, loading, getCaptcha } = useCaptcha();

interface FormModel {
  phone: string;
  code: string;
  password: string;
  confirmPassword: string;
}

const model: FormModel = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
});

// 注册表单校验（随语言切换）
const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules, createConfirmPwdRule } = useFormRules();

  return {
    phone: formRules.phone,
    code: formRules.code,
    password: formRules.pwd,
    confirmPassword: createConfirmPwdRule(model.password)
  };
});

/**
 * 作用：校验后提示 PC 注册流程未开放。
 * @param 无
 * @returns 返回 Promise，校验完成后结束
 */
async function handleSubmit() {
  await validate();
  // PC 主链路不走 mp 绑定，注册入口暂作为占位提示。
  window.$message?.warning('当前版本暂未开放该 PC 流程，请使用密码登录。');
}

/**
 * 作用：校验手机号后请求短信验证码。
 * @param 无
 * @returns 返回 Promise，验证码请求结束后结束
 */
async function handleGetCaptcha() {
  if (!model.phone) {
    window.$message?.warning($t('page.login.common.phonePlaceholder'));
    return;
  }

  await getCaptcha(model.phone);
}
</script>

<template>
  <AForm ref="formRef" :model="model" :rules="rules" @keyup.enter="handleSubmit">
    <AFormItem name="phone">
      <AInput v-model:value="model.phone" size="large" :placeholder="$t('page.login.common.phonePlaceholder')" />
    </AFormItem>
    <AFormItem name="code">
      <div class="w-full flex-y-center gap-16px">
        <AInput v-model:value="model.code" size="large" :placeholder="$t('page.login.common.codePlaceholder')" />
        <AButton size="large" :disabled="isCounting" :loading="loading" @click="handleGetCaptcha">
          {{ label }}
        </AButton>
      </div>
    </AFormItem>
    <AFormItem name="password">
      <AInputPassword
        v-model:value="model.password"
        size="large"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </AFormItem>
    <AFormItem name="confirmPassword">
      <AInputPassword
        v-model:value="model.confirmPassword"
        size="large"
        :placeholder="$t('page.login.common.confirmPasswordPlaceholder')"
      />
    </AFormItem>
    <ASpace direction="vertical" size="large" class="w-full">
      <AButton type="primary" block size="large" shape="round" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </AButton>
      <AButton block size="large" shape="round" @click="toggleLoginModule('pwd-login')">
        {{ $t('page.login.common.back') }}
      </AButton>
    </ASpace>
  </AForm>
</template>

<style scoped></style>
