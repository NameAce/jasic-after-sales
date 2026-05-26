import { ref, nextTick, type Ref } from 'vue'

/**
 * 收起 scroll-view 下拉刷新动画
 *
 * 微信约定（mp-weixin / iOS 实际验证）：
 * - refresher-triggered 必须经历 true → false 才会触发 @refresherrestore 并真正收起动画；
 * - 同一同步任务内连续多次写同一个 ref，Vue 调度 setData 时会做 diff 合并，
 *   端可能收不到中间的"true"，因此 true 与 false 之间必须 await nextTick 拆开；
 * - 若当前 triggered 已经是 false，再写 false 是空操作，不会引起视觉变化；
 *   仅当本路径需要"通过翻转引发端复位"时，才主动 false → true → false。
 *
 * 与旧实现的差异：
 * - 不再使用模块级共享的 generation；不同 scroll-view 的状态彼此独立，
 *   避免出现「同一页两个 refresher 互相作废对方的兜底写入」导致动画卡住的问题；
 * - 不再使用 300ms setTimeout 兜底；仅用 nextTick 保证 setData 切片，
 *   避免延时拉长 race 窗口（旧实现中 stop 与 reset 重叠会令动画无法关闭）。
 *
 * @param triggered  与 :refresher-triggered 绑定的响应式开关
 * @param shouldAbort 末段执行前的兜底检查；外部已 reset/restore 时返回 true，本函数立即中止后续写入
 */
async function closeRefresherAnimation(
  triggered: Ref<boolean>,
  shouldAbort: () => boolean
) {
  // 正常路径：executeRefresh 在 finally 调用时 triggered=true，直接写 false 即可让端关闭动画
  if (triggered.value) {
    triggered.value = false
    await nextTick()
    return
  }

  // 兜底路径：triggered 已被外部置为 false，但视觉动画可能仍残留，
  // 需要主动 true → nextTick → false 引发端 restore；
  // 注意：先 await nextTick 让端确认当前为 false 的状态，再翻转 true 才能被识别
  await nextTick()
  if (shouldAbort()) return
  triggered.value = true
  await nextTick()
  if (shouldAbort()) return
  triggered.value = false
  await nextTick()
}

/** useScrollRefresher 对外返回值类型，集中描述所有可消费 API，避免调用方依赖具体实现细节 */
export interface ScrollRefresherApi {
  /** 与 scroll-view 的 :refresher-triggered 绑定 */
  refresherTriggered: Ref<boolean>
  /** 绑定到 @refresherrefresh：用户下拉触发刷新 */
  onRefresherRefresh: () => Promise<void>
  /** 绑定到 @refresherrestore：端复位动画后回调，用于本地状态收尾 */
  onRefresherRestore: () => void
  /** 在业务事件中主动以 refresher 动画形式触发一次刷新（含可覆盖的执行逻辑） */
  runWithRefresherFeedback: (overrideRun?: () => Promise<void>) => Promise<void>
  /** 强制复位：Tab 切换 / v-if 视图切换 / 页面 onHide 时调用，避免动画残留挡住 Tab */
  resetScrollRefresher: () => void
}

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 *
 * 微信端约束（不直接走文档可能踩坑）：
 * - 用户下拉只触发 @refresherrefresh，端不会自动把 refresher-triggered 置 true，必须业务侧手动置 true；
 * - 置 true 后端可能再次触发一轮 @refresherrefresh，必须做并发去重；
 * - refresher-triggered 仅有 true → false 才会收起动画并触发 @refresherrestore；
 *   多次同步赋值会被 setData 合并，须用 await nextTick 在中间隔开。
 *
 * 整体设计：
 * - 单 generation 收敛：每次新一轮刷新或外部 reset 都自增 generation，
 *   所有异步收尾路径都判断 generation 是否仍属本轮，避免越界回写状态；
 * - 不做"自动补跑"：上一轮还在跑、又收到下拉，直接忽略本次，
 *   避免「reset/切 Tab 后又被自动续跑一次」的歧义；
 * - resetScrollRefresher 同步把状态拉死，让一级/二级 Tab 切换立即可响应；
 * - onRefresherRestore 与 reset 等价处理：基础库已经收尾，本地只清状态，
 *   不再主动做 true→false 翻转，避免与端二次交互冲突；
 * - finally 中不再无条件 close 动画：只有"本轮未被抢占"才走收尾，
 *   否则 reset 的清零结果说了算，杜绝 finally 把 triggered 反向置回 true。
 *
 * @param run 默认刷新逻辑（@refresherrefresh 与 runWithRefresherFeedback 无入参时调用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-26
 */
export function useScrollRefresher(run: () => Promise<void>): ScrollRefresherApi {
  // 与 :refresher-triggered 绑定的开关，必须由本 hook 全权管理，外部不要直接写入
  const refresherTriggered = ref(false)
  // 本轮刷新是否在进行中，仅用于去重重复触发（不再用于自动补跑）
  const refreshInFlight = ref(false)
  // 单调递增的轮次号；reset / 新一轮 refresh 都会 +1，作为所有异步尾部的兜底判据
  let generation = 0

  /**
   * 强制复位 refresher 与并发锁
   *
   * 适用场景：
   * - 一级 / 二级 Tab 切换；
   * - v-if 切换两套不同业务视图（如总部「总部处理 / 网点工单」）；
   * - 页面 onHide / 离开列表前兜底。
   *
   * 处理逻辑（关键顺序不可调换）：
   * 1. generation++：让 in-flight 的 executeRefresh 在 finally 看见 generation 已变，跳过尾部回写，
   *    避免出现「reset 之后被旧任务把 triggered 重新置回 true」的回弹；
   * 2. refreshInFlight 立即清零：保证 Tab 点击 / 新一轮刷新可即时响应，不被旧锁卡住；
   * 3. refresherTriggered 同步置 false：让 Vue 在下一帧 setData 推 false 给端，端收到后收起动画。
   *    若调用 reset 时 triggered 已经是 true（loading 转圈中），端会从 true → false 收到翻转信号，
   *    自然触发 @refresherrestore，由 onRefresherRestore 兜底确认（幂等，无副作用）。
   */
  const resetScrollRefresher = () => {
    generation++
    refreshInFlight.value = false
    refresherTriggered.value = false
  }

  /**
   * 基础库触发 @refresherrestore 时回调
   *
   * 此刻端已经把动画收起，本地只需清状态对齐即可；
   * 不再做 true → false 翻转，避免与端二次冲突触发额外的 @refresherrefresh。
   */
  const onRefresherRestore = () => {
    resetScrollRefresher()
  }

  /**
   * 执行一次刷新（下拉触发或业务主动调用）
   *
   * 并发与抢占处理：
   * - 若上一轮仍在跑：直接忽略本次，避免重复发请求 / 端被反复置 true；
   *   不再做"补跑"，由用户再次下拉或调用方显式调用决定要不要再来一次；
   * - 申请本轮 generation，整个生命周期都用它作为身份证；
   * - 若期间 reset / restore 触发（generation 已变），finally 不再回写状态，
   *   彻底交给 reset 的清零结果，避免覆盖。
   *
   * 异常处理：
   * - run() 抛错不向外抛，仅打印日志；finally 仍走完关闭流程，
   *   避免业务异常导致 refresher 动画永远不消失。
   */
  const executeRefresh = async (overrideRun?: () => Promise<void>) => {
    if (refreshInFlight.value) return

    const myGeneration = ++generation
    refreshInFlight.value = true
    // 端在下拉时不会自动把 triggered 置 true，必须业务侧主动开启 loading
    if (!refresherTriggered.value) {
      refresherTriggered.value = true
    }

    const shouldAbort = () => myGeneration !== generation

    try {
      await (overrideRun ?? run)()
    } catch (e) {
      // 业务异常不阻塞 refresher 收尾，避免动画永久残留
      console.error('[useScrollRefresher] refresh failed', e)
    } finally {
      // 抢占判定：若本轮被 reset / restore 抢占，所有状态已被对方清零，本路径不再回写
      if (!shouldAbort()) {
        refreshInFlight.value = false
        await closeRefresherAnimation(refresherTriggered, shouldAbort)
        // 双保险：close 内的 nextTick 可能让出 microtask 给抢占者，再确认一次最终状态
        if (!shouldAbort() && refresherTriggered.value) {
          refresherTriggered.value = false
        }
      }
    }
  }

  const onRefresherRefresh = () => executeRefresh()

  const runWithRefresherFeedback = (overrideRun?: () => Promise<void>) =>
    executeRefresh(overrideRun)

  return {
    refresherTriggered,
    onRefresherRefresh,
    onRefresherRestore,
    runWithRefresherFeedback,
    resetScrollRefresher
  }
}
