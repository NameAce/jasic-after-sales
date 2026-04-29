<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Cascader as ACascader } from 'ant-design-vue';
import { tagColorPositiveNeutral } from '@/constants/list-status-tag';
import {
  createCompanyAddress,
  deleteCompanyAddress,
  listCompanyAddress,
  setDefaultCompanyAddress,
  updateCompanyAddress
} from '@/service/api';
import { getResponseMsg } from '@/service/request/shared';
import {
  chinaRegionCascaderOptions,
  composeAddressWithRegion,
  isFullRegionSelection,
  splitFullAddressToRegionAndDetail
} from '@/utils/china-region';

type RowData = Record<string, any>;

const loading = ref(false);
const rows = ref<RowData[]>([]);
const formOpen = ref(false);
const submitting = ref(false);
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

const columns = [
  { title: '公司ID', dataIndex: 'companyId', key: 'companyId', width: 120 },
  { title: '地址详情', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '是否默认', dataIndex: 'isDefault', key: 'isDefault', width: 120 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 140 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 160 },
  { title: '操作', dataIndex: 'actions', key: 'actions', width: 220 }
];

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

async function loadList() {
  loading.value = true;
  const { data } = await listCompanyAddress();
  rows.value = pickRows(data);
  loading.value = false;
}

async function setDefault(record: RowData) {
  const id = record.addressId ?? record.id;
  if (!id) return;
  await setDefaultCompanyAddress(id);
  loadList();
}

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

function openEdit(record: RowData) {
  formTitle.value = '编辑地址';
  formModel.id = Number(record.id ?? record.addressId);
  formModel.contactName = String(record.contactName ?? '');
  formModel.contactPhone = String(record.contactPhone ?? '');
  const rawAddr = String(record.address ?? '');
  const parsed = splitFullAddressToRegionAndDetail(rawAddr);
  formModel.regionCodes = [...parsed.regionCodes];
  formModel.addressDetail = parsed.addressDetail;
  if (rawAddr.trim() && !parsed.regionCodes.length) {
    window.$message?.warning('未能从当前地址识别省市区，请手动选择省、市、区');
  }
  formModel.isDefault = Number(record.isDefault) === 1 ? 1 : 0;
  formOpen.value = true;
}

async function submitForm() {
  if (!formModel.contactName.trim() || !formModel.contactPhone.trim()) {
    window.$message?.warning('请填写联系人和联系电话');
    return;
  }

  const detail = formModel.addressDetail.trim();
  if (!isFullRegionSelection(formModel.regionCodes)) {
    window.$message?.warning('请选择完整的省、市、区');
    return;
  }
  if (!detail) {
    window.$message?.warning('请填写详细地址');
    return;
  }

  const address = composeAddressWithRegion(formModel.regionCodes, detail);

  submitting.value = true;
  try {
    const payload = {
      contactName: formModel.contactName.trim(),
      contactPhone: formModel.contactPhone.trim(),
      address,
      isDefault: formModel.isDefault
    };
    if (formModel.id) {
      const { response } = await updateCompanyAddress({ id: formModel.id, ...payload });
      window.$message?.success(getResponseMsg(response, '已保存'));
    } else {
      const { response } = await createCompanyAddress(payload);
      window.$message?.success(getResponseMsg(response, '已保存'));
    }
    formOpen.value = false;
    await loadList();
  } finally {
    submitting.value = false;
  }
}

async function removeAddress(record: RowData) {
  const id = Number(record.id ?? record.addressId);
  if (!Number.isFinite(id) || id <= 0) return;
  const { response } = await deleteCompanyAddress(id);
  window.$message?.success(getResponseMsg(response, '已删除'));
  await loadList();
}

onMounted(loadList);
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard title="公司地址管理" :bordered="false" class="flex-col-stretch card-wrapper sm:flex-1-hidden">
      <div class="mb-12px">
        <AButton type="primary" @click="openCreate">新增地址</AButton>
      </div>
      <ATable :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isDefault'">
            <ATag :color="tagColorPositiveNeutral(Number(record.isDefault) === 1)">
              {{ Number(record.isDefault) === 1 ? '是' : '否' }}
            </ATag>
          </template>
          <template v-if="column.key === 'actions'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openEdit(record)">
                编辑
              </AButton>
              <AButton type="link" size="small" class="table-action-link--success" @click="setDefault(record)">
                设为默认
              </AButton>
              <APopconfirm title="确认删除该地址？" @confirm="removeAddress(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="formOpen" :title="formTitle" :width="400">
      <AForm layout="vertical" class="mt-8px">
        <AFormItem label="联系人" required>
          <AInput v-model:value="formModel.contactName" />
        </AFormItem>
        <AFormItem label="联系电话" required>
          <AInput v-model:value="formModel.contactPhone" />
        </AFormItem>
        <AFormItem label="省市区" required>
          <ACascader
            v-model:value="formModel.regionCodes"
            class="w-full"
            :options="chinaRegionCascaderOptions"
            placeholder="请选择省 / 市 / 区"
            allow-clear
          />
        </AFormItem>
        <AFormItem label="详细地址" required>
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
