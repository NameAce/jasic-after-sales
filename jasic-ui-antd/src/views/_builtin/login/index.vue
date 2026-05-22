<script setup lang="ts">
/**
 * 登录入口页：波浪背景 + 按 `module` 切换密码/验证码/注册/重置密码/绑定微信等子表单。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { Component } from 'vue';
import { getColorPalette, mixColor } from '@sa/utils';
import { loginModuleRecord } from '@/constants/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import PwdLogin from './modules/pwd-login.vue';
import CodeLogin from './modules/code-login.vue';
import Register from './modules/register.vue';
import ResetPwd from './modules/reset-pwd.vue';
import BindWechat from './modules/bind-wechat.vue';

interface Props {
  /**
   * The login module
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  module?: UnionKey.LoginModule;
}

const props = defineProps<Props>();

// 主题配置（背景渐变等）
const themeStore = useThemeStore();

interface LoginModule {
  label: App.I18n.I18nKey;
  component: Component;
}

// 登录子模块与组件映射
const moduleMap: Record<UnionKey.LoginModule, LoginModule> = {
  'pwd-login': { label: loginModuleRecord['pwd-login'], component: PwdLogin },
  'code-login': { label: loginModuleRecord['code-login'], component: CodeLogin },
  register: { label: loginModuleRecord.register, component: Register },
  'reset-pwd': { label: loginModuleRecord['reset-pwd'], component: ResetPwd },
  'bind-wechat': { label: loginModuleRecord['bind-wechat'], component: BindWechat }
};

// PC 端允许的登录模块白名单（其余回落到密码登录）
const PC_ENTRY_WHITELIST: ReadonlySet<UnionKey.LoginModule> = new Set(['pwd-login', 'reset-pwd']);

// 当前展示的登录子模块（含白名单纠正）
const activeModule = computed(() => {
  const module = props.module || 'pwd-login';
  const resolvedModule = PC_ENTRY_WHITELIST.has(module) ? module : 'pwd-login';
  return moduleMap[resolvedModule];
});

// 波浪背景主题色（暗色用调色板加深）
const bgThemeColor = computed(() =>
  themeStore.darkMode ? getColorPalette(themeStore.themeColor, 7) : themeStore.themeColor
);

// 登录页整页背景混合色
const bgColor = computed(() => {
  const COLOR_WHITE = '#ffffff';

  const ratio = themeStore.darkMode ? 0.5 : 0.2;

  return mixColor(COLOR_WHITE, themeStore.themeColor, ratio);
});

// 系统标题文案（i18n）
const systemTitle = computed(() => $t('system.title'));
</script>

<template>
  <!-- 内置页：login/index.vue -->
  <div class="relative size-full flex-center" :style="{ backgroundColor: bgColor }">
    <WaveBg :theme-color="bgThemeColor" />
    <ACard class="relative z-4">
      <div class="w-400px lt-sm:w-300px">
        <header class="flex-y-center justify-between">
          <SystemLogo class="size-64px lt-sm:size-48px" />
          <h3 class="text-28px text-primary font-500 lt-sm:text-22px">{{ systemTitle }}</h3>
          <div class="i-flex-col">
            <ThemeSchemaSwitch
              :theme-schema="themeStore.themeScheme"
              :show-tooltip="false"
              class="text-20px lt-sm:text-18px"
              @switch="themeStore.toggleThemeScheme"
            />
          </div>
        </header>
        <main class="pt-24px">
          <h3 class="text-18px text-primary font-medium">
            {{ $t(activeModule.label) }}
          </h3>
          <div class="animation-slide-in-left pt-24px">
            <Transition :name="themeStore.page.animateMode" mode="out-in" appear>
              <component :is="activeModule.component" />
            </Transition>
          </div>
        </main>
      </div>
    </ACard>
  </div>
</template>

<style scoped></style>
