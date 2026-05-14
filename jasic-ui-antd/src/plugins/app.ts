/**
 * 应用级插件：Vue 全局错误处理、生产环境构建版本检测与更新通知等。
 */
import { h } from 'vue';
import type { App } from 'vue';
import { Button } from 'ant-design-vue';
import { $t } from '@/locales';

/**
 * 作用：注册 Vue 全局 errorHandler，仅记录运行时错误；异常页仅由接口错误跳转进入。
 * @param app Vue 应用实例
 * @returns {void}
 */
export function setupAppErrorHandle(app: App) {
  app.config.errorHandler = (err, vm, info) => {
    // eslint-disable-next-line no-console
    console.error(err, vm, info);
  };
}

/**
 * 作用：生产环境下按间隔拉取 index.html 的 buildTime，若有新版本则弹出刷新提示（受 VITE_AUTOMATICALLY_DETECT_UPDATE 控制）。
 * @returns {void}
 */
export function setupAppVersionNotification() {
  // 检测新版本的时间间隔（毫秒）
  const UPDATE_CHECK_INTERVAL = 3 * 60 * 1000;

  const canAutoUpdateApp = import.meta.env.VITE_AUTOMATICALLY_DETECT_UPDATE === 'Y' && import.meta.env.PROD;
  if (!canAutoUpdateApp) return;

  let isShow = false;
  let updateInterval: ReturnType<typeof setInterval> | undefined;

  const checkForUpdates = async () => {
    if (isShow) return;

    const buildTime = await getHtmlBuildTime();

    // If build time hasn't changed, no update is needed
    if (buildTime === BUILD_TIME) {
      return;
    }

    isShow = true;

    const key = `open${Date.now()}`;

    window.$notification?.open({
      key,
      message: $t('system.updateTitle'),
      description: $t('system.updateContent'),
      btn() {
        return h('div', { style: { display: 'flex', justifyContent: 'end', gap: '12px', width: '325px' } }, [
          h(
            Button,
            {
              onClick() {
                window.$notification?.destroy(key);
                isShow = false;
              }
            },
            () => $t('system.updateCancel')
          ),
          h(
            Button,
            {
              type: 'primary',
              onClick() {
                location.reload();
              }
            },
            () => $t('system.updateConfirm')
          )
        ]);
      },
      onClose() {
        isShow = false;
      }
    });
  };

  const startUpdateInterval = () => {
    if (updateInterval) {
      clearInterval(updateInterval);
    }
    updateInterval = setInterval(checkForUpdates, UPDATE_CHECK_INTERVAL);
  };

  // If updates should be checked, set up the visibility change listener and start the update interval
  if (!isShow && document.visibilityState === 'visible') {
    // Check for updates when the document is visible
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        checkForUpdates();
        startUpdateInterval();
      }
    });

    // Start the update interval
    startUpdateInterval();
  }
}

/**
 * 作用：请求首页 HTML 并解析 meta buildTime，用于与当前构建时间比对。
 * @returns {Promise<string>} 构建时间字符串，解析失败为空串
 */
async function getHtmlBuildTime() {
  const baseUrl = import.meta.env.VITE_BASE_URL || '/';

  const res = await fetch(`${baseUrl}index.html?time=${Date.now()}`);

  const html = await res.text();

  const match = html.match(/<meta name="buildTime" content="(.*)">/);

  const buildTime = match?.[1] || '';

  return buildTime;
}
