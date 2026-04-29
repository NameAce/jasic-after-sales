<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Modal } from 'ant-design-vue';
import {
  fetchChangePassword,
  fetchCreateWechatBindQrcode,
  fetchGetUserInfo,
  fetchGetWechatBindStatus,
  fetchUnbindWechat,
  fetchUpdateProfile
} from '@/service/api';
import { getResponseMsg } from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({
  name: 'UserCenter'
});

type WechatBindState = {
  bound: boolean;
  maskedOpenid: string;
  wechatPhone: string;
  expireAt: string;
  hasActiveTicket: boolean;
};

const authStore = useAuthStore();

const profileLoading = ref(false);
const passwordLoading = ref(false);
const bindLoading = ref(false);
const bindQrImage = ref('');
const unbindPassword = ref('');

const profileForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  currentPassword: ''
});

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const bindStatus = reactive<WechatBindState>({
  bound: false,
  maskedOpenid: '',
  wechatPhone: '',
  expireAt: '',
  hasActiveTicket: false
});

const roleText = computed(() => {
  return authStore.roleKeys.length ? authStore.roleKeys.join('、') : '-';
});

function applyBindStatus(raw: unknown) {
  const data = (raw || {}) as Record<string, unknown>;
  bindStatus.bound = Boolean(data.bound);
  bindStatus.maskedOpenid = String(data.maskedOpenid || '');
  bindStatus.wechatPhone = String(data.wechatPhone || '');
  bindStatus.expireAt = String(data.expireAt || '');
  bindStatus.hasActiveTicket = Boolean(data.hasActiveTicket);
}

async function loadUserInfo() {
  const { data, error } = await fetchGetUserInfo();
  if (error || !data) return;

  profileForm.username = String(data.username || data.userName || '');
  profileForm.realName = String(data.realName || '');
  profileForm.phone = String(data.phone || '');
  profileForm.email = String(data.email || '');
  profileForm.currentPassword = '';
}

function resetProfileForm() {
  loadUserInfo();
}

function resetPasswordForm() {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

async function submitProfile() {
  if (!profileForm.realName.trim()) {
    window.$message?.warning('请输入姓名');
    return;
  }
  if (!profileForm.phone.trim()) {
    window.$message?.warning('请输入手机号');
    return;
  }
  if (!profileForm.currentPassword.trim()) {
    window.$message?.warning('请输入当前密码以确认修改');
    return;
  }

  profileLoading.value = true;
  try {
    const payload: Record<string, unknown> = {
      realName: profileForm.realName.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      currentPassword: profileForm.currentPassword.trim()
    };
    const { error, response } = await fetchUpdateProfile(payload as Partial<Api.Auth.BackendUserInfo>);
    if (error) return;

    window.$message?.success(getResponseMsg(response, '资料修改成功'));
    await authStore.initUserInfo();
    await loadUserInfo();
  } finally {
    profileLoading.value = false;
  }
}

async function submitPassword() {
  if (!passwordForm.currentPassword.trim()) {
    window.$message?.warning('请输入当前密码');
    return;
  }
  if (!passwordForm.newPassword.trim()) {
    window.$message?.warning('请输入新密码');
    return;
  }
  if (passwordForm.confirmPassword !== passwordForm.newPassword) {
    window.$message?.warning('两次输入的新密码不一致');
    return;
  }

  passwordLoading.value = true;
  try {
    const { error, response } = await fetchChangePassword({
      oldPassword: passwordForm.currentPassword.trim(),
      newPassword: passwordForm.newPassword.trim()
    });
    if (error) return;

    window.$message?.success(getResponseMsg(response, '密码修改成功，请重新登录'));
    await authStore.resetStore();
  } finally {
    passwordLoading.value = false;
  }
}

async function refreshBindStatus() {
  bindLoading.value = true;
  try {
    const wasBound = bindStatus.bound;
    const { data, error, response } = await fetchGetWechatBindStatus();
    if (error) return;

    applyBindStatus(data);
    if (bindStatus.bound || !bindStatus.hasActiveTicket) {
      bindQrImage.value = '';
    }
    if (!wasBound && bindStatus.bound) {
      await authStore.initUserInfo();
      window.$message?.success(getResponseMsg(response, '微信绑定成功'));
    }
  } finally {
    bindLoading.value = false;
  }
}

async function generateBindQrcode() {
  bindLoading.value = true;
  try {
    const wasBound = bindStatus.bound;
    const { data, error, response } = await fetchCreateWechatBindQrcode();
    if (error) return;

    const payload = (data || {}) as Record<string, unknown>;
    applyBindStatus(payload);
    bindQrImage.value = String(payload.qrImageBase64 || '');

    if (bindStatus.bound) {
      await authStore.initUserInfo();
      window.$message?.success(getResponseMsg(response, wasBound ? '当前账号已绑定微信' : '微信绑定成功'));
      return;
    }
    if (bindQrImage.value) {
      window.$message?.success(getResponseMsg(response, '二维码已生成，请使用微信扫一扫'));
    }
  } finally {
    bindLoading.value = false;
  }
}

function handleUnbindWechat() {
  const currentPassword = unbindPassword.value.trim();
  if (!currentPassword) {
    window.$message?.warning('请输入当前密码');
    return;
  }

  Modal.confirm({
    title: '解绑微信',
    content: '解绑后当前账号将退出登录，如需继续使用请重新绑定。',
    okText: '确认解绑',
    cancelText: '取消',
    onOk: async () => {
      bindLoading.value = true;
      try {
        const { error, response } = await fetchUnbindWechat({ currentPassword } as Api.Auth.UnbindWechatParams);
        if (error) return;
        window.$message?.success(getResponseMsg(response, '微信解绑成功，请重新登录'));
        await authStore.resetStore();
      } finally {
        bindLoading.value = false;
      }
    }
  });
}

onMounted(async () => {
  await Promise.all([loadUserInfo(), refreshBindStatus()]);
});
</script>

<template>
  <div class="min-h-500px overflow-hidden lt-sm:overflow-auto">
    <ARow :gutter="[16, 16]">
      <ACol :xs="24" :lg="14">
        <ACard title="基本信息" :bordered="false">
          <AForm layout="vertical">
            <AFormItem label="用户名">
              <AInput v-model:value="profileForm.username" disabled />
            </AFormItem>
            <AFormItem label="姓名" required>
              <AInput v-model:value="profileForm.realName" placeholder="请输入姓名" />
            </AFormItem>
            <AFormItem label="手机号" required>
              <AInput v-model:value="profileForm.phone" placeholder="请输入手机号" />
            </AFormItem>
            <AFormItem label="邮箱">
              <AInput v-model:value="profileForm.email" placeholder="请输入邮箱" />
            </AFormItem>
            <AFormItem label="当前公司">
              <AInput :value="authStore.userInfo.currentCompanyName || '-'" disabled />
            </AFormItem>
            <AFormItem label="当前角色">
              <AInput :value="roleText" disabled />
            </AFormItem>
            <AFormItem label="当前密码" required>
              <AInputPassword
                v-model:value="profileForm.currentPassword"
                placeholder="请输入当前密码以确认修改"
                allow-clear
              />
            </AFormItem>
            <ASpace>
              <AButton type="primary" :loading="profileLoading" @click="submitProfile">保存资料</AButton>
              <AButton @click="resetProfileForm">重置</AButton>
            </ASpace>
          </AForm>
        </ACard>

        <ACard title="修改密码" :bordered="false" class="mt-16px">
          <AForm layout="vertical">
            <AFormItem label="当前密码" required>
              <AInputPassword v-model:value="passwordForm.currentPassword" placeholder="请输入当前密码" allow-clear />
            </AFormItem>
            <AFormItem label="新密码" required>
              <AInputPassword v-model:value="passwordForm.newPassword" placeholder="请输入新密码" allow-clear />
            </AFormItem>
            <AFormItem label="确认密码" required>
              <AInputPassword v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" allow-clear />
            </AFormItem>
            <ASpace>
              <AButton type="primary" :loading="passwordLoading" @click="submitPassword">确认修改</AButton>
              <AButton @click="resetPasswordForm">重置</AButton>
            </ASpace>
          </AForm>
        </ACard>
      </ACol>

      <ACol :xs="24" :lg="10">
        <ACard :bordered="false">
          <template #title>
            <div class="flex items-center justify-between">
              <span>微信绑定</span>
              <AButton size="small" @click="refreshBindStatus">刷新状态</AButton>
            </div>
          </template>

          <div v-if="bindLoading" class="h-220px flex items-center justify-center">
            <ASpin />
          </div>

          <template v-else-if="bindStatus.bound">
            <AAlert message="当前账号已绑定微信" type="success" show-icon />
            <div class="mt-16px flex items-start">
              <span class="w-90px flex-shrink-0 text-#909399">微信标识</span>
              <span class="break-all">{{ bindStatus.maskedOpenid || '-' }}</span>
            </div>
            <div class="mt-10px flex items-start">
              <span class="w-90px flex-shrink-0 text-#909399">微信手机号</span>
              <span class="break-all">{{ bindStatus.wechatPhone || '-' }}</span>
            </div>
            <AForm layout="vertical" class="mt-16px">
              <AFormItem label="当前密码（解绑验证）" required>
                <AInputPassword v-model:value="unbindPassword" placeholder="请输入当前密码" allow-clear />
              </AFormItem>
              <AButton size="small" danger :loading="bindLoading" @click="handleUnbindWechat">解绑微信</AButton>
            </AForm>
          </template>

          <template v-else>
            <AAlert message="当前账号尚未绑定微信" type="info" show-icon />
            <div class="mt-16px text-#606266 leading-1.7">
              请使用微信扫一扫下方二维码，在 B 端小程序中确认绑定。绑定时不会覆盖当前账号手机号，仅保存微信标识。
            </div>
            <div class="mt-16px border border-#dcdfe6 rounded-8px border-dashed bg-#f8fafc p-14px">
              <div
                class="mx-auto w-220px flex items-center justify-center rounded-8px bg-#fff p-10px shadow-[inset_0_0_0_1px_#ebeef5]"
              >
                <img
                  v-if="bindQrImage"
                  :src="bindQrImage"
                  alt="微信绑定二维码"
                  class="h-220px w-220px object-contain"
                />
                <div
                  v-else
                  class="min-h-220px w-220px flex items-center justify-center text-center text-#909399 leading-1.7"
                >
                  {{
                    bindStatus.hasActiveTicket
                      ? '当前存在有效二维码，如需继续绑定请重新生成。'
                      : '点击下方按钮生成绑定二维码。'
                  }}
                </div>
              </div>
              <div class="mt-14px text-center">
                <AButton size="small" type="primary" @click="generateBindQrcode">
                  {{ bindStatus.hasActiveTicket ? '重新生成二维码' : '生成二维码' }}
                </AButton>
              </div>
            </div>
            <div class="mt-10px flex items-start">
              <span class="w-90px flex-shrink-0 text-#909399">过期时间</span>
              <span>{{ bindStatus.expireAt || '-' }}</span>
            </div>
          </template>
        </ACard>
      </ACol>
    </ARow>
  </div>
</template>

<style scoped></style>
