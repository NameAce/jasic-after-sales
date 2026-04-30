export interface RequestInstanceState {
  /** 是否正在进行 token 刷新的 Promise，用于合并并发过期请求 */
  refreshTokenFn: Promise<boolean> | null;
  /** 当前正在展示或队列中的错误消息，用于避免重复 toast */
  errMsgStack: string[];
}
