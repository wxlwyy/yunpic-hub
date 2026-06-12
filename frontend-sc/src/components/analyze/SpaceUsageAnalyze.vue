<template>
  <div class="space-usage-analyze">
    <a-skeleton :loading="loading" active :paragraph="{ rows: 3 }">
      <a-flex gap="large" justify="space-around" align="center" class="usage-container">

        <div class="usage-item">
          <div class="label-group">
            <database-outlined class="icon" />
            <span class="label-text">存储空间</span>
          </div>
          <div class="progress-wrapper">
            <a-progress
              type="dashboard"
              :percent="Number(data.sizeUsage?.toFixed(1) ?? 0)"
              :stroke-color="getUsageColor(data.sizeUsage)"
              :stroke-width="10"
              :width="160"
            >
              <template #format="percent">
                <div class="percent-content">
                  <span class="num">{{ percent }}%</span>
                  <span class="desc">已用</span>
                </div>
              </template>
            </a-progress>
          </div>
          <div class="footer-info">
            <span class="current">{{ formatSize(data.usedSize) }}</span>
            <span class="divider">/</span>
            <span class="total">{{ data.maxSize ? formatSize(data.maxSize) : '无限制' }}</span>
          </div>
        </div>

        <a-divider type="vertical" style="height: 120px" />

        <div class="usage-item">
          <div class="label-group">
            <picture-outlined class="icon" />
            <span class="label-text">图片数量</span>
          </div>
          <div class="progress-wrapper">
            <a-progress
              type="dashboard"
              :percent="Number(data.countUsage?.toFixed(1) ?? 0)"
              :stroke-color="getUsageColor(data.countUsage)"
              :stroke-width="10"
              :width="160"
            >
              <template #format="percent">
                <div class="percent-content">
                  <span class="num">{{ percent }}%</span>
                  <span class="desc">已占</span>
                </div>
              </template>
            </a-progress>
          </div>
          <div class="footer-info">
            <span class="current">{{ data.usedCount }} 张</span>
            <span class="divider">/</span>
            <span class="total">{{ data.maxCount ?? '无限制' }} 张</span>
          </div>
        </div>
      </a-flex>
    </a-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { getSpaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { DatabaseOutlined, PictureOutlined } from '@ant-design/icons-vue'
import { formatSize } from '../../utils'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: string // 🚀 建议用 string 兼容长 ID
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

const data = ref<API.SpaceUsageAnalyzeResponse>({})
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSpaceUsageAnalyzeUsingPost({
      queryAll: props.queryAll,
      queryPublic: props.queryPublic,
      spaceId: props.spaceId,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data
    }
  } catch (e) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

watchEffect(() => {
  fetchData()
})

// 🚀 核心逻辑：根据使用率动态返回颜色
const getUsageColor = (percent: number | undefined) => {
  if (!percent) return '#52c41a' // 绿色
  if (percent > 90) return '#ff4d4f' // 红色（预警）
  if (percent > 70) return '#faad14' // 橙色（提醒）
  return '#1890ff' // 蓝色（正常）
}
</script>

<style scoped>
.usage-container {
  padding: 20px 0;
}

.usage-item {
  text-align: center;
  flex: 1;
}

.label-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #8c8c8c;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
}

.progress-wrapper {
  margin-bottom: 16px;
}

/* 进度条中间的文字自定义 */
.percent-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.percent-content .num {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
}

.percent-content .desc {
  font-size: 12px;
  color: #bfbfbf;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.footer-info .current {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.footer-info .divider {
  color: #d9d9d9;
}

.footer-info .total {
  color: #8c8c8c;
  font-size: 14px;
}
</style>
