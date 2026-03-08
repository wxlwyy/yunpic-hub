<template>
  <div class="space-size-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 5 }">
      <div v-if="dataList.length === 0" class="empty-box">
        <a-empty description="暂无大小分段数据" />
      </div>
      <v-chart v-else :option="options" style="height: 350px; max-width: 100%" />
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { getSpaceSizeAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import 'echarts'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string // 统一使用 string
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.SpaceSizeAnalyzeResponse[]>([])
const loading = ref(true)

// 复用大厂调色盘
const COLORS = ['#1890ff', '#36cfc9', '#52c41a', '#722ed1', '#faad14', '#ff4d4f']

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceSizeAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取大小分析数据失败')
  }
  loading.value = false
}

watchEffect(() => {
  fetchData()
})

const options = computed(() => {
  const pieData = dataList.value.map((item) => ({
    name: item.sizeRange,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      padding: [10, 16],
      borderRadius: 8,
      formatter: (params: any) => {
        return `<div style="color:#8c8c8c;font-size:12px">${params.seriesName}</div>
                <div style="font-weight:bold;margin-top:4px">
                  <span style="color:${params.color}">●</span> ${params.name}:
                  <span style="font-size:16px">${params.value}</span> 张
                  <span style="color:#bfbfbf">(${params.percent}%)</span>
                </div>`
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);'
    },
    legend: {
      bottom: '0',
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#8c8c8c' }
    },
    series: [
      {
        name: '资源大小分布',
        type: 'pie',
        // 🚀 核心：设置内外径变成环形图
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        // 🚀 关键：给扇区加圆角
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        // 🚀 悬停时的“放大”中心文字特效
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold',
            formatter: '{b}\n{d}%'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          }
        },
        color: COLORS,
        data: pieData,
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
