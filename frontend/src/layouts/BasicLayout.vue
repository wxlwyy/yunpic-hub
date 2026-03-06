<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header v-if="!isAuthPage" class="header">
        <GlobalHeader />
      </a-layout-header>

      <a-layout>
        <GlobalSider v-if="!isAuthPage" class="sider" />

        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>

      <a-layout-footer v-if="!isAuthPage" class="footer">
        智能协同云图库 · 视觉资产管理系统
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'

const route = useRoute()

// 判断当前是否为登录或注册页面
const isAuthPage = computed(() => {
  return ['/user/login', '/user/register'].includes(route.path)
})
</script>

<style scoped>
#basicLayout .header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px); /* 导航栏也加点毛玻璃，跟登录卡片呼应 */
  padding-inline: 24px;
  height: 64px;
  line-height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
  border-bottom: 1px solid #f0f0f0;
}

#basicLayout .content {
  background: #f8fafc; /* 更现代的淡蓝灰底色 */
  padding: 24px;
  /* 确保内容区域能撑满剩余高度 */
  min-height: calc(100vh - 64px - 70px);
}

#basicLayout .sider {
  background: #fff;
  border-right: 1px solid #f0f0f0;
}

#basicLayout .footer {
  background: #fff;
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  border-top: 1px solid #f0f0f0;
}

/* 针对登录注册页的特殊处理：去除所有边距，让背景铺满 */
:deep(.content) {
  padding: v-bind("isAuthPage ? '0' : '24px'") !important;
}
</style>
