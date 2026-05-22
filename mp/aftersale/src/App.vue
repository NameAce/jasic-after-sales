<script setup lang="ts">
/**
 * 售后客户端小程序（报修、工单、地址）根组件。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { onLaunch } from '@dcloudio/uni-app'
  onLaunch(() => {
    uni.requirePrivacyAuthorize({})
  })
</script>
<style lang="scss">
  @use '@/styles/index.scss';
  @use '@/uni.scss';

  page {
    background-color: $bg-page;
    min-height: 100vh;
    font-family: 'SimHei', '黑体', 'Heiti SC', sans-serif;
  }

  /* 纵向块级流式布局（勿用含 justify-content:center 的 flex-column，避免整页垂直居中） */
  .page-index {
    @include flex-column-gap;
    min-height: 100vh;
    background-color: $bg-page;
  }

  .page-padding {
    padding: 0 $space-lg env(safe-area-inset-bottom);
    box-sizing: border-box;
  }

  .page-padding-no-safe {
    padding-bottom: 0;
  }
</style>
