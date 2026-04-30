<script setup lang="ts">
/**
 * 主题抽屉页脚：一键重置主题为默认、复制当前主题为 JSON（键名去引号便于粘贴到配置）。
 */
import { onMounted, ref } from 'vue';
import Clipboard from 'clipboard';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';

defineOptions({
  name: 'ConfigOperation'
});

const themeStore = useThemeStore();

const domRef = ref<HTMLElement | null>(null);

/** 绑定 Clipboard 到「复制配置」按钮容器 */
function initClipboard() {
  if (!domRef.value) return;

  const clipboard = new Clipboard(domRef.value, {
    text: () => getClipboardText()
  });

  clipboard.on('success', () => {
    window.$message?.success($t('theme.configOperation.copySuccessMsg'));
  });
}

/** 生成复制文本：将 JSON 中 `"key":` 形式的键名去掉引号 */
function getClipboardText() {
  const reg = /"\w+":/g;

  const json = themeStore.settingsJson;

  return json.replace(reg, match => match.replace(/"/g, ''));
}

/** 恢复默认主题配置并提示 */
function handleReset() {
  themeStore.resetStore();

  setTimeout(() => {
    window.$message?.success($t('theme.configOperation.resetSuccessMsg'));
  }, 50);
}

onMounted(() => {
  initClipboard();
});
</script>

<template>
  <div class="flex justify-between">
    <AButton danger @click="handleReset">{{ $t('theme.configOperation.resetConfig') }}</AButton>
    <div ref="domRef">
      <AButton type="primary">{{ $t('theme.configOperation.copyConfig') }}</AButton>
    </div>
  </div>
</template>

<style scoped></style>
