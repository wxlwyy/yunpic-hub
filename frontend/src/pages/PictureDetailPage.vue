<template>
  <div id="pictureDetailPage">
    <template v-if="picture.id">
      <div
        class="immersive-bg"
        :style="{ backgroundImage: `url(${picture.url})` }"
      ></div>

      <div class="content-wrapper">
        <div class="floating-back" @click="router.back()">
          <LeftOutlined />
          <span>返回</span>
        </div>
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :lg="16" :xl="17">
            <div class="glass-card preview-container">
              <a-image
                :src="picture.url"
                class="main-image"
                :preview="true"
              />
              <div
                v-if="picture.picColor"
                class="image-glow"
                :style="{ backgroundColor: toHexColor(picture.picColor) }"
              ></div>
            </div>
          </a-col>

          <a-col :xs="24" :lg="8" :xl="7">
            <div class="glass-card info-panel">
              <div class="panel-header">
                <h2 class="pic-title">{{ picture.name ?? '未命名资产' }}</h2>
                <div class="author-section">
                  上传者：
                  <a-avatar :size="32" :src="picture.userVO?.userAvatar" class="avatar-shadow" />
                  <span class="author-name">{{ picture.userVO?.userName ?? '未知用户' }}</span>
                </div>
              </div>

              <a-divider style="margin: 16px 0; border-color: rgba(0,0,0,0.05)" />

              <div class="meta-grid">
                <div class="meta-item">
                  <span class="label">分类</span>
                  <a-tag color="blue" class="custom-tag">{{ picture.category ?? '默认' }}</a-tag>
                </div>
                <div class="meta-item">
                  <span class="label">标签</span>
                  <div class="tag-group">
                    <a-tag v-for="tag in picture.tags" :key="tag" class="custom-tag">#{{ tag }}</a-tag>
                  </div>
                </div>
                <div class="meta-item">
                  <span class="label">解析度</span>
                  <span class="value">{{ picture.picWidth ?? '-' }} × {{ picture.picHeight ?? '-' }} ({{ picture.picFormat ?? '未知' }})</span>
                </div>
                <div class="meta-item">
                  <span class="label">文件大小</span>
                  <span class="value">{{ formatSize(picture.picSize) }}</span>
                </div>
                <div class="meta-item" v-if="picture.picColor">
                  <span class="label">主色调</span>
                  <a-space>
                    <span class="value">{{ picture.picColor }}</span>
                    <div
                      class="color-dot"
                      :style="{ backgroundColor: toHexColor(picture.picColor) }"
                    ></div>
                  </a-space>
                </div>
              </div>

              <div class="intro-section">
                <span class="label">简介</span>
                <p class="intro-text">{{ picture.introduction || '暂无详细描述...' }}</p>
              </div>

              <div class="action-footer">
                <a-button type="primary" block size="large" class="download-btn" @click="doDownload">
                  <template #icon><DownloadOutlined /></template>
                  高清下载
                </a-button>

                <div class="secondary-actions">
                  <a-button @click="doShare" class="glass-btn">
                    <template #icon><ShareAltOutlined /></template> 分享
                  </a-button>
                  <a-button v-if="canEdit" @click="doEdit" class="glass-btn">
                    <template #icon><EditOutlined /></template> 编辑
                  </a-button>
                  <a-button v-if="canDelete" danger ghost @click="doDelete" class="glass-btn delete">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </template>

    <div v-else class="loading-box">
      <a-spin size="large" tip="正在载入艺术画廊..." />
    </div>

    <ShareModel ref="shareModalRef" :link="shareLink" title="分享图片" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { downloadImage, formatSize, toHexColor } from '../utils'
import { DeleteOutlined, EditOutlined, DownloadOutlined, ShareAltOutlined, LeftOutlined} from '@ant-design/icons-vue';
import ShareModel from '@/components/ShareModal.vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const picture = ref<API.PictureVO>({})

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(fetchPictureDetail)

const canEdit = computed(() => {
  return (picture.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
})

const canDelete = computed(() => {
  return (picture.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
})

const router = useRouter()
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: { id: picture.value.id, spaceId: picture.value.spaceId },
  })
}

const doDelete = async () => {
  const id = picture.value.id
  if (!id) return
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    router.back()
  }
}

const doDownload = () => {
  downloadImage(picture.value.url)
}

const shareModalRef = ref()
const shareLink = ref<string>()
const doShare = () => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
#pictureDetailPage {
  position: relative;
  min-height: calc(100vh - 150px);
  padding: 20px;
  overflow: hidden;
  border-radius: 24px;
}

.loading-box {
  height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.immersive-bg {
  position: absolute;
  top: -10%;
  left: -10%;
  width: 120%;
  height: 120%;
  background-size: cover;
  background-position: center;
  filter: blur(80px) brightness(0.7);
  z-index: 0;
  opacity: 0.5;
  transition: background-image 0.5s ease;
}

.content-wrapper {
  position: relative;
  z-index: 1;
}

.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 32px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  min-height: 600px;
}

.image-glow {
  position: absolute;
  width: 80%;
  height: 60%;
  filter: blur(100px);
  opacity: 0.2;
  z-index: -1;
  bottom: 0;
}

:deep(.main-image) {
  max-height: 75vh;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  object-fit: contain;
}

.info-panel {
  padding: 32px;
  height: 100%;
}

.pic-title {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 12px;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  font-weight: 600;
  color: #475569;
}

.label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.value {
  color: #334155;
  font-weight: 500;
}

.meta-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 1px solid rgba(0,0,0,0.1);
}

.intro-section {
  margin-top: 24px;
}

.intro-text {
  color: #64748b;
  line-height: 1.6;
}

.action-footer {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.download-btn {
  height: 50px;
  border-radius: 14px;
  font-weight: 700;
  font-size: 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2);
}

.secondary-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 50px;
  gap: 10px;
}

.glass-btn {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.6);
  font-weight: 600;
}

.floating-back {
  position: absolute;
  top: 40px;
  left: 40px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 50px;
  color: #1e293b;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.floating-back:hover {
  background: rgba(255, 255, 255, 0.5);
  transform: translateX(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}
</style>
