<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header v-if="!isAuthPage" class="header">
        <GlobalHeader />
      </a-layout-header>

      <a-layout>
        <div v-if="!isAuthPage" class="sider-placeholder">
          <GlobalSider class="fixed-sider" />
        </div>

        <a-layout-content :class="['content', isAuthPage ? 'auth-content' : '']">
          <router-view />
        </a-layout-content>
      </a-layout>

      <a-layout-footer v-if="!isAuthPage" class="footer">
        智能协同云图库 · CloudPic ©2026
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
const isAuthPage = computed(() => ['/user/login', '/user/register'].includes(route.path))
</script>

<style scoped>
/* 顶部导航固定 */
#basicLayout .header {
  position: fixed;
  top: 0;
  z-index: 1001;
  width: 100%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  padding-inline: 24px;
  height: 64px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 侧边栏固定逻辑 */
.sider-placeholder {
  width: 220px; /* 必须和侧边栏宽度一致 */
  flex: 0 0 220px;
}

.fixed-sider {
  position: fixed;
  top: 64px; /* 避开 Header */
  left: 0;
  width: 220px;
  height: calc(100vh - 64px);
  z-index: 100;
  background: #fff;
  border-right: 1px solid rgba(0, 0, 0, 0.05);
}

/* 内容区背景优化 */
#basicLayout .content {
  margin-top: 64px; /* 避开固定 Header */
  padding: 28px;
  background: #f8fafc; /* 高级白，让图片卡片更立体 */
  min-height: calc(100vh - 64px);
}

/* 登录注册页不需要边距 */
.auth-content {
  margin-top: 0 !important;
  padding: 0 !important;
  background: transparent !important;
}

#basicLayout .footer {
  background: #f8fafc;
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  /* 只有在非登录页才增加左边距，避开固定侧边栏 */
  margin-left: v-bind("isAuthPage ? '0' : '220px'");
}
</style>
