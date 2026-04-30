/**
 * HTTP 请求实例：主后端 createFlatRequest、多 baseURL 的 createRequest，统一鉴权与业务错误码处理。
 */
import { BACKEND_ERROR_CODE, createFlatRequest, createRequest } from '@sa/axios';
import { router } from '@/router';
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { getAuthorization, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

/** 开发环境且开启代理时，请求将走 Vite 代理前缀 */
const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL, otherBaseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

/**
 * 作用：将环境变量中的逗号分隔错误码解析为去空白的字符串数组。
 * @param raw 原始字符串
 * @returns {string[]} 错误码列表
 */
function parseCodeList(raw: string | undefined) {
  return (raw?.split(',') || []).map(c => c.trim()).filter(Boolean);
}

type ExceptionRouteName = '403' | '404' | '500';

/**
 * 作用：跳转到统一异常页（403/404/500），避免与当前路由重复 push。
 * @param routeName 目标异常路由名
 * @returns {void}
 */
function redirectToExceptionPage(routeName: ExceptionRouteName) {
  const currentRouteName = String(router.currentRoute.value.name || '');
  if (currentRouteName === routeName) return;

  router.push({ name: routeName }).catch(() => {});
}

/**
 * 作用：判断当前是否处于首页相关路由，用于无权限时在首页仅弹消息而不跳 403。
 * @returns {boolean} 是否在首页
 */
function isOnHomeRoute() {
  const current = router.currentRoute.value;
  const routeName = String(current.name || '');
  const routePath = String(current.path || '');

  return routeName === 'root' || routeName === 'home' || routePath === '/' || routePath.startsWith('/home');
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

      function logoutAndCleanup() {
        handleLogout();
        window.removeEventListener('beforeunload', handleLogout);

        request.state.errMsgStack = request.state.errMsgStack.filter(code => code !== responseCode);
      }

      const logoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_LOGOUT_CODES);
      if (logoutCodes.includes(responseCode)) {
        handleLogout();
        return null;
      }

      const modalLogoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES);
      if (modalLogoutCodes.includes(responseCode) && !request.state.errMsgStack?.includes(responseCode)) {
        request.state.errMsgStack = [...(request.state.errMsgStack || []), responseCode];

        window.addEventListener('beforeunload', handleLogout);

        window.$modal?.error({
          title: '提示',
          content: '登录已过期，请重新登录',
          okText: '重新登录',
          cancelText: '取消',
          maskClosable: false,
          onOk() {
            logoutAndCleanup();
          },
          onCancel() {
            logoutAndCleanup();
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
        redirectToExceptionPage('403');
        return null;
      }

      const serverErrorCodes = parseCodeList(import.meta.env.VITE_SERVICE_SERVER_ERROR_CODES ?? 'A0500');
      if (serverErrorCodes.includes(responseCode)) {
        redirectToExceptionPage('500');
        return null;
      }

      showErrorMsg(request.state, responseMsg);

      return null;
    },
    transformBackendResponse(response) {
      return response.data.data;
    },
    onError(error) {
      let message = error.message || '网络错误';
      let backendErrorCode = '';
      const httpStatus = error.response?.status;

      if (error.code === BACKEND_ERROR_CODE) {
        const errorData = (error.response?.data || {}) as App.Service.Response & { message?: string };
        message = errorData.msg || errorData.message || message;
        backendErrorCode = String(errorData.code) || '';
      }

      const modalLogoutCodes = parseCodeList(import.meta.env.VITE_SERVICE_MODAL_LOGOUT_CODES);
      if (modalLogoutCodes.includes(backendErrorCode)) {
        return;
      }

      const forbiddenEnvErr = import.meta.env.VITE_SERVICE_FORBIDDEN_CODES;
      const forbiddenCodes = parseCodeList(forbiddenEnvErr ?? 'A0200');
      if (forbiddenCodes.includes(backendErrorCode)) {
        if (isOnHomeRoute()) {
          showErrorMsg(request.state, message || '没有操作权限');
          return;
        }
        redirectToExceptionPage('403');
        return;
      }

      const serverErrorCodes = parseCodeList(import.meta.env.VITE_SERVICE_SERVER_ERROR_CODES ?? 'A0500');
      if (serverErrorCodes.includes(backendErrorCode)) {
        redirectToExceptionPage('500');
        return;
      }

      if (httpStatus === 404) {
        redirectToExceptionPage('404');
        return;
      }

      if (typeof httpStatus === 'number' && httpStatus >= 500) {
        redirectToExceptionPage('500');
        return;
      }

      showErrorMsg(request.state, message);
    }
  }
);

/**
 * 作用：演示/第二基地址请求实例（成功判断与数据结构不同于主接口）。
 * @remarks 使用 demo 服务 baseURL 与独立 token 头格式
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
