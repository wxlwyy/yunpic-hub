<template>
  <div id="searchPicturePage">
    <div class="page-header">
      <h2 class="page-title"><CameraOutlined />以图搜图</h2>
<!--      <p class="page-desc">基于深度学习的图像特征提取，为您在全网寻找高度相似的视觉资源。</p>-->
    </div>

    <div class="search-workspace">
      <a-row :gutter="[24, 24]">
        <a-col :xs="24" :md="8" :lg="6">
          <div class="sidebar-panel">
            <h3 class="panel-title"><AimOutlined /> 目标特征图</h3>
            <a-card class="original-card" :bordered="false">
              <template #cover>
                <div class="image-wrapper" :class="{ 'is-scanning': loading }">
                  <img
                    class="target-img"
                    :alt="picture.name"
                    :src="picture.thumbnailUrl ?? picture.url"
                  />
                  <div v-if="loading" class="scan-overlay">
                    <div class="scan-line"></div>
                  </div>
                </div>
              </template>
              <a-card-meta :title="picture.name || '未命名图片'">
                <template #description>
                  <a-tag color="blue" class="pulse-tag" v-if="loading">正在提取特征...</a-tag>
                  <a-tag color="green" v-else>特征提取完成</a-tag>
                </template>
              </a-card-meta>
            </a-card>
          </div>
        </a-col>

        <a-col :xs="24" :md="16" :lg="18">
          <div class="results-panel">
            <div class="results-header">
              <h3 class="panel-title"><AppstoreOutlined /> 全网匹配结果</h3>
              <span class="results-count" v-if="!loading">共找到 {{ dataList.length }} 个高度相似结果</span>
            </div>

            <a-empty
              v-if="!loading && dataList.length === 0"
              image="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
              description="AI 未能在图库中找到相似的图像"
              style="margin-top: 60px;"
            />

            <a-list
              :grid="{ gutter: 16, xs: 2, sm: 2, md: 3, lg: 4, xl: 4, xxl: 5 }"
              :data-source="dataList"
              :loading="loading"
              class="result-list"
            >
              <template #renderItem="{ item }">
                <a-list-item style="padding: 0">
                  <a :href="item.objUrl" target="_blank" class="result-link">
                    <a-card class="result-card" :bordered="false" hoverable>
                      <template #cover>
                        <div class="result-img-wrapper">
                          <img class="result-img" :src="item.thumbUrl" loading="lazy" />
                          <div class="hover-mask">
                            <span class="view-text"><LinkOutlined /> 查看来源</span>
                          </div>
                        </div>
                      </template>
                    </a-card>
                  </a>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPictureVoByIdUsingGet, searchPictureByPictureUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { CameraOutlined, AimOutlined, AppstoreOutlined, LinkOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const loading = ref<boolean>(true)

// 图片 id
const pictureId = computed(() => {
  return route.query?.pictureId
})

const picture = ref<API.PictureVO>({})

// 获取老数据
const getOldPicture = async () => {
  const id = route.query?.pictureId
  if (id) {
    const res = await getPictureVoByIdUsingGet({ id: id as any })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    }
  }
}

const dataList = ref<API.ImageSearchResult[]>([])
// 获取搜图结果
const fetchData = async () => {
  loading.value = true
  try {
    const res = await searchPictureByPictureUsingPost({
      pictureId: pictureId.value as any,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
    } else {
      message.error('匹配节点连接失败，' + res.data.message)
    }
  } catch (error) {
    message.error('网络请求异常，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 页面加载时请求
onMounted(() => {
  getOldPicture()
  fetchData()
})
</script>

<style scoped>
#searchPicturePage {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

/* 页面头部 */
.page-header {
  margin-bottom: 32px;
}
.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-desc {
  color: #64748b;
  font-size: 15px;
}

/* 工作区结构 */
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 左侧：原图卡片与扫描特效 */
.original-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  background: #fff;
}
.image-wrapper {
  position: relative;
  overflow: hidden;
  height: 240px;
  background: #f8fafc;
}
.target-img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 改为 contain，防止原图被裁剪缺失特征 */
}

/* AI 扫描光带特效 */
.scan-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(59, 130, 246, 0.1);
  pointer-events: none;
}
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(to bottom, rgba(59, 130, 246, 0), #3b82f6);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.8);
  animation: scan 1.5s infinite ease-in-out;
}
@keyframes scan {
  0% { top: -10%; opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { top: 110%; opacity: 0; }
}

/* 呼吸标签 */
.pulse-tag {
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.6; }
  100% { opacity: 1; }
}

/* 右侧：结果面板 */
.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
}
.results-count {
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 20px;
}

/* 结果卡片特效 */
.result-card {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}
.result-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}
.result-img-wrapper {
  position: relative;
  height: 180px;
  overflow: hidden;
}
.result-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.result-card:hover .result-img {
  transform: scale(1.05);
}

/* 悬浮黑色遮罩 */
.hover-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.result-card:hover .hover-mask {
  opacity: 1;
}
.view-text {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  border: 1px solid #fff;
  padding: 6px 16px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}
</style>
