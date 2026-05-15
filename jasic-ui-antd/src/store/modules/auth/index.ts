/**
 * 鉴权与会话：登录/登出、选公司、用户信息、token 与小程序绑定等，并与路由/页签 store 协同。
 */
import { computed, nextTick, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { defineStore } from 'pinia';
import { Modal } from 'ant-design-vue';
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

/** 登出接口最长等待时间；超时也继续清本地态，避免后端无响应时确认框一直 loading */
const LOGOUT_API_WAIT_MS = 3000;

export const useAuthStore = defineStore(SetupStoreId.Auth, () => {
  const route = useRoute();
  const routeStore = useRouteStore();
  const tabStore = useTabStore();
  const { toLogin, redirectFromLogin } = useRouterPush(false);
  const { loading: loginLoading, startLoading, endLoading } = useLoading();

  // 访问令牌，与本地存储同步
  const token = ref(getToken());
  // 需要选公司时后端返回的可选公司列表
  const companyOptions = ref<Api.Auth.CompanyOption[]>([]);

  // 当前登录用户展示信息与权限口径
  const userInfo: Api.Auth.UserInfo = reactive({
    userId: '',
    userName: '',
    roles: [],
    buttons: []
  });

  // 是否已持有 token（不代表 userInfo 一定已拉取）
  const isLogin = computed(() => Boolean(token.value));
  /** 与 jasic-ui user.js：仅当后端声明 needChooseCompany 时进入选公司流程；user-info 里带 companies 列表不触发 */
  const needChooseCompany = ref(false);
  // 去重后的角色 key 列表，用于静态超管等判断
  const roleKeys = computed(() => Array.from(new Set(userInfo.roles.filter(Boolean))));
  // 静态路由模式下是否命中配置的超级角色
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

  /**
   * 将后端角色数组规范为 roleKey 字符串数组（忽略纯字符串角色）。
   *
   * @param roles - 原始角色列表
   * @returns {string[]} 角色 key 列表
   */
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

  /**
   * 从用户信息中提取按钮/权限码列表（优先 perms）。
   *
   * @param raw - 原始用户字段
   * @returns {string[]} 权限码数组
   */
  function normalizeButtons(raw: RawUserInfo | null | undefined) {
    if (Array.isArray(raw?.perms)) {
      return raw.perms;
    }
    if (Array.isArray(raw?.buttons)) {
      return raw.buttons;
    }
    return [];
  }

  /**
   * 将后端用户对象转为前端统一的 UserInfo 结构。
   *
   * @param raw - 原始用户信息
   * @returns {Api.Auth.UserInfo} 规范化后的用户信息
   */
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

  /**
   * 用规范化结果覆盖响应式 userInfo。
   *
   * @param raw - 原始用户信息
   * @returns {void} 无返回值
   */
  function applyUserInfo(raw: RawUserInfo | null | undefined) {
    const normalized = normalizeUserInfo(raw);
    Object.assign(userInfo, normalized);
  }

  /**
   * 判断当前用户是否拥有目标角色中的任意一个。
   *
   * @param targetRoles - 目标角色 key 列表；空数组视为通过
   * @returns {boolean} 是否匹配
   */
  function hasAnyRole(targetRoles: string[]) {
    if (!targetRoles.length) return true;
    return roleKeys.value.some(role => targetRoles.includes(role));
  }

  /**
   * 根据登录或用户信息接口结果更新 token 上下文、公司与用户字段。
   *
   * @param payload - 登录响应或用户信息体
   * @returns {void} 无返回值
   */
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

  /**
   * 清空登录态与本地凭证，并按需跳转登录、重置路由与标签缓存。
   * 末尾 `Modal.destroyAll` 须在导航完成之后执行，避免在「退出确认」onOk 执行过程中拆掉当前 Modal 导致界面卡死。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function resetStore() {
    const authStore = useAuthStore();

    clearAuthStorage();

    authStore.$reset();

    if (!route.meta.constant) {
      await toLogin();
    }

    tabStore.cacheTabs();
    await routeStore.resetStore();

    // 须在导航与路由重置之后执行：若在「退出确认」的 onOk 链路内过早 destroyAll，会拆掉当前 Modal 实例，易导致 AntDV 卡死
    await nextTick();
    Modal.destroyAll();
  }

  /**
   * 调用后端登出接口后执行本地 resetStore。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function logout() {
    try {
      await Promise.race([
        fetchLogout(),
        new Promise<void>(resolve => {
          setTimeout(resolve, LOGOUT_API_WAIT_MS);
        })
      ]);
    } finally {
      await resetStore();
    }
  }

  /**
   * PC 密码登录：换 token、拉用户信息或进入选公司，并按需跳转与提示。
   *
   * @param userName - 用户名
   * @param password - 密码
   * @param redirect - 登录成功后是否执行登录前重定向，默认 true
   * @returns {Promise<void>} 无返回值
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

  /**
   * 使用登录接口返回的 token 落库并继续拉取用户态或标记需选公司。
   *
   * @param loginToken - 含 access/refresh token 及可选用户与公司上下文
   * @returns {Promise<boolean>} 是否已完成可进入系统的用户态（false 可能表示需选公司）
   */
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

  /**
   * 请求 /auth/user-info 并合并到本地 userInfo / 选公司状态。
   *
   * @returns {Promise<boolean>} 是否拉取成功
   */
  async function getUserInfo() {
    // PC 主流程用户态拉取口径：/auth/user-info
    const { data: info, error } = await fetchGetUserInfo();

    if (!error) {
      applyLoginResponseContext(info);

      return true;
    }

    return false;
  }

  /**
   * 提交选中的公司并完成登录后用户上下文更新。
   *
   * @param companyId - 公司主键
   * @returns {Promise<boolean>} 是否成功
   */
  async function chooseCompany(companyId: Api.Common.IdLike) {
    // PC 主流程选公司口径：/auth/choose-company
    const { data, error } = await fetchChooseCompany({ companyId });

    if (!error) {
      applyLoginResponseContext(data);
      return true;
    }

    return false;
  }

  /**
   * 应用启动时若本地有 token 则尝试静默拉取用户信息，失败则清理登录态。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function initUserInfo() {
    const hasToken = getToken();

    if (hasToken) {
      const pass = await getUserInfo();

      if (!pass) {
        resetStore();
      }
    }
  }

  /**
   * 小程序等场景：用微信 code 换 token（兼容链路，非 PC 主入口）。
   *
   * @param code - 微信授权码
   * @param redirect - 成功后是否重定向
   * @returns {Promise<void>} 无返回值
   */
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

  /**
   * 绑定微信并登录（兼容 mp 链路）。
   *
   * @param payload - 绑定登录参数
   * @param redirect - 成功后是否重定向
   * @returns {Promise<void>} 无返回值
   */
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

  /**
   * 确认微信绑定并完成登录（兼容 mp 链路）。
   *
   * @param payload - 绑定确认参数
   * @param redirect - 成功后是否重定向
   * @returns {Promise<void>} 无返回值
   */
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
