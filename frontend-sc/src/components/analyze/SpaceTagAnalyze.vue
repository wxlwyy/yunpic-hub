<template>
  <div class="space-tag-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 5 }">
      <div v-if="dataList.length === 0" class="empty-box">
        <a-empty description="暂无标签数据" />
      </div>
      <v-chart v-else :option="options" style="height: 350px; max-width: 100%" />
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { getSpaceTagAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import 'echarts'
import 'echarts-wordcloud'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.SpaceTagAnalyzeResponse[]>([])
const loading = ref(true)

// 🚀 大厂级调色盘：预设一组高级色，避免随机出“土色”
const PRESET_COLORS = [
  '#1890ff', '#36cfc9', '#52c41a', '#722ed1', '#faad14',
  '#f5222d', '#13c2c2', '#eb2f96', '#2f54eb', '#a0d911'
]

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceTagAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取标签数据失败')
  }
  loading.value = false
}

watchEffect(() => {
  fetchData()
})

const options = computed(() => {
  const tagData = dataList.value.map((item) => ({
    name: item.tag,
    value: item.count,
  }))

  return {
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderRadius: 8,
      padding: [8, 12],
      textStyle: { color: '#595959' },
      formatter: (params: any) => {
        return `<div style="font-weight:bold;color:${params.color}">${params.name}</div>
                <div style="margin-top:4px">出现频率：<span style="font-weight:bold">${params.value}</span> 次</div>`
      },
      extraCssText: 'box-shadow: 0 2px 8px rgba(0,0,0,0.1);'
    },
    series: [
      {
        type: 'wordCloud',
        shape: 'circle',
        left: 'center',
        top: 'center',
        width: '90%',
        height: '90%',
        right: null,
        bottom: null,
        sizeRange: [14, 50], // 🚀 增大字体跨度，突出重点
        rotationRange: [0, 0], // 🚀 建议设为 0，因为横向阅读最符合人类习惯
        rotationStep: 45,
        gridSize: 12,
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {
          fontFamily: 'PingFang SC, Microsoft YaHei',
          fontWeight: 600,
          // 🚀 核心改动：从预设颜色库中随机挑选
          color: () => PRESET_COLORS[Math.floor(Math.random() * PRESET_COLORS.length)]
        },
        // 🚀 悬停效果：加阴影和放大
        emphasis: {
          textStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.15)',
            fontSize: 55
          }
        },
        data: tagData,
      },
    ],
  }
})
</script>

<style scoped>
.empty-box {
  height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
