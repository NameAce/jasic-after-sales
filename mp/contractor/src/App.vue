<script setup lang="ts">
  import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
  onLaunch(() => {
    console.log('App Launch')
    // 请求隐私协议
    uni.requirePrivacyAuthorize({
      success() {
        console.log('用户已同意隐私协议')
      }
    })
  })
  onShow(() => {
    console.log('App Show')
  })
  onHide(() => {
    console.log('App Hide')
  })
</script>
<style lang="scss">
  @use '@/styles/index.scss';
  @use '@/uni.scss';

  /* 全局页面底部安全距离 */
  page {
    // 底部安全距离（兼容旧版本 iOS）
    background-color: $surface-page;
    min-height: 100vh;
    font-family: 'SimHei', '黑体', 'Heiti SC', sans-serif;
  }

  .page-index {
    // 组件之间的间隔
    @include flex-column;
    gap: $space-lg;
    padding: 0 $space-lg env(safe-area-inset-bottom);
    box-sizing: border-box;
  }
</style>
