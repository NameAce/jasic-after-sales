import type { ProxyOptions } from 'vite';
import { createServiceConfig } from '../../src/utils/service';

const DEFAULT_PROXY_TARGET = 'http://127.0.0.1:8080';

/**
 * Set http proxy
 *
 * @param env - The current env
 * @param enable - If enable http proxy
 */
export function createViteProxy(env: Env.ViteEnv, enable: boolean) {
  const isEnableHttpProxy = enable && env.VITE_HTTP_PROXY === 'Y';

  if (!isEnableHttpProxy) return undefined;

  const { baseURL, proxyPattern, other } = createServiceConfig(env);

  const proxy: Record<string, ProxyOptions> = createProxyItem({ baseURL, proxyPattern }, env);

  other.forEach(item => {
    Object.assign(proxy, createProxyItem(item, env));
  });

  return proxy;
}

function createProxyItem(item: App.Service.ServiceConfigItem, env: Env.ViteEnv) {
  const proxy: Record<string, ProxyOptions> = {};

  proxy[item.proxyPattern] = {
    target: resolveProxyTarget(item.baseURL, env),
    changeOrigin: true,
    rewrite: path => path.replace(new RegExp(`^${item.proxyPattern}`), '')
  };

  return proxy;
}

function resolveProxyTarget(baseURL: string, env: Env.ViteEnv) {
  if (/^https?:\/\//i.test(baseURL)) {
    return baseURL;
  }

  const proxyTarget = (env.VITE_SERVICE_PROXY_TARGET || DEFAULT_PROXY_TARGET).replace(/\/$/, '');
  const normalizedBaseURL = baseURL.startsWith('/') ? baseURL : `/${baseURL}`;

  return `${proxyTarget}${normalizedBaseURL}`;
}
