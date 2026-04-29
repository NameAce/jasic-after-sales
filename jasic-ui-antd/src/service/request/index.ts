import { BACKEND_ERROR_CODE, createFlatRequest, createRequest } from '@sa/axios';
import { router } from '@/router';
import { useAuthStore } from '@/store/modules/auth';
import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { getAuthorization, showErrorMsg } from './shared';
import type { RequestInstanceState } from './type';

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL, otherBaseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

function parseCodeList(raw: string | undefined) {
  return (raw?.split(',') || []).map(c => c.trim()).filter(Boolean);
}

type ExceptionRouteName = '403' | '404' | '500';

function redirectToExceptionPage(routeName: ExceptionRouteName) {
  const currentRouteName = String(router.currentRoute.value.name || '');
  if (currentRouteName === routeName) return;

  router.push({ name: routeName }).catch(() => {});
}

function isOnHomeRoute() {
  const current = router.currentRoute.value;
  const routeName = String(current.name || '');
  const routePath = String(current.path || '');

  return routeName === 'root' || routeName === 'home' || routePath === '/' || routePath.startsWith('/home');
}

const defaultRequestHeaders: Record<string, string> = {};
const apifoxToken = import.meta.env.VITE_APIFOX_TOKEN;
if (apifoxToken) {
  defaultRequestHeaders.apifoxToken = apifoxToken;
}

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
