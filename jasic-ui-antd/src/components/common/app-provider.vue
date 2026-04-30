<script setup lang="ts">
/**
 * 全局 Ant Design Vue `App` 包裹层：挂载 ContextHolder，将 message/modal/notification 挂到 window 供非组件内调用。
 */
import { createTextVNode, defineComponent } from 'vue';
import { App } from 'ant-design-vue';

defineOptions({
  name: 'AppProvider'
});

// 在子树内调用 App.useApp()，一次性注册全局静态方法
const ContextHolder = defineComponent({
  name: 'ContextHolder',
  setup() {
    const { message, modal, notification } = App.useApp();

    /** 将 ant-design-vue 实例挂到 window，与项目内 window.$message 等约定一致 */
    function register() {
      window.$message = message;
      window.$modal = modal;
      window.$notification = notification;
    }

    register();

    return () => createTextVNode();
  }
});
</script>

<template>
  <App class="h-full">
    <ContextHolder />
    <slot></slot>
  </App>
</template>

<style scoped></style>
