<script setup lang="ts">
/**
 * 公司地址簿：当前公司下联系人地址列表、默认地址与增删改（对接 company-address 接口）。
 */
import { computed, onMounted, reactive, ref } from 'vue';
import { Cascader as ACascader } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import { tagColorPositiveNeutral } from '@/constants/list-status-tag';
import {
  createCompanyAddress,
  deleteCompanyAddress,
  listCompanyAddress,
  setDefaultCompanyAddress,
  updateCompanyAddress
} from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import {
  type RegionCascaderOption,
  composeAddressWithRegion,
  fetchRegionCascaderOptions,
  isFullRegionSelection,
  loadRegionCascaderData,
  splitFullAddressToRegionAndDetail
} from '@/utils/china-region';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { createAntTableActionColumn } from '@/utils/table-action-width';

type RowData = Record<string, any>;

/** 操作列：编辑 / 设为默认 / 删除 同一行最多时横排估算 */

const pageMenuTitle = useRouteMenuTitle();

// 省市区级联选项（懒加载子节点）
const regionOptions = ref<RegionCascaderOption[]>([]);

// 列表加载态
const loading = ref(false);
// 地址表格数据
const rows = ref<RowData[]>([]);

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

// 新增/编辑抽屉是否打开
const formOpen = ref(false);
// 地址编辑表单实例（与建单抽屉一致：rules + validate）
const addressFormRef = ref<FormInstance | null>(null);
// 表单提交中
const submitting = ref(false);
// 抽屉标题
const formTitle = ref('新增地址');
const formModel = reactive<{
  id?: number;
  contactName: string;
  contactPhone: string;
  /** 门牌、街道等，不含省市区；与 regionCodes 组合为入库 address */
  addressDetail: string;
  regionCodes: string[];
  isDefault: 0 | 1;
}>({
  id: undefined,
  contactName: '',
  contactPhone: '',
  addressDetail: '',
  regionCodes: [],
  isDefault: 0
});

const mobileReg = /^1[3-9]\d{9}$/;

const addressFormRules = computed(() => ({
  contactName: [{ required: true, message: '请填写联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    {
      pattern: mobileReg,
      message: '请输入正确的手机号码',
      trigger: 'blur'
    }
  ],
  regionCodes: [
    { required: true, message: '请选择完整的省、市、区', trigger: 'change' },
    {
      validator: async () => {
        if (!isFullRegionSelection(formModel.regionCodes)) {
          return Promise.reject(new Error('请选择完整的省、市、区'));
        }
        return Promise.resolve();
      },
      trigger: 'change'
    }
  ],
  addressDetail: [{ required: true, message: '请填写详细地址', trigger: 'blur' }]
}));

// 表格列配置
const columns = [
  { title: '公司ID', dataIndex: 'companyId', key: 'companyId', width: 120 },
  { title: '地址详情', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '是否默认', dataIndex: 'isDefault', key: 'isDefault', width: 120 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 140 },
  {
    title: '联系电话',
    dataIndex: 'contactPhone',
    key: 'contactPhone',
    width: 160
  },
  createAntTableActionColumn({ dataIndex: 'actions', width: 200 })
];

/**
 * 作用：从接口分页对象中取出列表数组。
 * @param data - 接口返回数据
 * @returns 表格行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：加载公司地址列表。
 */
async function loadList() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await listCompanyAddress();
    if (consumeFlatError(flat)) {
      rows.value = [];
      return;
    }
    const data = (flat as { data?: unknown }).data;
    rows.value = pickRows(data);
    refreshEmptySuccessMsg(flat, rows.value.length);
  } catch (e: unknown) {
    rows.value = [];
    setMsgFromCatch(e);
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：将指定地址设为默认地址。
 * @param record - 表格行数据
 */
async function setDefault(record: RowData) {
  const id = record.addressId ?? record.id;
  if (!id) return;
  await setDefaultCompanyAddress(id);
  loadList();
}

/**
 * 作用：打开新增抽屉并重置表单。
 */
function openCreate() {
  formTitle.value = '新增地址';
  formModel.id = undefined;
  formModel.contactName = '';
  formModel.contactPhone = '';
  formModel.addressDetail = '';
  formModel.regionCodes = [];
  formModel.isDefault = 0;
  formOpen.value = true;
}

/**
 * 作用：打开编辑抽屉并根据完整地址拆解省市区与详细地址。
 * @param record - 表格行数据
 */
async function openEdit(record: RowData) {
  formTitle.value = '编辑地址';
  formModel.id = Number(record.id ?? record.addressId);
  formModel.contactName = String(record.contactName ?? '');
  formModel.contactPhone = String(record.contactPhone ?? '');
  const rawAddr = String(record.address ?? '');
  const parsed = await splitFullAddressToRegionAndDetail(rawAddr);
  formModel.regionCodes = [...parsed.regionCodes];
  formModel.addressDetail = parsed.addressDetail;
  if (rawAddr.trim() && !parsed.regionCodes.length) {
    window.$message?.warning('未能从当前地址识别省市区，请手动选择省、市、区');
  }
  formModel.isDefault = Number(record.isDefault) === 1 ? 1 : 0;
  formOpen.value = true;
}

/**
 * 作用：校验并提交新增或编辑地址。
 */
async function submitForm() {
  try {
    await addressFormRef.value?.validate();
  } catch {
    return;
  }

  const detail = formModel.addressDetail.trim();
  const address = await composeAddressWithRegion(formModel.regionCodes, detail);

  submitting.value = true;
  try {
    const payload = {
      contactName: formModel.contactName.trim(),
      contactPhone: formModel.contactPhone.trim(),
      address,
      isDefault: formModel.isDefault
    };
    if (formModel.id) {
      const res = await updateCompanyAddress({ id: formModel.id, ...payload });
      if (!notifyOnceSuccessFromFlatResult(res, '已保存')) return;
    } else {
      const res = await createCompanyAddress(payload);
      if (!notifyOnceSuccessFromFlatResult(res, '已保存')) return;
    }
    formOpen.value = false;
    await loadList();
  } finally {
    submitting.value = false;
  }
}

/**
 * 作用：删除指定地址。
 * @param record - 表格行数据
 */
async function removeAddress(record: RowData) {
  const id = Number(record.id ?? record.addressId);
  if (!Number.isFinite(id) || id <= 0) return;
  const res = await deleteCompanyAddress(id);
  if (!notifyOnceSuccessFromFlatResult(res, '已删除')) return;
  await loadList();
}

onMounted(async () => {
  await Promise.all([loadList(), fetchRegionCascaderOptions().then(list => (regionOptions.value = list))]);
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :title="pageMenuTitle" :bordered="false" class="flex-col-stretch card-wrapper sm:flex-1-hidden">
      <div class="mb-12px">
        <AButton type="primary" @click="openCreate">新增地址</AButton>
      </div>
      <ATable
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        row-key="id"
        size="small"
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isDefault'">
            <ATag :color="tagColorPositiveNeutral(Number(record.isDefault) === 1)">
              {{ Number(record.isDefault) === 1 ? '是' : '否' }}
            </ATag>
          </template>
          <template v-if="column.key === 'actions'">
            <ASpace :size="2" :wrap="false">
              <AButton type="link" size="small" class="table-action-link--primary" @click="openEdit(record)">
                编辑
              </AButton>
              <APopconfirm
                :title="`确认将「${record.contactName || '该'}」设为默认地址？`"
                @confirm="setDefault(record)"
              >
                <AButton type="link" size="small" class="table-action-link--success">设为默认</AButton>
              </APopconfirm>
              <APopconfirm title="确认删除该地址？" @confirm="removeAddress(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="formOpen" :title="formTitle" :width="400">
      <AForm ref="addressFormRef" class="mt-8px" layout="vertical" :model="formModel" :rules="addressFormRules as any">
        <AFormItem label="联系人" name="contactName" required>
          <AInput v-model:value="formModel.contactName" />
        </AFormItem>
        <AFormItem label="联系电话" name="contactPhone" required>
          <AInput v-model:value="formModel.contactPhone" />
        </AFormItem>
        <AFormItem label="省市区" name="regionCodes" required>
          <ACascader
            v-model:value="formModel.regionCodes"
            class="w-full"
            :options="regionOptions"
            :load-data="loadRegionCascaderData"
            placeholder="请选择省 / 市 / 区"
            allow-clear
          />
        </AFormItem>
        <AFormItem label="详细地址" name="addressDetail" required>
          <ATextarea
            v-model:value="formModel.addressDetail"
            :rows="3"
            placeholder="请填写街道、门牌号等（不含省市区；保存时会与上方省市区拼接）"
          />
        </AFormItem>
        <AFormItem label="是否默认">
          <ASelect
            v-model:value="formModel.isDefault"
            :options="[
              { label: '是', value: 1 },
              { label: '否', value: 0 }
            ]"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="formOpen = false">取消</AButton>
          <AButton type="primary" :loading="submitting" @click="submitForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>
