<script setup lang="ts">

import BasicLayout from '@/layouts/BasicLayout.vue'
import { healthUsingGet } from '@/api/mainController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

healthUsingGet().then((res) => {
  console.log(res)
})

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()
</script>

<template>
  <div id="app">
    <BasicLayout/>
  </div>

</template>

<style scoped>

</style>
