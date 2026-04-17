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
