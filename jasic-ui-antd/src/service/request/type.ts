/**
 * 请求实例运行时状态：与 `createFlatRequest` 的 state 挂载，供刷新 token、错误 toast 去重、登录过期弹窗防重使用。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface RequestInstanceState {
  /** 是否正在进行 token 刷新的 Promise，用于合并并发过期请求
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  refreshTokenFn: Promise<boolean> | null;
  /** 当前正在展示或队列中的错误 toast 文案，用于去重（勿与登录过期弹窗共用，否则定时清空会叠多层遮罩）
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  toastErrMsgStack: string[];
  /** 已展示「登录过期」类 Modal 的业务码，用于防重复弹窗
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  modalLogoutShownCodes: string[];
}
