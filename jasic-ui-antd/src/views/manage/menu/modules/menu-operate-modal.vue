<script setup lang="tsx">
/**
 * 菜单管理 — 新增/编辑弹窗：菜单类型、路由、图标、排序与权限等表单项。
 */
import { computed, nextTick, ref, watch } from 'vue';
import { SimpleScrollbar } from '@sa/materials';
import { enableStatusOptions, menuIconTypeOptions, menuTypeOptions } from '@/constants/business';
import { fetchGetAllRoles } from '@/service/api';
import { useAntdForm, useFormRules } from '@/hooks/common/form';
import { adaptiveModalWidth } from '@/hooks/common/modal-form-layout';
import { getLocalIcons } from '@/utils/icon';
import { $t } from '@/locales';
import SvgIcon from '@/components/custom/svg-icon.vue';
import {
  getLayoutAndPage,
  getPathParamFromRoutePath,
  getRoutePathByRouteName,
  getRoutePathWithParam,
  transformLayoutAndPageToComponent
} from './shared';

defineOptions({
  name: 'MenuOperateModal'
});

export type OperateType = AntDesign.TableOperateType | 'addChild';

interface Props {
  /** 操作类型：新增/编辑/加子级 */
  operateType: OperateType;
  /** 编辑行数据；新增子级时为父级行 */
  rowData?: Api.SystemManage.Menu | null;
  /** 系统内全部页面路径（供 page 下拉） */
  allPages: string[];
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, resetFields } = useAntdForm();
const { defaultRequiredRule } = useFormRules();

// 抽屉标题（随 operateType 变化）
const title = computed(() => {
  const titles: Record<OperateType, string> = {
    add: $t('page.manage.menu.addMenu'),
    addChild: $t('page.manage.menu.addChildMenu'),
    edit: $t('page.manage.menu.editMenu')
  };
  return titles[props.operateType];
});

type Model = Pick<
  Api.SystemManage.Menu,
  | 'menuType'
  | 'menuName'
  | 'routeName'
  | 'routePath'
  | 'component'
  | 'order'
  | 'i18nKey'
  | 'icon'
  | 'iconType'
  | 'status'
  | 'parentId'
  | 'keepAlive'
  | 'constant'
  | 'href'
  | 'hideInMenu'
  | 'activeMenu'
  | 'multiTab'
  | 'fixedIndexInTab'
> & {
  query: NonNullable<Api.SystemManage.Menu['query']>;
  buttons: NonNullable<Api.SystemManage.Menu['buttons']>;
  layout: string;
  page: string;
  pathParam: string;
};

// 菜单表单模型
const model = ref(createDefaultModel());

/**
 * 作用：创建菜单表单的默认值。
 * @param 无
 * @returns 默认 Model
 */
function createDefaultModel(): Model {
  return {
    menuType: '1',
    menuName: '',
    routeName: '',
    routePath: '',
    pathParam: '',
    component: '',
    layout: '',
    page: '',
    i18nKey: null,
    icon: '',
    iconType: '1',
    parentId: 0,
    status: '1',
    keepAlive: false,
    constant: false,
    order: 0,
    href: null,
    hideInMenu: false,
    activeMenu: null,
    multiTab: false,
    fixedIndexInTab: null,
    query: [],
    buttons: []
  };
}

type RuleKey = Extract<keyof Model, 'menuName' | 'status' | 'routeName' | 'routePath'>;

const rules: Record<RuleKey, App.Global.FormRule> = {
  menuName: defaultRequiredRule,
  status: defaultRequiredRule,
  routeName: defaultRequiredRule,
  routePath: defaultRequiredRule
};

// 编辑时禁止改菜单类型
const disabledMenuType = computed(() => props.operateType === 'edit');

const localIcons = getLocalIcons();
const localIconOptions = localIcons.map(item => ({
  label: () => (
    <div class="flex-y-center gap-16px">
      <SvgIcon localIcon={item} class="text-icon" />
      <span>{item}</span>
    </div>
  ),
  value: item
}));

// 顶级菜单时展示布局选择
const showLayout = computed(() => model.value.parentId === 0);

// 菜单类型为「页面」时展示 page 选择
const showPage = computed(() => model.value.menuType === '2');

/** 菜单表单顶层 AFormItem 数量下限（含 query/buttons 两块），恒大于 6，用于抽屉宽度至少 720 */
const manageMenuFormFieldCount = computed(() => {
  let n = 16; // 类型、名称、路由名、路径、参数、i18n、排序、图标类型、图标、状态、缓存、常量、外链、隐藏、多页签、固定序号
  if (showLayout.value) n += 1;
  if (showPage.value) n += 1;
  if (model.value.hideInMenu) n += 1;
  n += 2; // query、buttons
  return n;
});

const manageMenuDrawerWidth = computed(() =>
  adaptiveModalWidth(960, manageMenuFormFieldCount.value)
);

// 页面组件下拉选项（含当前 routeName 兜底）
const pageOptions = computed(() => {
  const allPages = [...props.allPages];

  if (model.value.routeName && !allPages.includes(model.value.routeName)) {
    allPages.unshift(model.value.routeName);
  }

  const opts: CommonType.Option[] = allPages.map(page => ({
    label: page,
    value: page
  }));

  return opts;
});

const layoutOptions: CommonType.Option[] = [
  {
    label: 'base',
    value: 'base'
  },
  {
    label: 'blank',
    value: 'blank'
  }
];

/** 角色下拉（表单扩展示例用） */
const roleOptions = ref<CommonType.Option<string>[]>([]);

/**
 * 作用：拉取全部角色选项。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function getRoleOptions() {
  const { error, data } = await fetchGetAllRoles();

  if (!error) {
    const options = data.map(item => ({
      label: item.roleName,
      value: item.roleCode
    }));

    roleOptions.value = [...options];
  }
}

/**
 * 作用：在路由 query 列表指定下标后插入一项。
 * @param index - 参考下标，新项插在 index+1
 * @returns {void} 无
 */
function addQuery(index: number) {
  model.value.query.splice(index + 1, 0, {
    key: '',
    value: ''
  });
}

/**
 * 作用：删除路由 query 指定下标项。
 * @param index - 下标
 * @returns {void} 无
 */
function removeQuery(index: number) {
  model.value.query.splice(index, 1);
}

/**
 * 作用：在按钮权限列表指定下标后插入一项。
 * @param index - 参考下标
 * @returns {void} 无
 */
function addButton(index: number) {
  model.value.buttons.splice(index + 1, 0, {
    code: '',
    desc: ''
  });
}

/**
 * 作用：删除按钮项指定下标。
 * @param index - 下标
 * @returns {void} 无
 */
function removeButton(index: number) {
  model.value.buttons.splice(index, 1);
}

/**
 * 作用：根据 operateType 与 rowData 初始化或回填表单。
 * @param 无
 * @returns 返回 Promise，nextTick 与赋值完成后结束
 */
async function handleInitModel() {
  model.value = createDefaultModel();

  if (!props.rowData) return;

  await nextTick();

  if (props.operateType === 'addChild') {
    const { id } = props.rowData;

    Object.assign(model.value, { parentId: id });
  }

  if (props.operateType === 'edit') {
    const { component, ...rest } = props.rowData;

    const { layout, page } = getLayoutAndPage(component);
    const { path, param } = getPathParamFromRoutePath(rest.routePath);

    Object.assign(model.value, rest, { layout, page, routePath: path, pathParam: param });
  }

  if (!model.value.query) {
    model.value.query = [];
  }
  if (!model.value.buttons) {
    model.value.buttons = [];
  }
}

/**
 * 作用：关闭菜单编辑抽屉。
 * @param 无
 * @returns {void} 无
 */
function closeDrawer() {
  visible.value = false;
}

/**
 * 作用：根据 routeName 同步生成 routePath。
 * @param 无
 * @returns {void} 无
 */
function handleUpdateRoutePathByRouteName() {
  if (model.value.routeName) {
    model.value.routePath = getRoutePathByRouteName(model.value.routeName);
  } else {
    model.value.routePath = '';
  }
}

/**
 * 作用：根据 routeName 同步生成 i18nKey。
 * @param 无
 * @returns {void} 无
 */
function handleUpdateI18nKeyByRouteName() {
  if (model.value.routeName) {
    model.value.i18nKey = `route.${model.value.routeName}` as App.I18n.I18nKey;
  } else {
    model.value.i18nKey = null;
  }
}

/**
 * 作用：组装提交给后端的菜单参数（component、routePath 等派生字段）。
 * @param 无
 * @returns 提交用对象
 */
function getSubmitParams() {
  const { layout, page, pathParam, ...params } = model.value;

  const component = transformLayoutAndPageToComponent(layout, page);
  const routePath = getRoutePathWithParam(model.value.routePath, pathParam);

  params.component = component;
  params.routePath = routePath;

  return params;
}

/**
 * 作用：校验表单并模拟保存成功（示例未接接口）。
 * @param 无
 * @returns 返回 Promise，校验与提示完成后结束
 */
async function handleSubmit() {
  await validate();

  const params = getSubmitParams();

  console.log('params: ', params);

  // request
  window.$message?.success($t('common.updateSuccess'));
  closeDrawer();
  emit('submitted');
}

// 打开弹窗时初始化表单、清校验并拉角色
watch(visible, () => {
  if (visible.value) {
    handleInitModel();
    resetFields();
    getRoleOptions();
  }
});

// routeName 变化时联动 routePath 与 i18nKey
watch(
  () => model.value.routeName,
  () => {
    handleUpdateRoutePathByRouteName();
    handleUpdateI18nKeyByRouteName();
  }
);
</script>

<template>
  <ADrawer v-model:open="visible" :title="title" :width="manageMenuDrawerWidth">
    <div class="h-480px">
      <SimpleScrollbar>
        <AForm ref="formRef" layout="vertical" :model="model" :rules="rules" class="pr-20px">
          <ARow :gutter="16">
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.menuType')" name="menuType">
                <ARadioGroup v-model:value="model.menuType" :disabled="disabledMenuType">
                  <ARadio v-for="item in menuTypeOptions" :key="item.value" :value="item.value">
                    {{ $t(item.label) }}
                  </ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.menuName')" name="menuName">
                <AInput v-model:value="model.menuName" :placeholder="$t('page.manage.menu.form.menuName')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.routeName')" name="routeName">
                <AInput v-model:value="model.routeName" :placeholder="$t('page.manage.menu.form.routeName')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.routePath')" name="routePath">
                <AInput v-model:value="model.routePath" disabled :placeholder="$t('page.manage.menu.form.routePath')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.pathParam')" name="pathParam">
                <AInput v-model:value="model.pathParam" :placeholder="$t('page.manage.menu.form.pathParam')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem v-if="showLayout" :label="$t('page.manage.menu.layout')" name="layout">
                <ASelect
                  v-model:value="model.layout"
                  :options="layoutOptions"
                  :placeholder="$t('page.manage.menu.form.layout')"
                />
              </AFormItem>
            </ACol>
            <ACol v-if="showPage" :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.page')" name="page">
                <ASelect
                  v-model:value="model.page"
                  :options="pageOptions"
                  :placeholder="$t('page.manage.menu.form.page')"
                />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.i18nKey')" name="i18nKey">
                <AInput v-model:value="model.i18nKey as string" :placeholder="$t('page.manage.menu.form.i18nKey')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.order')" name="order">
                <AInputNumber
                  v-model:value="model.order as number"
                  class="w-full"
                  :placeholder="$t('page.manage.menu.form.order')"
                />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.iconTypeTitle')" name="iconType">
                <ARadioGroup v-model:value="model.iconType">
                  <ARadio v-for="item in menuIconTypeOptions" :key="item.value" :value="item.value">
                    {{ $t(item.label) }}
                  </ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>

            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.icon')" name="icon">
                <template v-if="model.iconType === '1'">
                  <AInput v-model:value="model.icon" :placeholder="$t('page.manage.menu.form.icon')" class="flex-1">
                    <template #suffix>
                      <SvgIcon v-if="model.icon" :icon="model.icon" class="text-icon" />
                    </template>
                  </AInput>
                </template>
                <template v-if="model.iconType === '2'">
                  <ASelect
                    v-model:value="model.icon"
                    :placeholder="$t('page.manage.menu.form.localIcon')"
                    :options="localIconOptions"
                  />
                </template>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.menuStatus')" name="status">
                <ARadioGroup v-model:value="model.status">
                  <ARadio v-for="item in enableStatusOptions" :key="item.value" :value="item.value">
                    {{ $t(item.label) }}
                  </ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.keepAlive')" name="keepAlive">
                <ARadioGroup v-model:value="model.keepAlive">
                  <ARadio :value="true">{{ $t('common.yesOrNo.yes') }}</ARadio>
                  <ARadio :value="false">{{ $t('common.yesOrNo.no') }}</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.constant')" name="constant">
                <ARadioGroup v-model:value="model.constant">
                  <ARadio value>
                    {{ $t('common.yesOrNo.yes') }}
                  </ARadio>
                  <ARadio :value="false">
                    {{ $t('common.yesOrNo.no') }}
                  </ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.href')" name="href">
                <AInput v-model:value="model.href as string" :placeholder="$t('page.manage.menu.form.href')" />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.hideInMenu')" name="hideInMenu">
                <ARadioGroup v-model:value="model.hideInMenu">
                  <ARadio :value="true">{{ $t('common.yesOrNo.yes') }}</ARadio>
                  <ARadio :value="false">{{ $t('common.yesOrNo.no') }}</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol v-if="model.hideInMenu" :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.activeMenu')" name="activeMenu">
                <ASelect
                  v-model:value="model.activeMenu as string"
                  :options="pageOptions"
                  clearable
                  :placeholder="$t('page.manage.menu.form.activeMenu')"
                />
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.multiTab')" name="multiTab">
                <ARadioGroup v-model:value="model.multiTab">
                  <ARadio value :label="$t('common.yesOrNo.yes')" />
                  <ARadio :value="false" :label="$t('common.yesOrNo.no')" />
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :lg="12" :xs="24">
              <AFormItem :label="$t('page.manage.menu.fixedIndexInTab')" name="fixedIndexInTab">
                <AInputNumber
                  v-model:value="model.fixedIndexInTab as number"
                  class="w-full"
                  clearable
                  :placeholder="$t('page.manage.menu.form.fixedIndexInTab')"
                />
              </AFormItem>
            </ACol>
            <ACol :span="24">
              <AFormItem :label-col="{ span: 4 }" :label="$t('page.manage.menu.query')" name="query">
                <AButton v-if="model.query.length === 0" type="dashed" block @click="addQuery(-1)">
                  <template #icon>
                    <icon-carbon-add class="align-sub text-icon" />
                  </template>
                  <span class="ml-8px">{{ $t('common.add') }}</span>
                </AButton>
                <template v-else>
                  <div v-for="(item, index) in model.query" :key="index" class="flex gap-3">
                    <ACol :span="9">
                      <AFormItem :name="['query', index, 'key']">
                        <AInput
                          v-model:value="item.key"
                          :placeholder="$t('page.manage.menu.form.queryKey')"
                          class="flex-1"
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="9">
                      <AFormItem :name="['query', index, 'value']">
                        <AInput
                          v-model:value="item.value"
                          :placeholder="$t('page.manage.menu.form.queryValue')"
                          class="flex-1"
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="5">
                      <ASpace class="ml-12px">
                        <AButton size="middle" @click="addQuery(index)">
                          <template #icon>
                            <icon-ic-round-plus class="align-sub text-icon" />
                          </template>
                        </AButton>
                        <AButton size="middle" @click="removeQuery(index)">
                          <template #icon>
                            <icon-ic-round-remove class="align-sub text-icon" />
                          </template>
                        </AButton>
                      </ASpace>
                    </ACol>
                  </div>
                </template>
              </AFormItem>
            </ACol>
            <ACol :span="24">
              <AFormItem :label-col="{ span: 4 }" :label="$t('page.manage.menu.button')" name="buttons">
                <AButton v-if="model.buttons.length === 0" type="dashed" block @click="addButton(-1)">
                  <template #icon>
                    <icon-carbon-add class="align-sub text-icon" />
                  </template>
                  <span class="ml-8px">{{ $t('common.add') }}</span>
                </AButton>
                <template v-else>
                  <div v-for="(item, index) in model.buttons" :key="index" class="flex gap-3">
                    <ACol :span="9">
                      <AFormItem :name="['buttons', index, 'code']">
                        <AInput
                          v-model:value="item.code"
                          :placeholder="$t('page.manage.menu.form.buttonCode')"
                          class="flex-1"
                        ></AInput>
                      </AFormItem>
                    </ACol>
                    <ACol :span="9">
                      <AFormItem :name="['buttons', index, 'desc']">
                        <AInput
                          v-model:value="item.desc"
                          :placeholder="$t('page.manage.menu.form.buttonDesc')"
                          class="flex-1"
                        ></AInput>
                      </AFormItem>
                    </ACol>
                    <ACol :span="5">
                      <ASpace class="ml-12px">
                        <AButton size="middle" @click="addButton(index)">
                          <template #icon>
                            <icon-ic-round-plus class="align-sub text-icon" />
                          </template>
                        </AButton>
                        <AButton size="middle" @click="removeButton(index)">
                          <template #icon>
                            <icon-ic-round-remove class="align-sub text-icon" />
                          </template>
                        </AButton>
                      </ASpace>
                    </ACol>
                  </div>
                </template>
              </AFormItem>
            </ACol>
          </ARow>
        </AForm>
      </SimpleScrollbar>
    </div>
    <template #footer>
      <ASpace justify="end" :size="16">
        <AButton @click="closeDrawer">{{ $t('common.cancel') }}</AButton>
        <AButton type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</AButton>
      </ASpace>
    </template>
  </ADrawer>
</template>

<style scoped></style>
