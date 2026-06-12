<template>
  <div class="avatar-uploader-container">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      :custom-request="handleUpload"
      class="avatar-uploader"
    >
      <div v-if="picture?.url && !loading" class="avatar-wrapper">
        <img :src="picture.url" alt="avatar" class="avatar-img" />
        <div class="avatar-mask">
          <camera-outlined class="mask-icon" />
          <span>更换头像</span>
        </div>
      </div>

      <div v-else class="upload-trigger">
        <loading-outlined v-if="loading" class="status-icon" />
        <plus-outlined v-else class="status-icon" />
        <div class="ant-upload-text">{{ loading ? '上传中...' : '上传' }}</div>
      </div>
    </a-upload>
    <div class="upload-tip">建议：200x200 或等比例正方形，大小不超过 1MB</div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { PlusOutlined, LoadingOutlined, CameraOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { uploadUserAvatarUsingPost } from '@/api/userController'

interface Props {
  picture?: { url?: string } // 接收当前头像 URL
}

const props = defineProps<Props>()
const emit = defineEmits(['onSuccess']) // 成功后通知父组件

const loading = ref<boolean>(false)

/**
 * 上传前校验：头像要更严格一点
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  const isJpgOrPng = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!isJpgOrPng) {
    message.error('只支持 JPG/PNG/WebP 格式')
  }
  const isLt1M = file.size / 1024 / 1024 < 1
  if (!isLt1M) {
    message.error('头像不能超过 1MB')
  }
  return isJpgOrPng && isLt1M
}

/**
 * 核心：调用头像专用上传接口
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    // 🚀 这里调用的是咱们后端新写的 uploadUserAvatar 接口
    const res = await uploadUserAvatarUsingPost({}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('头像上传成功')
      // 🚀 重点：把后端返回的 URL 传给父组件
      emit('onSuccess', res.data.data)
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('头像上传异常')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 🚀 这里的样式让它看起来像个大厂的头像组件 */
.avatar-uploader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 强制把上传框变成圆形 */
:deep(.ant-upload.ant-upload-select-picture-card) {
  width: 120px !important;
  height: 120px !important;
  border-radius: 50% !important; /* 变成圆的 */
  overflow: hidden;
  background-color: #f8fafc;
  border: 2px dashed #e2e8f0;
  margin: 0;
}

.avatar-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 🚀 重点：头像必须 cover，保证铺满圆圈且不变形 */
}

/* 悬停遮罩：圆形 */
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  backdrop-filter: blur(2px);
  border-radius: 50%;
  font-size: 12px;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.mask-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.status-icon {
  font-size: 24px;
  color: #bfbfbf;
}

.upload-tip {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
</style>
