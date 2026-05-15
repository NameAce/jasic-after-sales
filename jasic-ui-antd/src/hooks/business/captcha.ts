/**
 * 登录验证码：按钮加载态、倒计时与模拟发送流程（可替换为真实短信/邮件接口）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { computed } from 'vue';
import { useCountDown, useLoading } from '@sa/hooks';
import { REG_PHONE } from '@/constants/reg';
import { $t } from '@/locales';

/**
 * 作用：登录验证码按钮：加载态、倒计时文案和模拟发短信流程（可对接真实接口）。
 * @returns 按钮文案 computed、倒计时控制与 `getCaptcha`
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useCaptcha() {
  const { loading, startLoading, endLoading } = useLoading();
  const { count, start, stop, isCounting } = useCountDown(10);

  // 汇总 loading / 倒计时 / 默认文案，生成按钮展示文字
  const label = computed(() => {
    let text = $t('page.login.codeLogin.getCode');
    // 倒计时文案
    const countingLabel = $t('page.login.codeLogin.reGetCode', { time: count.value });

    if (loading.value) {
      // 加载态时，显示空字符串
      text = '';
    }

    if (isCounting.value) {
      // 倒计时中时，显示倒计时文案
      text = countingLabel;
    }

    return text;
  });
  /**
   * 作用：验证手机号是否有效
   * @param phone 手机号
   * @returns 是否有效
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  function isPhoneValid(phone: string) {
    if (phone.trim() === '') {
      window.$message?.error?.($t('form.phone.required'));

      return false;
    }

    if (!REG_PHONE.test(phone)) {
      window.$message?.error?.($t('form.phone.invalid'));

      return false;
    }

    return true;
  }

  /**
   * 作用：获取验证码
   * @param phone 手机号
   * @returns 是否成功
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  async function getCaptcha(phone: string) {
    // 验证手机号是否有效
    const valid = isPhoneValid(phone);

    // 验证手机号是否有效或加载态时，返回
    if (!valid || loading.value) {
      return;
    }

    // 开始加载
    startLoading();

    // 模拟请求
    await new Promise(resolve => {
      setTimeout(resolve, 500);
    });

    // 显示成功提示
    window.$message?.success?.($t('page.login.codeLogin.sendCodeSuccess'));

    // 开始倒计时
    start();

    // 结束加载
    endLoading();
  }

  return {
    label,
    start,
    stop,
    isCounting,
    loading,
    getCaptcha
  };
}
