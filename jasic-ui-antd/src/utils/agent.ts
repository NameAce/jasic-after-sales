/**
 * 运行环境检测：根据 UA 判断是否桌面端（未命中常见移动设备标识则视为 PC）。
 *
 * @returns {boolean} 为 PC 返回 true
 */
export function isPC() {
  const agents = ['Android', 'iPhone', 'webOS', 'BlackBerry', 'SymbianOS', 'Windows Phone', 'iPad', 'iPod'];

  const isMobile = agents.some(agent => window.navigator.userAgent.includes(agent));

  return !isMobile;
}
