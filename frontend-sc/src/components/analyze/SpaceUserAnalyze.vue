<template>
  <div class="space-user-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 6 }">
      <div v-if="dataList.length === 0" class="empty-box">
        <a-empty description="暂无上传趋势数据" />
      </div>
      <v-chart v-else :option="options" style="height: 400px; max-width: 100%" />
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { getSpaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string
  timeDimension?: string // 外部可控维度
  userId?: string // 外部可控用户
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.SpaceUserAnalyzeResponse[]>([])
const loading = ref(true)

// 内部控制的时间维度和用户 ID（如果父组件没传，则用内部的）
const timeDimension = ref(props.timeDimension || 'day')
const userId = ref(props.userId)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    timeDimension: timeDimension.value,
    userId: userId.value,
  })
  if (res.data.code === 0) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取上传趋势失败')
  }
  loading.value = false
}

watchEffect(() => {
  fetchData()
})

const options = computed(() => {
  const periods = dataList.value.map((item) => item.period)
  const counts = dataList.value.map((item) => item.count)

  return {
    grid: {
      top: '12%',
      left: '3%',
      right: '4%',
      bottom: '15%', // 留出空间给 DataZoom
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderRadius: 8,
      boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
      axisPointer: {
        lineStyle: { color: '#1890ff', width: 2, type: 'dashed' }
      }
    },
    // 🚀 增加缩放控件，对大数据量友好
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        start: 0,
        end: 100,
        height: 20,
        bottom: 10,
        handleIcon: 'path://M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
        handleSize: '80%',
        brushSelect: false,
        borderColor: '#f0f0f0',
        fillerColor: 'rgba(24, 144, 255, 0.1)',
        labelFormatter: '' // 隐藏冗余文字
      }
    ],
    xAxis: {
      type: 'category',
      data: periods,
      boundaryGap: false, // 🚀 关键：让线条紧贴左右边界，更开阔
      axisLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#8c8c8c' },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '上传数量 (张)',
      nameTextStyle: { color: '#8c8c8c', padding: [0, 0, 10, 0] },
      splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      axisLabel: { color: '#8c8c8c' }
    },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: counts,
        smooth: 0.4, // 🚀 0.4 的顺滑度最自然，像波浪
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#1890ff', borderWidth: 2, borderColor: '#fff' },
        lineStyle: { width: 4, color: '#1890ff' }, // 线条加粗
        // 🚀 核心：面积填充渐变
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24, 144, 255, 0.4)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0)' }
          ])
        },
        emphasis: {
          focus: 'series',
          itemStyle: {
            scale: 1.5,
            shadowBlur: 10,
            shadowColor: 'rgba(24, 144, 255, 0.5)'
          }
        }
      }
    ]
  }
})
</script>

<style scoped>
.empty-box {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
