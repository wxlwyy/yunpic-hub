<script setup lang="ts">

import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader/>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">智能协同云图库</a-layout-footer>
    </a-layout>
  </div>
</template>

<style scoped>
#basicLayout .header {
  background: white;
  margin-bottom: 16px;
  padding-inline: 20px;
}
#basicLayout .content {
  background: linear-gradient(to right, #fefefe, #fff);
  padding: 20px;
  margin-bottom: 35px;
}
#basicLayout .footer {
  background: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
}
</style>
