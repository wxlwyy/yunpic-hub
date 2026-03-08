<template>
  <div class="space-rank-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 6 }">
      <div v-if="dataList.length === 0" class="empty-box">
        <a-empty description="暂无空间排行数据" />
      </div>

      <div v-else class="rank-container">
        <div v-for="(item, index) in dataList" :key="item.id" class="rank-row" @click="handleRowClick(item.id)">
          <div class="rank-badge">
            <span v-if="index === 0" class="medal">🥇</span>
            <span v-else-if="index === 1" class="medal">🥈</span>
            <span v-else-if="index === 2" class="medal">🥉</span>
            <span v-else class="rank-num">{{ index + 1 }}</span>
          </div>

          <div class="rank-main">
            <div class="rank-info">
              <div class="name-group">
                <span class="space-name" :title="item.spaceName">{{ item.spaceName }}</span>
                <a-tag v-if="index === 0" color="red" class="top-tag">榜首</a-tag>
              </div>
              <div class="data-group">
                <span class="size-label">已用容量：</span>
                <span class="size-value">{{ formatSize(item.totalSize) }}</span>
              </div>
            </div>

            <div class="progress-section">
              <a-progress
                :percent="calculateRelativePercent(item.totalSize)"
                :stroke-color="getProgressBarColor(index)"
                :show-info="false"
                stroke-width="8"
                class="custom-progress"
              />
            </div>
          </div>
          <div class="rank-action">
            <right-outlined class="arrow-icon" />
          </div>
        </div>
      </div>
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { getSpaceRankAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '../../utils'
import router from "@/router";

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string // 改为 string 兼容 19 位长 ID
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const dataList = ref<API.Space[]>([])
const loading = ref(true)

/**
 * 加载数据：直接锁定 Top 10
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceRankAnalyzeUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      topN: 10, // 固定获取前十名
    })
    if (res.data.code === 0) {
      dataList.value = res.data.data ?? []
    }
  } catch (e) {
    message.error('获取排行数据失败')
  } finally {
    loading.value = false
  }
}

watchEffect(() => {
  fetchData()
})

/**
 * 🚀 核心逻辑：计算相对于“榜一大哥”的比例
 */
const calculateRelativePercent = (currentSize: number) => {
  if (dataList.value.length === 0) return 0
  const max = dataList.value[0].totalSize // 第一名就是天花板
  return Math.max(5, (currentSize / max) * 100) // 最少给 5% 的进度，不然太短了看不见
}

/**
 * 🚀 核心逻辑：根据排名给颜色
 */
const getProgressBarColor = (index: number) => {
  const colors = [
    'linear-gradient(90deg, #ff4d4f 0%, #ff7875 100%)', // 第一名：火热红
    'linear-gradient(90deg, #1890ff 0%, #69c0ff 100%)', // 第二名：科技蓝
    'linear-gradient(90deg, #52c41a 0%, #95de64 100%)', // 第三名：生命绿
  ]
  return colors[index] || '#d9d9d9' // 其他：中庸灰
}


const handleRowClick = (spaceId: string) => {
  if (!spaceId) return
  // 直接跳转到分析页面，并带上 spaceId 参数
  // 因为已经在分析页面了，这样跳转会自动触发页面上的 watchEffect 重新查数据
  router.push({
    path: '/space_analyze',
    query: { spaceId }
  })
}
</script>

<style scoped>
.rank-row {
  cursor: pointer; /* 🚀 让鼠标移上去变成小手 */
  position: relative;
}
.rank-row:active {
  background: #e6f7ff; /* 🚀 点下去的时候有个蓝色的反馈 */
}
.rank-container {
  padding: 8px 0;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 12px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.rank-row:hover {
  background: rgba(24, 144, 255, 0.05); /* 鼠标移入变浅蓝 */
  transform: translateX(6px); /* 稍微位移多一点 */
}

.rank-action {
  opacity: 0;
  transition: all 0.3s;
  color: #1890ff;
}

.rank-row:hover .rank-action {
  opacity: 1; /* 🚀 移入时才显示右箭头 */
}

/* 奖牌样式 */
.rank-badge {
  width: 40px;
  text-align: center;
  font-size: 22px;
}

.rank-num {
  font-family: 'DIN Alternate', 'Arial', sans-serif;
  font-weight: 800;
  color: #bfbfbf;
  font-size: 18px;
}

.rank-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.space-name {
  font-weight: 600;
  color: #262626;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-tag {
  font-size: 10px;
  margin-left: 8px;
  line-height: 1.5;
  border-radius: 4px;
}

.size-value {
  font-weight: 700;
  color: #1890ff;
}

.size-label {
  font-size: 12px;
  color: #8c8c8c;
}

.custom-progress :deep(.ant-progress-inner) {
  background-color: #f5f5f5;
}

.empty-box {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
