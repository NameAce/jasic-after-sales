/**
 * 菜单/路由图标：将 `@sa/hooks` 的 Svg 渲染与本项目 `SvgIcon` 组件绑定。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { useSvgIconRender } from '@sa/hooks';
import SvgIcon from '@/components/custom/svg-icon.vue';

/**
 * 作用：基于 `@sa/hooks` 的 `useSvgIconRender`，绑定本项目 SvgIcon 组件。
 * @returns {{ SvgIconVNode }} 渲染函数
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useSvgIcon() {
  const { SvgIconVNode } = useSvgIconRender(SvgIcon);

  return {
    SvgIconVNode
  };
}
