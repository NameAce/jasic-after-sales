/**
 * HTTP 请求实例：主后端 createFlatRequest、多 baseURL 的 createRequest，统一鉴权与业务错误码处理。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { Modal } from 'ant-design-vue';
import type { AxiosError } from 'axios';
import { BACKEND_ERROR_CODE, createFlatRequest, createRequest } from '@sa/axios';
import { router } from '@/router';
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { getAuthorization, getResponseMsg, showErrorMsg } from './shared';
import {
  shouldSkipSessionExpiredModalForUrl,
  shouldSkipSessionExpiredModalOnLoginRoute
} from './skip-session-expired-modal';
import type { RequestInstanceState } from './type';

/** 开发环境且开启代理时，请求将走 Vite 代理前缀
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL, otherBaseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

/**
 * 作用：将环境变量中的逗号分隔错误码解析为去空白的字符串数组。
 * @param raw 原始字符串
 * @returns {string[]} 错误码列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function parseCodeList(raw: string | undefined) {
  return (raw?.split(',') || []).map(c => c.trim()).filter(Boolean);
}

type ExceptionRouteName = '403' | '404' | '500';

/**
 * 作用：跳转到统一异常页（403/404/500），携带接口返回的 msg；避免同文案重复 replace。
 * @param routeName 目标异常路由名
 * @param msg 接口 msg/message，展示在异常页
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function redirectToExceptionPage(routeName: ExceptionRouteName, msg?: string) {
  const current = router.currentRoute.value;
  const currentName = String(current.name || '');
  const trimmed = msg?.trim() || '';
  const query = trimmed ? { msg: trimmed } : {};

  if (currentName === routeName && String(current.query.msg || '') === trimmed) {
    return;
  }

  router.push({ name: routeName, query, replace: true }).catch(() => {});
}

/**
 * 作用：判断当前是否处于首页相关路由，用于无权限时在首页仅弹消息而不跳 403。
 * @returns {boolean} 是否在首页
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function isOnHomeRoute() {
  const current = router.currentRoute.value;
  const routeName = String(current.name || '');
  const routePath = String(current.path || '');

  return routeName === 'root' || routeName === 'home' || routePath === '/' || routePath.startsWith('/home');
}

function showForbiddenError(state: RequestInstanceState, message: string) {
  if (isOnHomeRoute()) {
    showErrorMsg(state, message || '没有操作权限');
    return;
  }
  redirectToExceptionPage('403', message);
}

function tryHandleHttpStatusError(ctx: {
  state: RequestInstanceState;
  httpStatus: number | undefined;
  httpBodyMsg: string;
  message: string;
}) {
  const { state, httpStatus, httpBodyMsg, message } = ctx;
  if (httpStatus === 403) {
    const statusMsg = httpBodyMsg || message;
    showForbiddenError(state, statusMsg || '没有操作权限');
    return true;
  }
  if (httpStatus === 404) {
    redirectToExceptionPage('404', httpBodyMsg || message);
    return true;
  }
  if (typeof httpStatus === 'number' && httpStatus >= 500) {
    redirectToExceptionPage('500', httpBodyMsg || message);
    return true;
  }
  return false;
}

const defaultRequestHeaders: Record<string, string> = {};
// 联调 Apifox 等工具时可在请求头附加 token
const apifoxToken = import.meta.env.VITE_APIFOX_TOKEN;
if (apifoxToken) {
  defaultRequestHeaders.apifoxToken = apifoxToken;
}

/**
 * 作用：主后端扁平请求实例：附加 Authorization、按业务码处理登出/无权限/500、转换 data 与网络错误提示。
 * @remarks 与 `@sa/axios` createFlatRequest 配置对象配对导出
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const request = createFlatRequest<App.Service.Response, RequestInstanceState>(
  {
    baseURL,
    timeout: 30000,
    headers: defaultRequestHeaders
  },
  {
    async onRequest(config) {
      const Authorization = getAuthorization();
      if (Authorization) {
        Object.assign(config.headers, { Authorization });
      }

      return config;
    },
    isBackendSuccess(response) {
      return String(response.data.code) === import.meta.env.VITE_SERVICE_SUCCESS_CODE;
    },
    async onBackendFail(response) {
      const authStore = useAuthStore();
      const responseCode = String(response.data.code);
      const responseData = response.data as App.Service.Response & { message?: string };
      const responseMsg = responseData.msg || responseData.message || '操作失败';

      function handleLogout() {
        authStore.resetStore();
      }

      async function logoutAndCleanup() {
        window.removeEventListener('beforeunload', handleLogout);

        if (!request.state.modalLogoutShownCodes) {
          request.state.modalLogoutShownCodes = [];
        }
        request.state.modalLogoutShownCodes = request.state.modalLogoutShownCodes.filter(code => code !== responseCode);

        await authStore.resetStore();
      }

      const logoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_LOGOUT_CODES);
      if (logoutCodes.includes(responseCode)) {
        handleLogout();
        return null;
      }

      const modalLogoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES);
      const requestUrl = String(response.config?.url || '');
      const skipSessionExpiredModal =
        shouldSkipSessionExpiredModalForUrl(requestUrl) || shouldSkipSessionExpiredModalOnLoginRoute();

      const shownCodes = request.state.modalLogoutShownCodes || [];
      if (modalLogoutCodes.includes(responseCode) && !shownCodes.includes(responseCode) && !skipSessionExpiredModal) {
        request.state.modalLogoutShownCodes = [...shownCodes, responseCode];

        window.addEventListener('beforeunload', handleLogout);

        // 清理可能残留的确认框遮罩，避免多层 mask 导致整页无法点击
        Modal.destroyAll();

        window.$modal?.error({
          title: '提示',
          content: '登录已过期，请重新登录',
          okText: '重新登录',
          cancelText: '取消',
          maskClosable: false,
          onOk() {
            return logoutAndCleanup();
          },
          onCancel() {
            return logoutAndCleanup();
          }
        });

        return null;
      }

      // 与 jasic-ui request.js：A0200 固定「没有操作权限」+ warning；其它码可通过 VITE_SERVICE_FORBIDDEN_CODES 扩展
      const forbiddenEnv = import.meta.env.VITE_SERVICE_FORBIDDEN_CODES;
      const forbiddenCodes = parseCodeList(forbiddenEnv ?? 'A0200');
      if (forbiddenCodes.includes(responseCode)) {
        if (isOnHomeRoute()) {
          showErrorMsg(request.state, responseMsg || '没有操作权限');
          return null;
        }
        redirectToExceptionPage('403', responseMsg);
        return null;
      }

      const serverErrorCodes = parseCodeList(import.meta.env.VITE_SERVICE_SERVER_ERROR_CODES ?? 'A0500');
      if (serverErrorCodes.includes(responseCode)) {
        redirectToExceptionPage('500', responseMsg);
        return null;
      }

      showErrorMsg(request.state, responseMsg);

      return null;
    },
    transformBackendResponse(response) {
      return response.data.data;
    },
    onError(error) {
      /** 业务码失败已在 `onBackendFail` 里提示，避免与 `showErrorMsg` 重复
       * @修改人 黄碧莲
       * @修改时间 2026-05-14
       */
      if ((error as AxiosError)?.code === BACKEND_ERROR_CODE) {
        return;
      }

      let message = error.message || '网络错误';
      let backendErrorCode = '';
      const httpStatus = error.response?.status;

      const errorData = (error.response?.data || {}) as App.Service.Response & { message?: string };
      message = errorData.msg || errorData.message || message;
      backendErrorCode = String(errorData.code) || '';

      const httpBodyMsg = getResponseMsg(error.response, message);

      const modalLogoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES);
      if (modalLogoutCodes.includes(backendErrorCode)) {
        return;
      }

      const forbiddenEnvErr = import.meta.env.VITE_SERVICE_FORBIDDEN_CODES;
      const forbiddenCodes = parseCodeList(forbiddenEnvErr ?? 'A0200');
      if (forbiddenCodes.includes(backendErrorCode)) {
        showForbiddenError(request.state, message);
        return;
      }

      const serverErrorCodes = parseCodeList(import.meta.env.VITE_SERVICE_SERVER_ERROR_CODES ?? 'A0500');
      if (serverErrorCodes.includes(backendErrorCode)) {
        redirectToExceptionPage('500', message);
        return;
      }

      if (tryHandleHttpStatusError({ state: request.state, httpStatus, httpBodyMsg, message })) {
        return;
      }

      showErrorMsg(request.state, message);
    }
  }
);

/**
 * 作用：演示/第二基地址请求实例（成功判断与数据结构不同于主接口）。
 * @remarks 使用 demo 服务 baseURL 与独立 token 头格式
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const demoRequest = createRequest<App.Service.DemoResponse>(
  {
    baseURL: otherBaseURL.demo
  },
  {
    async onRequest(config) {
      const { headers } = config;

      const token = localStg.get('token');
      const Authorization = token ? `Bearer ${token}` : null;
      Object.assign(headers, { Authorization });

      return config;
    },
    isBackendSuccess(response) {
      return response.data.status === '200';
    },
    async onBackendFail(_response) {},
    transformBackendResponse(response) {
      return response.data.result;
    },
    onError(error) {
      let message = error.message;

      if (error.code === BACKEND_ERROR_CODE) {
        message = error.response?.data?.message || message;
      }

      window.$message?.error(message);
    }
  }
);
