/**
 * Iconify：可选配置自建 API 地址，用于内网或离线图标资源。
 */
import { addAPIProvider } from '@iconify/vue';

/**
 * 作用：若配置了自建 Iconify 资源地址，则注册为离线/私有图标 API。
 * @returns {void}
 */
export function setupIconifyOffline() {
  const { VITE_ICONIFY_URL } = import.meta.env;

  if (VITE_ICONIFY_URL) {
    addAPIProvider('', { resources: [VITE_ICONIFY_URL] });
  }
}
