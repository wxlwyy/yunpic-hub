<template>
  <div class="url-picture-upload">
    <a-input-group compact class="url-input-group">
      <a-input
        v-model:value="fileUrl"
        style="width: calc(100% - 100px)"
        placeholder="粘贴图片链接 (http/https)..."
        @press-enter="handleUpload"
        allow-clear
      />
      <a-button
        type="primary"
        :loading="loading"
        @click="handleUpload"
        style="width: 100px"
      >
        抓取
      </a-button>
    </a-input-group>

    <div class="preview-container">
      <div v-if="picture?.url && !loading" class="image-wrapper">
        <img :src="picture.url" alt="preview" class="upload-img" />
        <div class="upload-mask">
          <link-outlined />
          <span>已从 URL 抓取</span>
        </div>
      </div>

      <div v-else class="upload-trigger">
        <template v-if="loading">
          <loading-outlined class="status-icon active" />
          <div class="ant-upload-text">正在为您抓取远程资源...</div>
        </template>
        <template v-else>
          <cloud-download-outlined class="status-icon" />
          <div class="ant-upload-text">在上方输入链接后点击抓取</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { LoadingOutlined, CloudDownloadOutlined, LinkOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { uploadPictureByUrlUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: string | number // 🚀 兼容长 ID
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const loading = ref<boolean>(false);
const fileUrl = ref<string>()

/**
 * URL 抓取上传
 */
const handleUpload = async () => {
  if (!fileUrl.value) {
    return message.warning('请先输入图片 URL');
  }
  // 🚀 简单的正则校验，提升专业度
  if (!/^https?:\/\/.+/.test(fileUrl.value)) {
    return message.error('请输入正确的 http/https 协议链接');
  }

  loading.value = true
  try {
    const params: API.UploadPictureRequest = { fileUrl: fileUrl.value }
    params.spaceId = props.spaceId as any;
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片抓取成功')
      props.onSuccess?.(res.data.data)
      fileUrl.value = '' // 🚀 成功后清空输入框
    } else {
      message.error('抓取失败：' + res.data.message)
    }
  } catch (error) {
    message.error('远程链接无法访问，请检查 URL')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.url-picture-upload {
  margin-bottom: 24px;
}

.url-input-group :deep(.ant-input) {
  border-radius: 8px 0 0 8px !important;
  height: 40px;
}

.url-input-group :deep(.ant-btn) {
  border-radius: 0 8px 8px 0 !important;
  height: 40px;
  font-weight: 600;
}

/* 🚀 复用本地上传的“精装修”布局 */
.preview-container {
  margin-top: 16px;
  width: 100%;
  min-height: 200px;
  max-height: 400px;
  background-color: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s;
}

.preview-container:hover {
  border-color: #1890ff;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-img {
  width: 100%;
  height: auto;
  max-height: 400px;
  object-fit: contain;
  padding: 8px;
}

/* 悬停遮罩层 */
.upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
  backdrop-filter: blur(2px);
}

.image-wrapper:hover .upload-mask {
  opacity: 1;
}

.upload-trigger {
  text-align: center;
  padding: 40px 0;
}

.status-icon {
  font-size: 32px;
  color: #bfbfbf;
  margin-bottom: 12px;
}

.status-icon.active {
  color: #1890ff;
}

.ant-upload-text {
  color: #8c8c8c;
  font-size: 14px;
}
</style>
