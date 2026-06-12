<template>
  <div class="space-category-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 5 }">
      <div v-if="dataList.length === 0" class="empty-box">
        <a-empty description="暂无分类数据" />
      </div>
      <v-chart v-else :option="options" style="height: 350px; max-width: 100%" />
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { getSpaceCategoryAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import * as echarts from 'echarts' // 🚀 引入核心包用于创建渐变色

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.SpaceCategoryAnalyzeResponse[]>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  const res = await getSpaceCategoryAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取分类数据失败')
  }
  loading.value = false
}

watchEffect(() => {
  fetchData()
})

const options = computed(() => {
  // 🚀 核心改动：人性化处理分类名称
  const categories = dataList.value.map((item) => item.category || '未分类')
  const countData = dataList.value.map((item) => item.totalCount)
  const sizeData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2))

  return {
    grid: {
      top: '15%',
      left: '3%',
      right: '3%',
      bottom: '10%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      // 🚀 增强提示框，带上单位
      formatter: (params: any) => {
        let res = `<div style="font-weight:bold;margin-bottom:4px">${params[0].name}</div>`
        params.forEach((item: any) => {
          const unit = item.seriesName === '图片总大小' ? ' MB' : ' 张'
          res += `<div style="display:flex;justify-content:space-between;gap:20px">
                    <span>${item.marker}${item.seriesName}</span>
                    <span style="font-weight:bold">${item.value}${unit}</span>
                  </div>`
        })
        return res
      }
    },
    legend: {
      data: ['图片数量', '图片总大小'],
      top: '0',
      itemWidth: 10,
      itemHeight: 10
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisTick: { show: false },
      axisLabel: { color: '#8c8c8c' }
    },
    yAxis: [
      {
        type: 'value',
        name: '数量 (张)',
        nameTextStyle: { color: '#8c8c8c', padding: [0, 0, 0, -30] },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } }
      },
      {
        type: 'value',
        name: '大小 (MB)',
        position: 'right',
        nameTextStyle: { color: '#8c8c8c', padding: [0, -30, 0, 0] },
        splitLine: { show: false } // 右轴不显示网格线，防止混乱
      }
    ],
    series: [
      {
        name: '图片数量',
        type: 'bar',
        barWidth: '25%',
        // 🚀 渐变色：蓝色系
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#1890ff' },
            { offset: 1, color: '#69c0ff' }
          ])
        },
        data: countData,
        yAxisIndex: 0
      },
      {
        name: '图片总大小',
        type: 'bar',
        barWidth: '25%',
        // 🚀 渐变色：绿色系
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#52c41a' },
            { offset: 1, color: '#b7eb8f' }
          ])
        },
        data: sizeData,
        yAxisIndex: 1
      }
    ]
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
