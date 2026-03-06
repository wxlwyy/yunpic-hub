<template>
  <div id="addPicturePage">
    <div class="page-header">
      <h2 class="page-title">
        <component :is="route.query?.id ? 'EditOutlined' : 'PlusCircleOutlined'" class="title-icon" />
        {{ route.query?.id ? '编辑图片详情' : '创作新图片' }}
      </h2>
      <a-typography-paragraph v-if="spaceId" class="space-badge">
        <RocketOutlined /> 归属空间：
        <a :href="`/space/${spaceId}`" target="_blank">#{{ spaceId }}</a>
      </a-typography-paragraph>
    </div>

    <div class="studio-container">
      <section class="studio-section upload-card">
        <div class="section-label">01 媒体素材</div>
        <a-tabs v-model:activeKey="uploadType" class="modern-tabs">
          <a-tab-pane key="file">
            <template #tab>
      <span>
        <FileImageOutlined />
        {{ route.query?.id ? '替换文件' : '文件上传' }}
      </span>
            </template>
            <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
          </a-tab-pane>
          <a-tab-pane key="url" force-render>
            <template #tab>
      <span>
        <LinkOutlined />
        {{ route.query?.id ? '更换 URL' : 'URL 抓取' }}
      </span>
            </template>
            <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
          </a-tab-pane>
        </a-tabs>

        <transition name="fade-slide">
          <div v-if="picture" class="smart-edit-bar">
            <div class="bar-content">
              <a-button class="tool-btn" @click="doEditPicture">
                <template #icon><EditOutlined /></template> 专业裁剪
              </a-button>
              <div class="divider"></div>
              <a-button type="primary" ghost class="tool-btn ai-btn" @click="doImagePainting">
                <template #icon><FullscreenOutlined /></template> AI 智能扩图
              </a-button>
            </div>
          </div>
        </transition>
      </section>

      <transition name="fade-slide">
        <section v-if="picture" class="studio-section info-card">
          <div class="section-label">02 属性定义</div>
          <a-form layout="vertical" :model="pictureForm" @finish="handleSubmit" class="modern-form">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="名称" name="name" :rules="[{ required: true, message: '给图片起个好名字吧' }]">
                  <a-input v-model:value="pictureForm.name" placeholder="请输入名称" class="modern-input" />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="简介" name="introduction">
                  <a-textarea
                    v-model:value="pictureForm.introduction"
                    placeholder="描述一下这张图片的背景或故事..."
                    :auto-size="{ minRows: 3, maxRows: 6 }"
                    class="modern-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="分类" name="category">
                  <a-auto-complete
                    v-model:value="pictureForm.category"
                    placeholder="选择或输入分类"
                    :options="categoryOptions"
                    class="modern-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item label="标签" name="tags">
                  <a-select
                    v-model:value="pictureForm.tags"
                    mode="tags"
                    placeholder="输入标签按回车确认"
                    :options="tagOptions"
                    class="modern-input"
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <div class="form-footer">
              <a-button type="primary" html-type="submit" class="submit-btn" size="large" block>
                {{ route.query?.id ? '保存修改' : '立即发布到云端' }}
              </a-button>
            </div>
          </a-form>
        </section>
      </transition>
    </div>

    <ImageOutPainting
      ref="imageOutPaintingRef"
      :picture="picture"
      :spaceId="spaceId"
      :onSuccess="onImageOutPaintingSuccess"
    />
    <ImageCropper
      ref="imageCropperRef"
      :imageUrl="picture?.url"
      :picture="picture"
      :spaceId="spaceId"
      :space="space"
      :onSuccess="onCropSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import {
  EditOutlined, FullscreenOutlined, PlusCircleOutlined,
  RocketOutlined, FileImageOutlined, LinkOutlined
} from '@ant-design/icons-vue'
// ... 保持你原有的所有 script 逻辑不变 (fetchData, handleSubmit 等) ...
// 记得补上上面引入的图标
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, h, onMounted, reactive, ref, watchEffect } from 'vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

const uploadType = ref<'file' | 'url'>('file')
const route = useRoute()
const router = useRouter()
const spaceId = computed(() => route.query?.spaceId)
const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.EditPictureRequest>({})
const space = ref<API.SpaceVO>()

const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

const handleSubmit = async (values: any) => {
  const pictureId = picture.value?.id
  if (!pictureId) return
  const res = await editPictureUsingPost({ id: pictureId, spaceId: spaceId.value, ...values })
  if (res.data.code === 0) {
    message.success('保存成功')
    router.push({ path: `/picture/${pictureId}` })
  }
}

const categoryOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])
const getCategoryTagOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    categoryOptions.value = (res.data.data.categoryList ?? []).map(item => ({ value: item, label: item }))
    tagOptions.value = (res.data.data.tagList ?? []).map(item => ({ value: item, label: item }))
  }
}

const getOldPicture = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      Object.assign(pictureForm, {
        name: data.name,
        introduction: data.introduction,
        category: data.category,
        tags: data.tags
      })
    }
  }
}

const imageCropperRef = ref()
const doEditPicture = () => imageCropperRef.value?.openModal()
const onCropSuccess = (newPicture: API.PictureVO) => picture.value = newPicture

const imageOutPaintingRef = ref()
const doImagePainting = () => imageOutPaintingRef.value?.openModal()
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => picture.value = newPicture

const fetchSpace = async () => {
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({ id: spaceId.value })
    if (res.data.code === 0) space.value = res.data.data
  }
}

onMounted(() => { getCategoryTagOptions(); getOldPicture(); })
watchEffect(fetchSpace)
</script>

<style scoped>
#addPicturePage {
  max-width: 900px; /* 适当拉宽，让表单不那么局促 */
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.title-icon {
  color: #3b82f6;
}

.space-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 8px;
  color: #64748b;
  font-size: 13px;
}

.studio-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.studio-section {
  background: #fff;
  border-radius: 24px;
  padding: 32px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  position: relative;
}

.section-label {
  position: absolute;
  top: -12px;
  left: 24px;
  background: #3b82f6;
  color: #fff;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

/* 上传区样式 */
.upload-card {
  min-height: 300px;
}

.modern-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px !important;
}

.modern-tabs :deep(.ant-tabs-tab) {
  font-weight: 600;
  font-size: 15px;
}

/* 悬浮编辑条 */
.smart-edit-bar {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.bar-content {
  display: flex;
  align-items: center;
  background: #1e293b;
  padding: 8px 16px;
  border-radius: 50px;
  gap: 8px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.tool-btn {
  color: #cbd5e1 !important;
  background: transparent !important;
  border: none !important;
  font-weight: 600;
}

.tool-btn:hover {
  color: #fff !important;
}

.tool-btn.ai-btn {
  color: #60a5fa !important;
}

.divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.1);
}

/* 表单样式 */
.modern-input {
  border-radius: 12px !important;
}

.submit-btn {
  height: 54px;
  border-radius: 16px;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  margin-top: 24px;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2);
}

/* 动画 */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.5s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
</style>
