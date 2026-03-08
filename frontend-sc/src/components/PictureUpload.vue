<template>
  <div class="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      :custom-request="handleUpload"
      class="avatar-uploader"
    >
      <div v-if="picture?.url && !loading" class="image-wrapper">
        <img :src="picture.url" alt="picture" class="upload-img" />
        <div class="upload-mask">
          <edit-outlined />
          <span>更换图片</span>
        </div>
      </div>

      <div v-else class="upload-trigger">
        <loading-outlined v-if="loading" class="status-icon" />
        <plus-outlined v-else class="status-icon" />
        <div class="ant-upload-text">{{ loading ? '上传中...' : '点击或拖拽上传' }}</div>
      </div>
    </a-upload>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { PlusOutlined, LoadingOutlined, EditOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: string | number // 🚀 兼容长 ID
  onSuccess?: (newPicture: API.PictureVO) => void
}
const props = defineProps<Props>()

const loading = ref<boolean>(false);

/**
 * 上传前的校验
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  // 🚀 增加 WebP 格式支持
  const isTypeOk = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type);
  if (!isTypeOk) {
    message.error('仅支持 JPG/PNG/WebP 格式的图片');
  }
  const isLt5M = file.size / 1024 / 1024 < 5; // 🚀 适当放宽到 5M，毕竟现在手机拍照都大
  if (!isLt5M) {
    message.error('图片不能超过 5MB');
  }
  return isTypeOk && isLt5M;
};

/**
 * 自定义上传
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true;
  try {
    const params: API.UploadPictureRequest = props.picture ? { id: props.picture.id } : {};
    params.spaceId = props.spaceId as any;

    const res = await uploadPictureUsingPost(params, {}, file);

    if (res.data.code === 0 && res.data.data) {
      message.success('上传成功');
      props.onSuccess?.(res.data.data);
    } else {
      message.error('上传失败：' + res.data.message);
    }
  } catch (error) {
    message.error('网络错误，上传失败');
  } finally {
    // 🚀 重点优化：无论如何都要停止 loading
    loading.value = false;
  }
}
</script>

<style scoped>
/* 优化后的上传框样式 */
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  /* 🚀 核心改动：不再写死 height，改用 min/max 组合 */
  height: auto !important;
  min-height: 180px;
  max-height: 400px; /* 给一个封顶值，防止长图霸屏 */

  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #fafafa;
  border-radius: 12px;
  padding: 0 !important; /* 移除内边距，让图片满铺 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.picture-upload :deep(.ant-upload:hover) {
  border-color: #1890ff;
  background-color: #f0f7ff;
}

/* 图片容器 */
.image-wrapper {
  position: relative;
  width: 100%;
  /* 🚀 让容器高度根据内容自适应 */
  height: auto;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-img {
  width: 100%;
  /* 🚀 这里的 height: auto 配合 width: 100% 能够保持原图比例 */
  height: auto;
  max-height: 400px;
  object-fit: contain;
  display: block;
}

/* 🚀 高级遮罩层：鼠标移上去显示 */
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

.status-icon {
  font-size: 28px;
  color: #bfbfbf;
  margin-bottom: 12px;
}

.ant-upload-text {
  color: #8c8c8c;
  font-size: 13px;
}

/* 状态图标（没图时） */
.upload-trigger {
  padding: 40px 0; /* 🚀 没图时撑开一点高度，显得大方 */
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
