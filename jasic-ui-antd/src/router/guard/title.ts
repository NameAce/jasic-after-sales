/**
 * 文档标题守卫：根据路由 meta 的 i18nKey 或 title 更新浏览器 tab 标题。
 */
import type { Router } from 'vue-router';
import { useTitle } from '@vueuse/core';
import { $t } from '@/locales';

/**
 * 作用：在路由切换后根据 meta 的 i18nKey/title 设置浏览器标题。
 * @param router Vue Router 实例
 * @returns {void}
 */
export function createDocumentTitleGuard(router: Router) {
  router.afterEach(to => {
    const { i18nKey, title } = to.meta;

    const documentTitle = i18nKey ? $t(i18nKey) : title;

    useTitle(documentTitle);
  });
}
