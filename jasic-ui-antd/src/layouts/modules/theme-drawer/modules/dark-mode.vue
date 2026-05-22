<script setup lang="ts">
/**
 * 主题抽屉 — 外观模式：亮/暗/跟随系统、侧栏反色、灰度与色弱辅助。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import type { SegmentedOption } from 'ant-design-vue/es/segmented/src/segmented';
import { themeSchemaRecord } from '@/constants/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import SettingItem from '../components/setting-item.vue';

defineOptions({
  name: 'DarkMode'
});

const themeStore = useThemeStore();

const icons: Record<UnionKey.ThemeScheme, string> = {
  light: 'material-symbols:sunny',
  dark: 'material-symbols:nightlight-rounded',
  auto: 'material-symbols:hdr-auto'
};

/**
 * 构建 Segmented 选项（含图标 payload）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getSegmentOptions() {
  const opts: SegmentedOption[] = Object.keys(themeSchemaRecord).map(item => {
    const key = item as UnionKey.ThemeScheme;
    return {
      value: item,
      payload: {
        icon: icons[key]
      }
    };
  });

  return opts;
}

// Segmented 控件选项（亮/暗/跟随系统）含图标 payload
const options = computed(() => getSegmentOptions());

/**
 * Segmented 变更：写入全局 themeScheme
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSegmentChange(value: string | number) {
  themeStore.setThemeScheme(value as UnionKey.ThemeScheme);
}

// 仅亮色全局 + 纵向类布局时展示「侧栏反色」开关
const showSiderInverted = computed(() => !themeStore.darkMode && themeStore.layout.mode.includes('vertical'));

type CheckedType = boolean | string | number;

/**
 * 灰度模式开关
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleGrayscaleChange(value: CheckedType) {
  themeStore.setGrayscale(value as boolean);
}

/**
 * 色弱模式开关
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleColourWeaknessChange(value: CheckedType) {
  themeStore.setColourWeakness(value as boolean);
}
</script>

<template>
  <ADivider>{{ $t('theme.themeSchema.title') }}</ADivider>
  <!-- 布局子模块：dark-mode -->
  <div class="flex-col-stretch gap-16px">
    <div class="i-flex-center">
      <ASegmented :value="themeStore.themeScheme" :options="options" class="bg-layout" @change="handleSegmentChange">
        <template #label="{ payload }">
          <div class="w-[70px] flex justify-center">
            <SvgIcon :icon="payload.icon" class="h-28px text-icon-small" />
          </div>
        </template>
      </ASegmented>
    </div>
    <Transition name="sider-inverted">
      <SettingItem v-if="showSiderInverted" :label="$t('theme.sider.inverted')">
        <ASwitch v-model:checked="themeStore.sider.inverted" />
      </SettingItem>
    </Transition>
    <SettingItem :label="$t('theme.grayscale')">
      <ASwitch :checked="themeStore.grayscale" @update:checked="handleGrayscaleChange" />
    </SettingItem>
    <SettingItem :label="$t('theme.colourWeakness')">
      <ASwitch :checked="themeStore.colourWeakness" @update:checked="handleColourWeaknessChange" />
    </SettingItem>
  </div>
</template>

<style scoped>
.sider-inverted-enter-active,
.sider-inverted-leave-active {
  --uno: h-22px transition-all-300;
}

.sider-inverted-enter-from,
.sider-inverted-leave-to {
  --uno: translate-x-20px opacity-0 h-0;
}
</style>
