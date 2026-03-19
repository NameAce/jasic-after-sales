<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default { name: 'Page404' }
</script>

<style lang="scss" scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
  .error-content {
    text-align: center;
    h1 { font-size: 80px; color: #909399; margin: 0 0 16px; }
    p { font-size: 16px; color: #606266; margin-bottom: 24px; }
  }
}
</style>
