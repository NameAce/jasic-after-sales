/**
 * 服务地址配置：解析主服务 baseURL、代理模式与其它微服务地址映射（JSON5）。
 */
import json5 from 'json5';

/**
 * 作用：根据 Vite 环境变量解析主服务与其它微服务 baseURL（JSON5 解析其它地址映射）。
 * @param env 当前 Vite 注入的环境对象
 * @returns {App.Service.ServiceConfig} 含代理模式路径与服务列表的配置
 */
export function createServiceConfig(env: Env.ViteEnv) {
  const { VITE_SERVICE_BASE_URL, VITE_OTHER_SERVICE_BASE_URL } = env;

  let other = {} as Record<App.Service.OtherBaseURLKey, string>;
  try {
    other = json5.parse(VITE_OTHER_SERVICE_BASE_URL);
  } catch {
    // eslint-disable-next-line no-console
    console.error('VITE_OTHER_SERVICE_BASE_URL is not a valid json5 string');
  }

  const httpConfig: App.Service.SimpleServiceConfig = {
    baseURL: VITE_SERVICE_BASE_URL,
    other
  };

  const otherHttpKeys = Object.keys(httpConfig.other) as App.Service.OtherBaseURLKey[];

  const otherConfig: App.Service.OtherServiceConfigItem[] = otherHttpKeys.map(key => {
    return {
      key,
      baseURL: httpConfig.other[key],
      proxyPattern: createProxyPattern(key)
    };
  });

  const config: App.Service.ServiceConfig = {
    baseURL: httpConfig.baseURL,
    proxyPattern: createProxyPattern(),
    other: otherConfig
  };

  return config;
}

/**
 * 作用：得到实际请求使用的 baseURL（开发代理模式下为 `/api` 或 `/proxy-*`）。
 * @param env 当前 Vite 环境
 * @param isProxy 是否走开发代理
 * @returns {{ baseURL: string; otherBaseURL: Record<string, string> }} 主与其它服务地址
 */
export function getServiceBaseURL(env: Env.ViteEnv, isProxy: boolean) {
  const { baseURL, other } = createServiceConfig(env);

  const otherBaseURL = {} as Record<App.Service.OtherBaseURLKey, string>;

  other.forEach(item => {
    otherBaseURL[item.key] = isProxy ? item.proxyPattern : item.baseURL;
  });

  return {
    baseURL: isProxy ? createProxyPattern() : baseURL,
    otherBaseURL
  };
}

/**
 * 作用：生成 Vite 代理路径片段（默认 `/api`，指定 key 时为 `/proxy-{key}`）。
 * @param key 其它服务键名，缺省为主服务
 * @returns {string} 代理路径前缀
 */
function createProxyPattern(key?: App.Service.OtherBaseURLKey) {
  if (!key) {
    return '/api';
  }

  return `/proxy-${key}`;
}
