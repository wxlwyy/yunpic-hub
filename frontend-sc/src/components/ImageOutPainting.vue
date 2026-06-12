<template>
  <a-modal
    class="image-out-painting-modal"
    v-model:visible="visible"
    title="✨ AI 智能扩图引擎"
    :footer="false"
    width="800px"
    @cancel="closeModal"
    destroyOnClose
  >
    <div class="control-panel">
      <span class="control-label">选择扩图比例：</span>
      <a-radio-group v-model:value="outPaintingScale" button-style="solid" :disabled="!!taskId">
        <a-radio-button :value="1.5">1.5 倍轻度扩展</a-radio-button>
        <a-radio-button :value="2.0">2.0 倍标准扩图</a-radio-button>
        <a-radio-button :value="3.0">3.0 倍全景重构</a-radio-button>
      </a-radio-group>
    </div>

    <a-row :gutter="24" class="visual-workspace">
      <a-col :span="12">
        <div class="panel-header">
          <PictureOutlined class="icon" /> 原始参考图
        </div>
        <div class="image-wrapper">
          <img :src="picture?.url" :alt="picture?.name" class="preview-img" />
        </div>
      </a-col>

      <a-col :span="12">
        <div class="panel-header">
          <ApiOutlined class="icon ai-icon" /> AI 生成结果
        </div>

        <div class="image-wrapper result-wrapper">
          <img
            v-if="resultImageUrl"
            :src="resultImageUrl"
            :alt="picture?.name"
            class="preview-img"
          />

          <div v-else-if="taskId" class="ai-generating-state">
            <div class="scanner"></div>
            <div class="ai-icon-wrapper">
              <ThunderboltTwoTone two-tone-color="#8b5cf6" style="font-size: 40px" class="pulse-icon" />
            </div>
            <p class="generate-text">AI 算力引擎正在重构像素...</p>
            <p class="generate-subtext">已成功下发任务，正在等待算力集群响应</p>
          </div>

          <div v-else class="empty-state">
            <FormatPainterOutlined class="empty-icon" />
            <p>点击下方按钮开始智能扩图</p>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-divider style="margin: 20px 0" />

    <div class="action-footer">
      <a-button @click="closeModal" :disabled="!!taskId">
        取消操作
      </a-button>
      <a-button
        type="primary"
        ghost
        class="magic-btn"
        :loading="!!taskId"
        @click="createTask"
        v-if="!resultImageUrl"
      >
        <template #icon><ThunderboltOutlined v-if="!taskId" /></template>
        {{ taskId ? '生成中...' : '开始执行扩图任务' }}
      </a-button>
      <a-button
        type="primary"
        class="apply-btn"
        v-if="resultImageUrl"
        :loading="uploadLoading"
        @click="handleUpload"
      >
        <template #icon><CheckCircleOutlined /></template>
        保存并应用该结果
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskUsingGet,
  uploadPictureByUrlUsingPost
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import {
  PictureOutlined, ApiOutlined, FormatPainterOutlined,
  ThunderboltOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const visible = ref(false)
const resultImageUrl = ref<string>()
const taskId = ref<string | null>(null)
const uploadLoading = ref<boolean>(false)

// 新增：让用户可选择的扩图倍数
const outPaintingScale = ref<number>(2.0)

const createTask = async () => {
  if (!props.picture?.id) return

  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    parameters: {
      xScale: outPaintingScale.value,
      yScale: outPaintingScale.value,
    },
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('任务引擎已启动，正在为您生成图像...')
    taskId.value = res.data.data.output.taskId
    startPolling()
  } else {
    message.error('任务下发失败，' + res.data.message)
  }
}

let pollingTimer: ReturnType<typeof setInterval> | null = null

const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  // 关键修复：清理轮询时，一定要把 taskId 置空，否则按钮会一直处于 loading 状态
  taskId.value = null
}

const startPolling = () => {
  if (!taskId.value) return

  pollingTimer = setInterval(async () => {
    try {
      const res = await getPictureOutPaintingTaskUsingGet({
        taskId: taskId.value as string,
      })

      // 1. 正常响应
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('🎉 AI 扩图重构完成！')
          resultImageUrl.value = taskResult.outputImageUrl
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('AI 扩图任务失败，请尝试换一张图')
          clearPolling()
        }
      }
      // 🚨 2. 关键修复：后端抛出业务异常（比如图片太小），立刻熔断！
      else {
        message.error('扩图异常：' + res.data.message)
        clearPolling() // 停止转圈
      }
    } catch (error) {
      console.error('轮询任务状态失败', error)
      message.error('与 AI 节点通讯中断，已停止查询')
      clearPolling()
    }
  }, 3000)
}

onUnmounted(() => {
  clearPolling()
})

const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.UploadPictureRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('结果图片已成功同步至图库')
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('同步失败，' + res.data.message)
    }
  } catch (error) {
    message.error('网络传输异常')
  } finally {
    uploadLoading.value = false
  }
}

const openModal = () => {
  // 关键修复：每次打开弹窗前，重置之前的残留状态
  resultImageUrl.value = undefined
  taskId.value = null
  outPaintingScale.value = 2.0
  visible.value = true
}

const closeModal = () => {
  visible.value = false
  // 关键修复：关闭弹窗时必须清理定时器，防止后台持续发请求刷爆接口
  clearPolling()
}

defineExpose({
  openModal,
})
</script>

<style scoped>
.image-out-painting-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}

/* 顶部控制盘 */
.control-panel {
  background: #f8fafc;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
}
.control-label {
  font-weight: 600;
  color: #334155;
  margin-right: 12px;
}

/* 工作区 */
.panel-header {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-icon {
  color: #8b5cf6; /* 象征 AI 的紫色 */
}

.image-wrapper {
  width: 100%;
  height: 320px;
  background: #f1f5f9;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}

.preview-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

/* 空闲状态 */
.empty-state {
  text-align: center;
  color: #94a3b8;
}
.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
  color: #cbd5e1;
}

/* AI 生成中动效 */
.ai-generating-state {
  position: relative; /* 确保扫描线在容器内移动 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(139, 92, 246, 0.02) 0%, rgba(139, 92, 246, 0.08) 100%);
}
.generate-text {
  margin-top: 16px;
  font-weight: 600;
  color: #8b5cf6;
  margin-bottom: 4px;
}
.generate-subtext {
  font-size: 12px;
  color: #94a3b8;
}

/* 扫描仪动画特效 */
.scanner {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 8px; /* 稍微加厚一点，更有存在感 */
  background: linear-gradient(to bottom, rgba(139, 92, 246, 0), #8b5cf6);
  box-shadow: 0 4px 15px rgba(139, 92, 246, 0.6);
  animation: scan-vertical 2.5s infinite linear; /* 时间拉长一点，更显从容 */
  z-index: 10;
}

/* 呼吸灯效果，代替转圈圈 */
.pulse-icon {
  animation: icon-pulse 2s infinite ease-in-out;
  margin-bottom: 16px;
}

@keyframes scan-vertical {
  0% { top: 0; opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

/* 底部操作区 */
.action-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.magic-btn {
  border-color: #8b5cf6;
  color: #8b5cf6;
}
.magic-btn:hover {
  background: #8b5cf6;
  color: #fff;
}
.apply-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
}
.apply-btn:hover {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
}
</style>
