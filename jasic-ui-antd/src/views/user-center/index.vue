<script setup lang="ts">
/**
 * 个人中心：资料修改、改密、微信绑定状态与二维码等（对接 auth 用户信息接口）。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { Modal } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
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

// 全局认证状态（当前用户、公司等）
const authStore = useAuthStore();

// 资料保存按钮加载态
const profileLoading = ref(false);
const profileFormRef = ref<FormInstance | null>(null);
// 修改密码提交加载态
const passwordLoading = ref(false);
const passwordFormRef = ref<FormInstance | null>(null);
// 微信绑定相关请求加载态
const bindLoading = ref(false);
// 绑定二维码图片（Base64 或 URL）
const bindQrImage = ref('');
// 解绑微信验证表单
const unbindFormRef = ref<FormInstance | null>(null);
const unbindForm = reactive({
  currentPassword: ''
});

// 个人资料表单模型
const profileForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  currentPassword: ''
});

/** 最近一次从服务端同步的资料快照，用于「重置」立即回退未保存的编辑 */
const profileBaseline = reactive({
  username: '',
  realName: '',
  phone: '',
  email: ''
});

// 修改密码表单模型
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 微信绑定状态（是否已绑定、票据与过期等）
const bindStatus = reactive<WechatBindState>({
  bound: false,
  maskedOpenid: '',
  wechatPhone: '',
  expireAt: '',
  hasActiveTicket: false
});

// 当前用户角色键名展示文案（顿号分隔）
const roleText = computed(() => {
  return authStore.roleKeys.length ? authStore.roleKeys.join('、') : '-';
});

const mobileReg = /^1[3-9]\d{9}$/;

const profileFormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: mobileReg, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  currentPassword: [{ required: true, message: '请输入当前密码以确认修改', trigger: 'blur' }]
};

const passwordFormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: async () => {
        if (passwordForm.confirmPassword.trim() !== passwordForm.newPassword.trim()) {
          return Promise.reject(new Error('两次输入的新密码不一致'));
        }
        return Promise.resolve();
      },
      trigger: 'blur'
    }
  ]
};

const unbindFormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }]
};

/**
 * 作用：将接口返回的微信绑定状态写入本地响应式对象。
 * @param raw - 接口原始数据
 * @returns {void} 无
 */
function applyBindStatus(raw: unknown) {
  const data = (raw || {}) as Record<string, unknown>;
  bindStatus.bound = Boolean(data.bound);
  bindStatus.maskedOpenid = String(data.maskedOpenid || '');
  bindStatus.wechatPhone = String(data.wechatPhone || '');
  bindStatus.expireAt = String(data.expireAt || '');
  bindStatus.hasActiveTicket = Boolean(data.hasActiveTicket);
}

/**
 * 作用：将接口用户资料写入表单模型，并同步更新重置基线快照。
 * @param raw - 用户信息接口 data
 * @returns {void} 无
 */
function applyProfileFormData(raw: Record<string, unknown>) {
  const username = String(raw.username || raw.userName || '');
  const realName = String(raw.realName || '');
  const phone = String(raw.phone || '');
  const email = String(raw.email || '');

  profileForm.username = username;
  profileForm.realName = realName;
  profileForm.phone = phone;
  profileForm.email = email;
  profileForm.currentPassword = '';

  profileBaseline.username = username;
  profileBaseline.realName = realName;
  profileBaseline.phone = phone;
  profileBaseline.email = email;
}

/**
 * 作用：用本地基线快照回退资料表单（不依赖异步请求即可立刻生效）。
 * @param 无
 * @returns {void} 无
 */
function restoreProfileFormFromBaseline() {
  profileForm.username = profileBaseline.username;
  profileForm.realName = profileBaseline.realName;
  profileForm.phone = profileBaseline.phone;
  profileForm.email = profileBaseline.email;
  profileForm.currentPassword = '';
}

/**
 * 作用：拉取当前用户资料并回填资料表单。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function loadUserInfo() {
  const { data, error } = await fetchGetUserInfo();
  if (error || !data) return;

  applyProfileFormData(data as unknown as Record<string, unknown>);
}

/**
 * 作用：重置资料表单为最近一次服务端快照，并清理校验态；随后静默拉取最新资料。
 * @param 无
 * @returns 返回 Promise，重置流程结束后结束
 */
async function resetProfileForm() {
  restoreProfileFormFromBaseline();
  await nextTick();
  profileFormRef.value?.clearValidate();
  await loadUserInfo();
  window.$message?.success('资料已重置');
}

/**
 * 作用：清空修改密码表单输入并恢复表单校验态。
 * @param 无
 * @returns 返回 Promise，重置流程结束后结束
 */
async function resetPasswordForm() {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  await nextTick();
  passwordFormRef.value?.resetFields();
  passwordFormRef.value?.clearValidate();
  window.$message?.success('密码表单已重置');
}

/**
 * 作用：校验并提交个人资料修改。
 * @param 无
 * @returns 返回 Promise，提交流程结束后结束
 */
async function submitProfile() {
  try {
    await profileFormRef.value?.validate();
  } catch {
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
    const { data, error, response } = await fetchUpdateProfile(payload as Partial<Api.Auth.BackendUserInfo>);
    if (error) return;

    window.$message?.success(getResponseMsg(response, '资料修改成功'));
    // 后端 updateProfile 已返回最新用户对象，直接同步表单与全局用户态，避免再请求 user-info
    if (data) {
      applyProfileFormData(data as unknown as Record<string, unknown>);
      await authStore.initUserInfo(data);
      return;
    }
    await authStore.initUserInfo();
  } finally {
    profileLoading.value = false;
  }
}

/**
 * 作用：校验并提交修改密码，成功后退出登录。
 * @param 无
 * @returns 返回 Promise，提交流程结束后结束
 */
async function submitPassword() {
  try {
    await passwordFormRef.value?.validate();
  } catch {
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

/**
 * 作用：刷新微信绑定状态；若由未绑定变为已绑定则提示并刷新用户信息。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
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

/**
 * 作用：请求生成微信绑定二维码并更新绑定状态与图片。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
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

/**
 * 作用：校验密码后弹出确认框，确认则调用解绑并退出登录。
 * @param 无
 * @returns {void} 无
 */
async function handleUnbindWechat() {
  try {
    await unbindFormRef.value?.validate();
  } catch {
    return;
  }
  const currentPassword = unbindForm.currentPassword.trim();

  Modal.confirm({
    title: '解绑微信',
    content: '解绑后当前账号将退出登录，如需继续使用请重新绑定。',
    okText: '确认解绑',
    cancelText: '取消',
    onOk: async () => {
      bindLoading.value = true;
      try {
        const { error, response } = await fetchUnbindWechat({
          currentPassword
        } as Api.Auth.UnbindWechatParams);
        if (error) return;
        window.$message?.success(getResponseMsg(response, '微信解绑成功，请重新登录'));
        unbindForm.currentPassword = '';
        await authStore.resetStore();
      } finally {
        bindLoading.value = false;
      }
    }
  });
}

/**
 * 作用：进入页面时并行加载用户资料与微信绑定状态。
 * @param 无
 * @returns 返回 Promise，初始化完成后结束
 */
onMounted(async () => {
  await Promise.all([loadUserInfo(), refreshBindStatus()]);
});
</script>

<template>
  <div class="min-h-500px overflow-hidden lt-sm:overflow-auto">
    <ARow :gutter="[16, 16]">
      <ACol :xs="24" :lg="14">
        <ACard title="基本信息" :bordered="false">
          <AForm ref="profileFormRef" layout="vertical" :model="profileForm" :rules="profileFormRules as any">
            <AFormItem label="用户名">
              <AInput v-model:value="profileForm.username" disabled />
            </AFormItem>
            <AFormItem label="姓名" name="realName" required>
              <AInput v-model:value="profileForm.realName" placeholder="请输入姓名" />
            </AFormItem>
            <AFormItem label="手机号" name="phone" required>
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
            <AFormItem label="当前密码" name="currentPassword" required>
              <AInputPassword
                v-model:value="profileForm.currentPassword"
                placeholder="请输入当前密码以确认修改"
                allow-clear
              />
            </AFormItem>
            <ASpace>
              <AButton type="primary" :loading="profileLoading" @click="submitProfile">保存资料</AButton>
              <AButton html-type="button" @click="resetProfileForm">重置</AButton>
            </ASpace>
          </AForm>
        </ACard>

        <ACard title="修改密码" :bordered="false" class="mt-16px">
          <AForm ref="passwordFormRef" layout="vertical" :model="passwordForm" :rules="passwordFormRules as any">
            <AFormItem label="当前密码" name="currentPassword" required>
              <AInputPassword v-model:value="passwordForm.currentPassword" placeholder="请输入当前密码" allow-clear />
            </AFormItem>
            <AFormItem label="新密码" name="newPassword" required>
              <AInputPassword v-model:value="passwordForm.newPassword" placeholder="请输入新密码" allow-clear />
            </AFormItem>
            <AFormItem label="确认密码" name="confirmPassword" required>
              <AInputPassword v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" allow-clear />
            </AFormItem>
            <ASpace>
              <AButton type="primary" :loading="passwordLoading" @click="submitPassword">确认修改</AButton>
              <AButton html-type="button" @click="resetPasswordForm">重置</AButton>
            </ASpace>
          </AForm>
        </ACard>
      </ACol>

      <ACol :xs="24" :lg="10" class="min-w-0">
        <ACard :bordered="false" class="w-full">
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
            <AForm
              ref="unbindFormRef"
              layout="vertical"
              class="mt-16px"
              :model="unbindForm"
              :rules="unbindFormRules as any"
            >
              <AFormItem label="当前密码（解绑验证）" name="currentPassword" required>
                <AInputPassword v-model:value="unbindForm.currentPassword" placeholder="请输入当前密码" allow-clear />
              </AFormItem>
              <AButton size="small" danger :loading="bindLoading" @click="handleUnbindWechat">解绑微信</AButton>
            </AForm>
          </template>

          <template v-else>
            <AAlert message="当前账号尚未绑定微信" type="info" show-icon />
            <p class="wechat-bind-hint text-#606266">
              请使用微信扫一扫下方二维码，在 B 端小程序中确认绑定。绑定时不会覆盖当前账号手机号，仅保存微信标识。
            </p>
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
                <div v-else class="wechat-bind-qr-placeholder min-h-220px w-full text-center text-#909399">
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

<style scoped>
/* leading-1.7 在 Uno 中会按间距刻度换算成极小行高，此处用倍率行高避免说明文字被纵向挤压 */
.wechat-bind-hint {
  margin: 16px 0 0;
  line-height: 1.7;
  word-break: break-word;
  white-space: normal;
}

.wechat-bind-qr-placeholder {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 10px;
  line-height: 1.7;
  word-break: break-word;
  white-space: normal;
}
</style>
