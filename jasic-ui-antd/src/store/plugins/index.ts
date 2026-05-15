/**
 * Pinia 插件：为枚举内的 setup Store 注入可恢复到初始快照的 `$reset`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { PiniaPluginContext } from 'pinia';
import { jsonClone } from '@sa/utils';
import { SetupStoreId } from '@/enum';

/**
 * Pinia 插件：为 setup 语法的 store 提供可恢复到初始快照的 `$reset`。
 *
 * @param context - Pinia 插件上下文
 * @returns {void} 无返回值
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function resetSetupStore(context: PiniaPluginContext) {
  const setupSyntaxIds = Object.values(SetupStoreId) as string[];

  if (setupSyntaxIds.includes(context.store.$id)) {
    const { $state } = context.store;

    const defaultStore = jsonClone($state);

    context.store.$reset = () => {
      context.store.$patch(defaultStore);
    };
  }
}
