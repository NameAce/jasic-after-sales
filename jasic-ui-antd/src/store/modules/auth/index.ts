import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { defineStore } from 'pinia';
import { useLoading } from '@sa/hooks';
import { router } from '@/router';
import {
  fetchChooseCompany,
  fetchGetUserInfo,
  fetchLogin,
  fetchLogout,
  fetchMpBindConfirm,
  fetchMpBindLogin,
  fetchMpLogin
} from '@/service/api';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { useRouteStore } from '../route';
import { useTabStore } from '../tab';
import { clearAuthStorage, getToken } from './shared';

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const route = useRoute();
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  const token = ref(getToken());
  const companyOptions = ref<Api.Auth.CompanyOption[]>([]);

  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    userName: '',
    roles: [],
    buttons: []
  });

  /** Is login */
  const isLogin = computed(() => Boolean(token.value));
  /** 与 jasic-ui user.js：仅当后端声明 needChooseCompany 时进入选公司流程；user-info 里带 companies 列表不触发 */
  const needChooseCompany = ref(false);
  const roleKeys = computed(() => Array.from(new Set(userInfo.roles.filter(Boolean))));
  /** Is super role in static route */
  const isStaticSuper = computed(() => {
    const { VITE_AUTH_ROUTE_MODE, VITE_STATIC_SUPER_ROLE } = import.meta.env;

    return VITE_AUTH_ROUTE_MODE === 'static' && roleKeys.value.includes(VITE_STATIC_SUPER_ROLE);
  });

  /**
   * 登录链路边界约定：
   * - PC 主链路：login -> loginByToken -> getUserInfo/chooseCompany -> redirect
   * - mp 兼容链路：loginByWechatCode / bindWechatAndLogin / confirmWechatBindAndLogin
   *   仅兼容保留，默认不由 PC 登录入口触发
   */

  type RawUserInfo = {
    userId?: string;
    id?: Api.Common.IdLike;
    realName?: string;
    userName?: string;
    username?: string;
    roles?: Array<string | Api.Auth.BackendRole>;
    buttons?: string[];
    perms?: string[];
    currentCompanyId?: Api.Common.IdLike;
    currentCompanyName?: string;
    currentTypeCode?: string;
    currentSubjectType?: string;
  };

  function normalizeRoles(roles: RawUserInfo['roles']) {
    if (!Array.isArray(roles)) return [];

    return roles
      .map(role => {
        // 主授权口径仅使用 roleKey，字符串角色不进入主授权计算。
        if (typeof role === 'string') return '';
        if (role && typeof role === 'object') {
          return role.roleKey || '';
        }
        return '';
      })
      .filter(Boolean);
  }

  function normalizeButtons(raw: RawUserInfo | null | undefined) {
    if (Array.isArray(raw?.perms)) {
      return raw.perms;
    }
    if (Array.isArray(raw?.buttons)) {
      return raw.buttons;
    }
    return [];
  }

  function normalizeUserInfo(raw: RawUserInfo | null | undefined): Api.Auth.UserInfo {
    const roles = normalizeRoles(raw?.roles);
    const buttons = normalizeButtons(raw);

    return {
      userId: String(raw?.userId || raw?.id || ''),
      // 与 jasic-ui Navbar：realName || username
      userName: String(raw?.realName || raw?.userName || raw?.username || ''),
      roles,
      buttons,
      currentCompanyId: raw?.currentCompanyId,
      currentCompanyName: raw?.currentCompanyName,
      currentTypeCode: raw?.currentTypeCode,
      currentSubjectType: raw?.currentSubjectType
    };
  }

  function applyUserInfo(raw: RawUserInfo | null | undefined) {
    const normalized = normalizeUserInfo(raw);
    Object.assign(userInfo, normalized);
  }

  function hasAnyRole(targetRoles: string[]) {
    if (!targetRoles.length) return true;
    return roleKeys.value.some(role => targetRoles.includes(role));
  }

  function applyLoginResponseContext(payload: Api.Auth.LoginResponse | Api.Auth.BackendUserInfo | null | undefined) {
    if (!payload) {
      companyOptions.value = [];
      needChooseCompany.value = false;
      return;
    }

    const companies = Array.isArray(payload.companies) ? payload.companies : [];
    const needSelect = Boolean(payload.needChooseCompany);
    needChooseCompany.value = needSelect;
    companyOptions.value = needSelect ? companies : [];

    if ('userInfo' in payload && payload.userInfo) {
      applyUserInfo(payload.userInfo);
      return;
    }

    if (!needSelect) {
      applyUserInfo(payload as Api.Auth.BackendUserInfo);
    }
  }

  /** Reset auth store */
  async function resetStore() {
    const authStore = useAuthStore();

    clearAuthStorage();

    authStore.$reset();

    if (!route.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    routeStore.resetStore();
  }

  /** Logout with backend session cleanup */
  async function logout() {
    try {
      await fetchLogout();
    } finally {
      await resetStore();
    }
  }

  /**
   * Login
   *
   * @param userName User name
   * @param password Password
   * @param [redirect=true] Whether to redirect after login. Default is `true`
   */
  async function login(userName: string, password: string, redirect = true) {
    // PC 主流程登录入口：仅密码登录会走该分支（/auth/login）。
    startLoading();

    const { data: loginToken, error } = await fetchLogin(userName, password);

    if (!error) {
      const pass = await loginByToken(loginToken);

      if (pass) {
        await redirectFromLogin(redirect);

        window.$notification?.success({
          message: $t('page.login.common.loginSuccess'),
          description: $t('page.login.common.welcomeBack', { userName: userInfo.userName })
        });
      } else if (needChooseCompany.value) {
        // 与 jasic-ui login/index.vue：needChooseCompany → `/choose-company`（不保留 redirect）
        await router.push({ name: 'choose-company' });
      }
    } else {
      resetStore();
    }

    endLoading();
  }

  async function loginByToken(loginToken: Api.Auth.LoginToken) {
    // 1. stored in the localStorage, the later requests need it in headers
    localStg.set('token', loginToken.token);
    if (loginToken.refreshToken) {
      localStg.set('refreshToken', loginToken.refreshToken);
    } else {
      localStg.remove('refreshToken');
    }

    const loginResponse = loginToken as Api.Auth.LoginResponse;

    applyLoginResponseContext(loginResponse);

    if (needChooseCompany.value) {
      // 与 jasic-ui user.js：先落 token，再进入选公司页（含仅关联一家公司时也手动确认）
      token.value = loginToken.token;
      return false;
    }

    // 2. get user info
    const pass = userInfo.userId ? true : await getUserInfo();

    if (pass) {
      token.value = loginToken.token;

      return true;
    }

    return false;
  }

  async function getUserInfo() {
    // PC 主流程用户态拉取口径：/auth/user-info
    const { data: info, error } = await fetchGetUserInfo();

    if (!error) {
      applyLoginResponseContext(info);

      return true;
    }

    return false;
  }

  async function chooseCompany(companyId: Api.Common.IdLike) {
    // PC 主流程选公司口径：/auth/choose-company
    const { data, error } = await fetchChooseCompany({ companyId });

    if (!error) {
      applyLoginResponseContext(data);
      return true;
    }

    return false;
  }

  async function initUserInfo() {
    const hasToken = getToken();

    if (hasToken) {
      const pass = await getUserInfo();

      if (!pass) {
        resetStore();
      }
    }
  }

  async function loginByWechatCode(code: string, redirect = true) {
    // 非 PC 主流程：该函数仅用于兼容保留的 mp 登录链路，不应作为 PC 登录入口接入。
    startLoading();

    const { data, error } = await fetchMpLogin({ code });

    if (!error) {
      const pass = await loginByToken(data);
      if (pass) {
        await redirectFromLogin(redirect);
      } else if (needChooseCompany.value) {
        await router.push({ name: 'choose-company' });
      }
    }

    endLoading();
  }

  async function bindWechatAndLogin(payload: Api.Auth.MpBindLoginParams, redirect = true) {
    // 非 PC 主流程：该函数仅用于兼容保留的 mp 绑定登录链路，不应由 PC register/reset-pwd 触发。
    startLoading();

    const { data, error } = await fetchMpBindLogin(payload);
    if (!error) {
      const pass = await loginByToken(data);
      if (pass) {
        await redirectFromLogin(redirect);
      } else if (needChooseCompany.value) {
        await router.push({ name: 'choose-company' });
      }
    }

    endLoading();
  }

  async function confirmWechatBindAndLogin(payload: Api.Auth.MpBindConfirmParams, redirect = true) {
    // 非 PC 主流程：该函数仅用于兼容保留的 mp 绑定确认链路，不应由 PC 登录页主入口触发。
    startLoading();

    const { data, error } = await fetchMpBindConfirm(payload);
    if (!error) {
      const pass = await loginByToken(data);
      if (pass) {
        await redirectFromLogin(redirect);
      } else if (needChooseCompany.value) {
        await router.push({ name: 'choose-company' });
      }
    }

    endLoading();
  }

  return {
    token,
    userInfo,
    companyOptions,
    isStaticSuper,
    isLogin,
    needChooseCompany,
    roleKeys,
    loginLoading,
    hasAnyRole,
    resetStore,
    logout,
    login,
    chooseCompany,
    initUserInfo,
    loginByWechatCode,
    bindWechatAndLogin,
    confirmWechatBindAndLogin
  };
});
