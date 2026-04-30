/**
 * 本地 SVG 图标：扫描 `src/assets/svg-icon` 获取无扩展名 key 列表，供菜单/表单图标选择等使用。
 *
 * @returns {string[]} 图标名数组
 */
export function getLocalIcons() {
  const svgIcons = import.meta.glob('/src/assets/svg-icon/*.svg');

  const keys = Object.keys(svgIcons)
    .map(item => item.split('/').at(-1)?.replace('.svg', '') || '')
    .filter(Boolean);

  return keys;
}
