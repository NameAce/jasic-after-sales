<script setup lang="ts">
/**
 * 参数配置 — 表单模式面板：按后端 groupKey 分组展示 Tab，每组内编辑该分组下的参数项。
 * 数据来源：GET /system/config/grouped（一次性返回各分组及配置项，默认不含 legacy）；
 * 保存动作：PUT /system/config/grouped（同组批量保存，事务内落库并在提交后刷新缓存）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance } from 'ant-design-vue';
import { listSystemConfigGrouped, refreshConfigCache, saveSystemConfigGroup } from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuth } from '@/hooks/business/auth';

type RowData = Record<string, any>;

/** 分组标识到 Tab 展示名称的兜底映射（接口未返回 groupName 时使用）。 */
const CONFIG_GROUP_LABEL_MAP: Record<string, string> = {
  org: '组织配置',
  wechat: '微信配置',
  work_order: '工单配置',
  legacy: '历史配置'
};

const emit = defineEmits<{
  /** 保存或刷新缓存成功后通知父级。 */
  saved: [];
}>();

const { hasAuth } = useAuth();

/** 是否允许保存参数（与表格行内编辑权限一致）。 */
const canUpdateConfig = computed(() => hasAuth(['system:config:update']));
/** 是否允许刷新参数缓存。 */
const canRefreshCache = computed(() => hasAuth(['system:config:refresh']));

const loading = ref(false);
const submitting = ref(false);
const activeGroupKey = ref<string>('');

/** 后端返回的配置分组列表（含分组元数据与各分组 configs）。 */
const configGroups = ref<RowData[]>([]);

/** 各分组下的原始配置行（含 id、configKey 等，用于提交）。 */
const itemsByGroup = ref<Record<string, RowData[]>>({});

/** 各分组下可编辑的参数值，以 configKey 为键。 */
const valueModelsByGroup = reactive<Record<string, Record<string, string>>>({});

const formRef = ref<FormInstance | null>(null);

/**
 * 作用：页面内业务方法：pickConfigGroups。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickConfigGroups(data: unknown) {
  return Array.isArray(data) ? (data as RowData[]) : [];
}

/** 当前 Tab 列表：顺序与后端 grouped 接口返回一致。 */
const groupTabs = computed(() => {
  return configGroups.value
    .map(group => {
      const key = String(group.groupKey || '');
      if (!key) return null;
      return {
        key,
        label: String(group.groupName || '') || CONFIG_GROUP_LABEL_MAP[key] || key
      };
    })
    .filter((tab): tab is { key: string; label: string } => Boolean(tab));
});

/** 当前激活分组下的配置项列表。 */
const activeGroupItems = computed(() => itemsByGroup.value[activeGroupKey.value] || []);

/**
 * 作用：判断是否满足条件：isSecretConfigKey。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isSecretConfigKey(configKey: string) {
  const key = String(configKey || '').toLowerCase();
  return key.includes('.secret') || key.endsWith('secret');
}

/**
 * 作用：页面内业务方法：preferTextarea。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function preferTextarea(configKey: string, configValue: string) {
  const key = String(configKey || '');
  if (key.includes('pagePath') || key.includes('path')) return true;
  return String(configValue || '').length > 80;
}

/**
 * 作用：判断是否满足条件：isBuiltInConfig。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isBuiltInConfig(item: RowData) {
  return Number(item.configType) === 1;
}

/**
 * 作用：判断是否满足条件：isConfigItemEditable。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isConfigItemEditable(item: RowData) {
  return canUpdateConfig.value && !isBuiltInConfig(item);
}

/**
 * 作用：初始化：initGroupValueModels。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function initGroupValueModels(groupKey: string, items: RowData[]) {
  const model: Record<string, string> = {};
  for (const item of items) {
    const configKey = String(item.configKey || '');
    if (configKey) {
      model[configKey] = item.configValue == null ? '' : String(item.configValue);
    }
  }
  valueModelsByGroup[groupKey] = model;
}

/**
 * 作用：应用配置或路由参数：applyGroupedConfig。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyGroupedConfig(groups: RowData[]) {
  configGroups.value = groups;
  const nextItemsByGroup: Record<string, RowData[]> = {};
  for (const group of groups) {
    const groupKey = String(group.groupKey || '');
    if (groupKey) {
      const items = Array.isArray(group.configs) ? (group.configs as RowData[]) : [];
      nextItemsByGroup[groupKey] = items;
      initGroupValueModels(groupKey, items);
    }
  }
  itemsByGroup.value = nextItemsByGroup;

  const tabKeys = groups.map(g => String(g.groupKey || '')).filter(Boolean);
  const firstWithData = tabKeys.find(key => (nextItemsByGroup[key] || []).length > 0);
  const firstTab = tabKeys[0];
  if (firstWithData && !(nextItemsByGroup[activeGroupKey.value] || []).length) {
    activeGroupKey.value = firstWithData;
  } else if (!tabKeys.includes(activeGroupKey.value)) {
    activeGroupKey.value = firstWithData || firstTab || '';
  }
}

/**
 * 作用：加载数据：loadFormData。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadFormData() {
  loading.value = true;
  try {
    const flat = await listSystemConfigGrouped({ includeLegacy: false });
    if (flat && typeof flat === 'object' && 'error' in flat && (flat as { error?: unknown }).error) {
      configGroups.value = [];
      itemsByGroup.value = {};
      return;
    }
    const data = flat != null && typeof flat === 'object' && 'data' in flat ? (flat as { data?: unknown }).data : flat;
    applyGroupedConfig(pickConfigGroups(data));
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：构造数据或配置：buildGroupSavePayload。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildGroupSavePayload(groupKey: string, items: RowData[], model: Record<string, string>) {
  return {
    groupKey,
    configs: items.map(item => {
      const configKey = String(item.configKey || '');
      return {
        id: item.id ?? item.configId,
        configName: item.configName,
        configKey,
        configValue: model[configKey],
        configType: item.configType,
        remark: item.remark
      };
    })
  };
}

/**
 * 作用：校验并提交：submitCurrentGroup。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitCurrentGroup() {
  if (!canUpdateConfig.value) {
    window.$message?.warning?.('无参数修改权限');
    return;
  }
  const items = activeGroupItems.value;
  const model = valueModelsByGroup[activeGroupKey.value] || {};
  const changed = items.filter(item => {
    // 内置参数仅展示，不参与保存提交，与旧版参数页删除限制口径一致。
    if (isBuiltInConfig(item)) return false;
    const key = String(item.configKey || '');
    const origin = item.configValue == null ? '' : String(item.configValue);
    return key && model[key] !== origin;
  });
  if (!changed.length) {
    window.$message?.info?.('当前分组无修改');
    return;
  }
  submitting.value = true;
  try {
    // 一次提交当前分组全部变更，走后端分组事务保存，避免逐条 PUT 导致部分成功部分失败。
    const flat = await saveSystemConfigGroup(buildGroupSavePayload(activeGroupKey.value, changed, model));
    if (flat && typeof flat === 'object' && 'error' in flat && (flat as { error?: unknown }).error) {
      return;
    }

    window.$message?.success?.('已保存');
    await loadFormData();
    emit('saved');
  } finally {
    submitting.value = false;
  }
}

/**
 * 作用：处理交互事件：handleRefreshCache。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleRefreshCache() {
  const flat = await refreshConfigCache();
  if (!notifyOnceSuccessFromFlatResult(flat, '参数缓存已刷新')) return;
  await loadFormData();
  emit('saved');
}

watch(
  groupTabs,
  tabs => {
    if (!tabs.length) return;
    if (!tabs.some(tab => tab.key === activeGroupKey.value)) {
      activeGroupKey.value = tabs[0].key;
    }
  },
  { immediate: true }
);

defineExpose({
  loadFormData
});

loadFormData();
</script>

<template>
  <!-- 参数配置面板：按 groupKey 分 Tab，组内批量编辑后 PUT grouped 保存 -->
  <div class="config-form-panel">
    <div class="config-form-panel__body">
      <ASpin :spinning="loading">
        <ATabs v-if="groupTabs.length" v-model:active-key="activeGroupKey" type="card">
          <ATabPane v-for="tab in groupTabs" :key="tab.key" :tab="tab.label">
            <AForm ref="formRef" layout="vertical" class="config-form-panel__form mt-8px">
              <template v-if="(itemsByGroup[tab.key] || []).length">
                <div
                  v-for="item in itemsByGroup[tab.key]"
                  :key="String(item.configKey || item.id)"
                  class="config-form-panel__item"
                >
                  <AFormItem>
                    <!-- 标签行：参数名称 + 键名括号；有备注时悬停 label 展示文字提示 -->
                    <template #label>
                      <ATooltip v-if="item.remark" :title="String(item.remark)" placement="topLeft">
                        <span class="config-form-panel__label config-form-panel__label--hint">
                          <span>{{ item.configName || item.configKey }}</span>
                          <span v-if="item.configKey" class="config-form-panel__key">({{ item.configKey }})</span>
                        </span>
                      </ATooltip>
                      <span v-else class="config-form-panel__label">
                        <span>{{ item.configName || item.configKey }}</span>
                        <span v-if="item.configKey" class="config-form-panel__key">({{ item.configKey }})</span>
                      </span>
                    </template>
                    <AInputPassword
                      v-if="isSecretConfigKey(String(item.configKey || ''))"
                      v-model:value="valueModelsByGroup[tab.key][String(item.configKey)]"
                      :disabled="!isConfigItemEditable(item)"
                      :placeholder="isBuiltInConfig(item) ? '内置参数不可修改' : `请输入${item.configName || '参数值'}`"
                      autocomplete="new-password"
                    />
                    <ATextarea
                      v-else-if="
                        preferTextarea(
                          String(item.configKey || ''),
                          valueModelsByGroup[tab.key][String(item.configKey)] || ''
                        )
                      "
                      v-model:value="valueModelsByGroup[tab.key][String(item.configKey)]"
                      :disabled="!isConfigItemEditable(item)"
                      :rows="3"
                      :placeholder="isBuiltInConfig(item) ? '内置参数不可修改' : `请输入${item.configName || '参数值'}`"
                    />
                    <AInput
                      v-else
                      v-model:value="valueModelsByGroup[tab.key][String(item.configKey)]"
                      :disabled="!isConfigItemEditable(item)"
                      :placeholder="isBuiltInConfig(item) ? '内置参数不可修改' : `请输入${item.configName || '参数值'}`"
                    />
                  </AFormItem>
                </div>
              </template>
              <AEmpty v-else description="该分组暂无配置项" />
            </AForm>
          </ATabPane>
        </ATabs>
        <AEmpty v-else description="暂无可维护的参数配置" />
      </ASpin>
    </div>
    <div class="config-form-panel__footer">
      <ASpace>
        <AButton v-if="canRefreshCache" :loading="loading" @click="handleRefreshCache">刷新参数缓存</AButton>
        <AButton v-if="canUpdateConfig" type="primary" :loading="submitting" @click="submitCurrentGroup">保存</AButton>
      </ASpace>
    </div>
  </div>
</template>

<style scoped>
.config-form-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 12px;
}

/* 表单区域占满剩余高度，内容超出可视区时纵向滚动 */
.config-form-panel__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.config-form-panel__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}

.config-form-panel__item + .config-form-panel__item {
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border, #f0f0f0);
}

.config-form-panel__label {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.config-form-panel__label--hint {
  cursor: help;
}

.config-form-panel__key {
  font-size: 12px;
  font-weight: normal;
  color: rgba(0, 0, 0, 0.45);
}
</style>
