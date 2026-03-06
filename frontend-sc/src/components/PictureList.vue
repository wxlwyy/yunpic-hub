<template>
  <div class="picture-list-container">
    <div class="masonry-grid" v-if="dataList && dataList.length > 0">
      <div v-for="picture in dataList" :key="picture.id" class="masonry-item">
        <div class="picture-card" @click="doClickPicture(picture)">
          <div class="image-wrapper">
            <img
              class="main-image"
              :src="picture.url"
              :style="{ aspectRatio: `${picture.picWidth} / ${picture.picHeight}` }"
              :alt="picture.name"
              loading="lazy"
              @load="handleImageLoad(picture.id)"
              :class="{ 'is-loaded': loadedImages.includes(picture.id) }"
            />

            <div class="card-overlay" v-if="showOp">
              <div class="actions-bar" @click.stop>
                <a-tooltip title="以图搜图">
                  <SearchOutlined class="action-icon" @click="doSearch(picture, $event)" />
                </a-tooltip>
                <a-tooltip title="分享">
                  <ShareAltOutlined class="action-icon" @click="doShare(picture, $event)" />
                </a-tooltip>
                <a-tooltip title="编辑" v-if="canEdit">
                  <EditOutlined class="action-icon" @click="doEdit(picture, $event)" />
                </a-tooltip>
                <a-tooltip title="删除" v-if="canDelete">
                  <DeleteOutlined class="action-icon delete" @click="doDelete(picture, $event)" />
                </a-tooltip>
              </div>
            </div>
          </div>

          <div class="info-footer">
            <div class="pic-name">{{ picture.name }}</div>
            <div class="tag-row">
              <span class="category-badge">{{ picture.category ?? '默认' }}</span>
              <span v-for="tag in picture.tags" :key="tag" class="custom-tag">
                #{{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <a-empty v-else-if="!loading" description="暂无图片" />

    <ShareModel ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
// ... 此处保留你原有的 script 逻辑 (props, router, doEdit, doDelete 等) ...
// 逻辑代码完全不需要变动，只需确保引用了图标和组件
import { useRouter } from 'vue-router'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, SearchOutlined, ShareAltOutlined } from '@ant-design/icons-vue'
import { ref } from 'vue'
import ShareModel from '@/components/ShareModal.vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
const shareModalRef = ref()
const shareLink = ref<string>()

const doClickPicture = (picture: API.PictureVO) => {
  router.push({ path: `/picture/${picture.id}` })
}

const doSearch = (picture, e) => {
  e.stopPropagation()
  window.open(`/search_picture?pictureId=${picture.id}`)
}

const doEdit = (picture, e) => {
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: { id: picture.id, spaceId: picture.spaceId },
  })
}

const doDelete = async (picture, e) => {
  e.stopPropagation()
  const res = await deletePictureUsingPost({ id: picture.id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props?.onReload()
  }
}

const doShare = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) shareModalRef.value.openModal()
}

const loadedImages = ref<string[]>([])

const handleImageLoad = (id: string) => {
  loadedImages.value.push(id)
}
</script>

<style scoped>
.picture-list-container {
  padding: 10px 0;
}

/* 瀑布流核心：列布局 */
.masonry-grid {
  column-count: 5; /* 默认 5 列 */
  column-gap: 20px;
  width: 100%;
}

/* 响应式调整列数 */
@media (max-width: 1600px) { .masonry-grid { column-count: 4; } }
@media (max-width: 1200px) { .masonry-grid { column-count: 3; } }
@media (max-width: 768px) { .masonry-grid { column-count: 2; } }
@media (max-width: 480px) { .masonry-grid { column-count: 1; } }

.masonry-item {
  break-inside: avoid; /* 防止图片在列中间断开 */
  margin-bottom: 20px;
  animation: fadeInUp 0.6s ease-out backwards;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 卡片样式 */
.picture-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #f0f0f0;
}

.picture-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

/* 图片包裹区 */
.image-wrapper {
  position: relative;
  overflow: hidden;
  background: #f5f5f5;
}

.main-image {
  width: 100%;
  display: block;
  object-fit: cover; /* 充满容器 */
  transition: transform 0.5s;
}

.picture-card:hover .main-image {
  transform: scale(1.05); /* 悬浮微变大 */
}

/* 悬浮遮罩按钮 */
.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.2);
  opacity: 0;
  transition: opacity 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.picture-card:hover .card-overlay {
  opacity: 1;
}

.actions-bar {
  display: flex;
  gap: 12px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 30px;
  backdrop-filter: blur(5px);
  transform: translateY(20px);
  transition: transform 0.3s;
}

.picture-card:hover .actions-bar {
  transform: translateY(0);
}

.action-icon {
  font-size: 18px;
  color: #64748b;
  transition: color 0.2s;
}

.action-icon:hover {
  color: #3b82f6;
}

.action-icon.delete:hover {
  color: #ff4d4f;
}

/* 底部信息 */
.info-footer {
  padding: 12px;
}

.pic-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-badge {
  font-size: 11px;
  background: #eff6ff;
  color: #3b82f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.custom-tag {
  font-size: 11px;
  color: #94a3b8;
}

/* 图片基础样式：默认完全透明 */
/* 1. 提速：将 0.8s 改为 0.5s，让图片浮现更清爽 */
.main-image {
  width: 100%;
  display: block;
  object-fit: cover;
  opacity: 0;
  transform: scale(1.01); /* 缩放幅度也调小一点，更稳重 */
  transition: opacity 0.5s cubic-bezier(0.4, 0, 0.2, 1),
  transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 图片加载完成后的样式 */
.main-image.is-loaded {
  opacity: 1;
  transform: scale(1); /* 回归正常大小 */
}

/* 2. 骨架屏升级：极光流光效果 */
.image-wrapper {
  position: relative;
  overflow: hidden;
  /* 背景换成更有质感的淡蓝灰渐变 */
  background: linear-gradient(
    110deg,
    #f8fafc 8%,
    #eff6ff 18%, /* 这里带一点淡淡的蓝色 */
    #f8fafc 33%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
  min-height: 200px;
  border-radius: 16px 16px 0 0;
}

/* 更丝滑的流光动画 */
@keyframes shimmer {
  to {
    background-position-x: -200%;
  }
}

/* 当图片加载好后，关掉骨架屏动画，避免消耗 CPU */
.is-loaded + .image-wrapper, /* 如果你需要的话，但通常直接让图片覆盖就行 */
.picture-card:has(.is-loaded) .image-wrapper {
  animation: none;
  background: #f1f5f9;
}

</style>
