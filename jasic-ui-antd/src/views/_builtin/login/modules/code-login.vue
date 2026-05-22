<script setup lang="ts">
/**
 * 内置页：code-login。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, reactive } from 'vue';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { useCaptcha } from '@/hooks/business/captcha';
import { $t } from '@/locales';

defineOptions({
  name: 'CodeLogin'
});

// 路由切换、验证码与登录会话
const { toggleLoginModule } = useRouterPush();
const { formRef, validate } = useAntdForm();
const { label, isCounting, loading, getCaptcha } = useCaptcha();
const authStore = useAuthStore();

interface FormModel {
  phone: string;
  code: string;
}

const model: FormModel = reactive({
  phone: '',
  code: ''
});

// 手机号验证码登录校验
const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules } = useFormRules();

  return {
    phone: formRules.phone,
    code: formRules.code
  };
});

/**
 * 作用：校验后以微信 code 登录。
 * @param 无
 * @returns 返回 Promise，登录结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleSubmit() {
  await validate();
  await authStore.loginByWechatCode(model.code);
}

/**
 * 作用：校验手机号非空后获取短信验证码。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
    <!-- 内置页：login/modules/code-login.vue -->
    <AFormItem name="phone" required>
      <AInput v-model:value="model.phone" size="large" :placeholder="$t('page.login.common.phonePlaceholder')" />
    </AFormItem>
    <AFormItem name="code" required>
      <div class="w-full flex-y-center gap-16px">
        <AInput v-model:value="model.code" size="large" :placeholder="$t('page.login.common.codePlaceholder')" />
        <AButton size="large" :disabled="isCounting" :loading="loading" @click="handleGetCaptcha">
          {{ label }}
        </AButton>
      </div>
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
