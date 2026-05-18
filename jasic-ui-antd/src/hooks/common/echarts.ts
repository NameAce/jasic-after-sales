/**
 * ECharts 按需引入与 DOM 尺寸联动：封装折线/柱状/饼图等常用配置的 composable。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { computed, effectScope, nextTick, onScopeDispose, ref, watch } from 'vue';
import { useElementSize } from '@vueuse/core';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import type { BarSeriesOption, LineSeriesOption, PieSeriesOption } from 'echarts/charts';
import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  TransformComponent
} from 'echarts/components';
import type {
  DatasetComponentOption,
  GridComponentOption,
  LegendComponentOption,
  TitleComponentOption,
  ToolboxComponentOption,
  TooltipComponentOption
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import { useThemeStore } from '@/store/modules/theme';

export type ECOption = echarts.ComposeOption<
  | BarSeriesOption
  | LineSeriesOption
  | PieSeriesOption
  | TitleComponentOption
  | LegendComponentOption
  | TooltipComponentOption
  | GridComponentOption
  | ToolboxComponentOption
  | DatasetComponentOption
>;

echarts.use([
  TitleComponent,
  LegendComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  ToolboxComponent,
  BarChart,
  LineChart,
  PieChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
]);

interface ChartHooks {
  onRender?: (chart: echarts.ECharts) => void | Promise<void>;
  onUpdated?: (chart: echarts.ECharts) => void | Promise<void>;
  onDestroy?: (chart: echarts.ECharts) => void | Promise<void>;
}

/**
 * 作用：创建 ECharts 实例并响应容器尺寸与主题切换；对外暴露 `domRef` 与更新 options 方法。
 * @param optionsFactory 返回初始 option 的工厂函数
 * @param hooks 渲染/更新/销毁钩子
 * @returns {{ domRef; updateOptions; setOptions }}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useEcharts<T extends ECOption>(optionsFactory: () => T, hooks: ChartHooks = {}) {
  const scope = effectScope();

  const themeStore = useThemeStore();
  // 来自主题 Store 的暗色开关，驱动 chart 主题与 loading 配色
  const darkMode = computed(() => themeStore.darkMode);

  const domRef = ref<HTMLElement | null>(null);
  const initialSize = { width: 0, height: 0 };
  const { width, height } = useElementSize(domRef, initialSize);

  let chart: echarts.ECharts | null = null;
  const chartOptions: T = optionsFactory();

  const {
    onRender = instance => {
      const textColor = darkMode.value ? 'rgb(224, 224, 224)' : 'rgb(31, 31, 31)';
      const maskColor = darkMode.value ? 'rgba(0, 0, 0, 0.4)' : 'rgba(255, 255, 255, 0.8)';

      instance.showLoading({
        color: themeStore.themeColor,
        textColor,
        fontSize: 14,
        maskColor
      });
    },
    onUpdated = instance => {
      instance.hideLoading();
    },
    onDestroy
  } = hooks;

  /**
   * 作用：判断当前 DOM 与尺寸是否允许初始化图表。
   * @returns {boolean}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function canRender() {
    return domRef.value && initialSize.width > 0 && initialSize.height > 0;
  }

  /** 是否已完成 init 且存在实例
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function isRendered() {
    return Boolean(domRef.value && chart);
  }

  /**
   * 作用：在已渲染状态下合并并应用新 option，可传入回调二次加工。
   * @param callback 接收当前 opts 与工厂，返回待 setOption 的对象
   * @returns {Promise<void>}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function updateOptions(callback: (opts: T, optsFactory: () => T) => ECOption = () => chartOptions) {
    if (!isRendered()) return;

    const updatedOpts = callback(chartOptions, optionsFactory);

    Object.assign(chartOptions, updatedOpts);

    if (isRendered()) {
      chart?.clear();
    }

    chart?.setOption({ ...updatedOpts, backgroundColor: 'transparent' });

    await onUpdated?.(chart!);
  }

  function setOptions(options: T) {
    chart?.setOption(options);
  }

  /** 首屏或主题切换后创建实例并 setOption
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function render() {
    if (!isRendered()) {
      const chartTheme = darkMode.value ? 'dark' : 'light';

      await nextTick();

      chart = echarts.init(domRef.value, chartTheme);

      chart.setOption({ ...chartOptions, backgroundColor: 'transparent' });

      await onRender?.(chart);
    }
  }

  /** 调用 echarts resize
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function resize() {
    chart?.resize();
  }

  /** dispose 实例并清空引用
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function destroy() {
    if (!chart) return;

    await onDestroy?.(chart);
    chart?.dispose();
    chart = null;
  }

  /** 暗色变化时销毁并重建以切换 echarts 内置主题
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function changeTheme() {
    await destroy();
    await render();
    await onUpdated?.(chart!);
  }

  /**
   * 作用：在容器宽高变化时更新内部 initialSize 并 render/resize/destroy。
   * @param w 宽
   * @param h 高
   * @returns {Promise<void>}
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  async function renderChartBySize(w: number, h: number) {
    initialSize.width = w;
    initialSize.height = h;

    // size is abnormal, destroy chart
    if (!canRender()) {
      await destroy();

      return;
    }

    // resize chart
    if (isRendered()) {
      resize();
    }

    // render chart
    await render();
  }

  scope.run(() => {
    // 容器尺寸变化：重算是否可渲染并 resize 或重建
    watch([width, height], ([newWidth, newHeight]) => {
      renderChartBySize(newWidth, newHeight);
    });

    // 暗色模式切换：换主题重建图表
    watch(darkMode, () => {
      changeTheme();
    });
  });

  onScopeDispose(() => {
    destroy();
    scope.stop();
  });

  return {
    domRef,
    updateOptions,
    setOptions
  };
}
