<script setup lang="ts">
/**
 * 主题抽屉 — 主题色：推荐色开关与各语义色 ColorPicker（info 可跟随主色）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { ColorPicker } from '@sa/materials';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import SettingItem from '../components/setting-item.vue';

defineOptions({
  name: 'ThemeColor'
});

const themeStore = useThemeStore();

/**
 * 更新某一语义色并同步派生样式
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleUpdateColor(color: string, key: App.Theme.ThemeColorKey) {
  themeStore.updateThemeColors(key, color);
}
</script>

<template>
  <!-- 布局子模块：theme-color -->
  <ADivider>{{ $t('theme.themeColor.title') }}</ADivider>
  <div class="flex-col-stretch gap-12px">
    <ATooltip placement="topLeft">
      <SettingItem key="recommend-color" :label="$t('theme.recommendColor')">
        <ASwitch v-model:checked="themeStore.recommendColor" />
      </SettingItem>
      <template #title>
        <p>
          <span class="pr-12px">{{ $t('theme.recommendColorDesc') }}</span>
          <br />
          <AButton
            type="link"
            href="https://uicolors.app/create"
            target="_blank"
            rel="noopener noreferrer"
            class="text-gray"
          >
            https://uicolors.app/create
          </AButton>
        </p>
      </template>
    </ATooltip>
    <SettingItem v-for="(_, key) in themeStore.themeColors" :key="key" :label="$t(`theme.themeColor.${key}`)">
      <template v-if="key === 'info'" #suffix>
        <ACheckbox v-model:checked="themeStore.isInfoFollowPrimary">
          {{ $t('theme.themeColor.followPrimary') }}
        </ACheckbox>
      </template>
      <ColorPicker
        :color="themeStore.themeColors[key]"
        :disabled="key === 'info' && themeStore.isInfoFollowPrimary"
        @update:color="handleUpdateColor($event, key)"
      />
    </SettingItem>
  </div>
</template>

<style scoped></style>
