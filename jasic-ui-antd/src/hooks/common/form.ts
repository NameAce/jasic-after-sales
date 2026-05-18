/**
 * 表单工具：`useFormRules` 常用校验规则与 `useAntdForm` 对 FormInstance 的封装。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { ref, toValue } from 'vue';
import type { ComputedRef, Ref } from 'vue';
import type { FormInstance } from 'ant-design-vue';
import { REG_CODE_SIX, REG_EMAIL, REG_PHONE, REG_PWD, REG_USER_NAME } from '@/constants/reg';
import { $t } from '@/locales';

/**
 * 作用：提供常用表单校验规则与 Ant Design Form 实例封装。
 * @returns 规则工厂与 `formRef` 操作方法
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useFormRules() {
  const patternRules = {
    userName: {
      pattern: REG_USER_NAME,
      message: $t('form.userName.invalid'),
      trigger: 'change'
    },
    phone: {
      pattern: REG_PHONE,
      message: $t('form.phone.invalid'),
      trigger: 'change'
    },
    pwd: {
      pattern: REG_PWD,
      message: $t('form.pwd.invalid'),
      trigger: 'change'
    },
    code: {
      pattern: REG_CODE_SIX,
      message: $t('form.code.invalid'),
      trigger: 'change'
    },
    email: {
      pattern: REG_EMAIL,
      message: $t('form.email.invalid'),
      trigger: 'change'
    }
  } satisfies Record<string, App.Global.FormRule>;

  const formRules = {
    userName: [createRequiredRule($t('form.userName.required')), patternRules.userName],
    phone: [createRequiredRule($t('form.phone.required')), patternRules.phone],
    pwd: [createRequiredRule($t('form.pwd.required')), patternRules.pwd],
    code: [createRequiredRule($t('form.code.required')), patternRules.code],
    email: [createRequiredRule($t('form.email.required')), patternRules.email]
  } satisfies Record<string, App.Global.FormRule[]>;

  /** 通用必填规则（仅 message）
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  const defaultRequiredRule = createRequiredRule($t('form.required'));

  /**
   * 作用：创建必填校验规则。
   * @param message 校验失败提示
   * @returns {App.Global.FormRule}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function createRequiredRule(message: string) {
    return {
      required: true,
      message
    };
  }

  /**
   * 作用：创建与主密码字段一致的确认密码校验规则。
   * @param pwd 主密码（ref/computed/字符串）
   * @returns {App.Global.FormRule[]}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function createConfirmPwdRule(pwd: string | Ref<string> | ComputedRef<string>) {
    const confirmPwdRule: App.Global.FormRule[] = [
      { required: true, message: $t('form.confirmPwd.required') },
      {
        validator: (rule, value) => {
          if (value.trim() !== '' && value !== toValue(pwd)) {
            return Promise.reject(rule.message);
          }
          return Promise.resolve();
        },
        message: $t('form.confirmPwd.invalid'),
        trigger: 'change'
      }
    ];
    return confirmPwdRule;
  }

  return {
    patternRules,
    formRules,
    defaultRequiredRule,
    createRequiredRule,
    createConfirmPwdRule
  };
}

/**
 * 作用：`ref` 持有 Ant Design Form 实例，提供 `validate` / `resetFields`。
 * @returns {{ formRef; validate; resetFields }}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useAntdForm() {
  const formRef = ref<FormInstance | null>(null);

  async function validate() {
    await formRef.value?.validate();
  }

  function resetFields() {
    formRef.value?.resetFields();
  }

  return {
    formRef,
    validate,
    resetFields
  };
}
