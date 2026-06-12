<template>
  <div id="addSpacePage">
    <div class="page-header">
      <h2 class="page-title">
        <component :is="spaceType === SPACE_TYPE_ENUM.TEAM ? 'TeamOutlined' : 'UserOutlined'" class="title-icon" />
        {{ route.query?.id ? '编辑' : '开通' }}{{ SPACE_TYPE_MAP[spaceType] }}
      </h2>
      <p class="page-desc">
        {{ spaceType === SPACE_TYPE_ENUM.TEAM ? '创建一个多人协作的云端图库，让团队共享创意资源。' : '打造属于你自己的私密云端图库，安全存储珍贵影像。' }}
      </p>
    </div>

    <a-card class="form-card" :bordered="false">
      <a-form layout="vertical" :model="formData" @finish="handleSubmit" class="modern-form">
        <a-form-item label="空间名称" name="spaceName" :rules="[{ required: true, message: '请给空间起个响亮的名字' }]">
          <a-input
            v-model:value="formData.spaceName"
            placeholder="例如：设计部公用图库 / 我的旅行珍藏..."
            class="modern-input"
            size="large"
            allow-clear
          >
            <template #prefix><AppstoreAddOutlined style="color: #bfbfbf" /></template>
          </a-input>
        </a-form-item>

        <a-form-item label="配置空间级别" name="spaceLevel">
          <a-select
            v-model:value="formData.spaceLevel"
            :options="SPACE_LEVEL_OPTIONS"
            placeholder="请选择需要的空间级别"
            class="modern-input"
            size="large"
            allow-clear
          >
            <template #suffixIcon><TrophyOutlined style="color: #bfbfbf" /></template>
          </a-select>
        </a-form-item>

        <div class="form-footer">
          <a-button type="primary" html-type="submit" class="submit-btn" size="large" block :loading="loading">
            <template #icon><CloudUploadOutlined v-if="!loading" /></template>
            {{ loading ? '正在分配云端资源...' : (route.query?.id ? '保存空间配置' : '立即开通空间') }}
          </a-button>
        </div>
      </a-form>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card class="info-card" :bordered="false" title="💎 空间权益与规则说明">
      <a-alert
        message="权限管控须知"
        description="管理员可创建任意类型的空间；VIP 贵宾可开通普通或专业版空间；普通用户目前仅支持开通基础版空间。"
        type="info"
        show-icon
        style="margin-bottom: 20px; border-radius: 8px;"
      />

      <div class="level-grid">
        <div v-for="spaceLevel in spaceLevelList" :key="spaceLevel.value" class="level-item">
          <div class="level-header">
            <span class="level-name">{{ spaceLevel.text }}</span>
          </div>
          <div class="level-detail">
            <div class="detail-row">
              <span class="detail-label">存储容量</span>
              <span class="detail-value highlight">{{ formatSize(spaceLevel.maxSize) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">最大图片数</span>
              <span class="detail-value">{{ spaceLevel.maxCount }} 张</span>
            </div>
          </div>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  UserOutlined, TeamOutlined, AppstoreAddOutlined,
  TrophyOutlined, CloudUploadOutlined
} from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet, listSpaceLevelUsingGet, updateSpaceUsingPost
} from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_OPTIONS, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space.ts'
import { formatSize } from "@/utils"; // 👈 无论你在哪层目录，@ 永远代表 src

const formData = reactive<API.AddSpaceRequest | API.UpdateSpaceRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
})
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const oldSpace = ref<API.SpaceVO>()
const spaceLevelList = ref<API.SpaceLevel[]>([])

// 空间类别（没传空间类别时，默认就是私有空间）
const spaceType = computed(() => {
  if (route.query?.type) {
    return Number(route.query.type)
  }
  return SPACE_TYPE_ENUM.PRIVATE
})

/**
 * 提交表单
 */
const handleSubmit = async () => {
  const spaceId = oldSpace.value?.id
  loading.value = true
  let res
  try {
    if (spaceId) {
      // 更新
      res = await updateSpaceUsingPost({
        id: spaceId,
        ...formData,
      })
    } else {
      // 创建
      res = await addSpaceUsingPost({
        ...formData,
        spaceType: spaceType.value
      })
    }
    if (res.data.code === 0 && res.data.data) {
      message.success(spaceId ? '配置更新成功' : '空间开通成功')
      let path = `/space/${spaceId ?? res.data.data}`
      router.push({ path })
    } else {
      message.error('操作异常：' + res.data.message)
    }
  } catch (e) {
    message.error('网络或系统异常')
  } finally {
    loading.value = false
  }
}

// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('加载空间级别失败：' + res.data.message)
  }
}

// 获取老数据
const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({ id: id as any })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      oldSpace.value = data
      formData.spaceName = data.spaceName
      formData.spaceLevel = data.spaceLevel
    }
  }
}

onMounted(() => {
  fetchSpaceLevelList()
  getOldSpace()
})
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
}

/* 头部样式 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}
.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.title-icon {
  color: #3b82f6;
}
.page-desc {
  color: #64748b;
  font-size: 15px;
}

/* 卡片通用样式 */
.form-card, .info-card {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.modern-input {
  border-radius: 10px !important;
}

/* 提交按钮 */
.form-footer {
  margin-top: 32px;
}
.submit-btn {
  height: 54px;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.2);
  transition: all 0.3s ease;
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.3);
}

/* 权益展示网格 */
.level-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.level-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}
.level-item:hover {
  background: #fff;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
  transform: translateY(-2px);
}

.level-header {
  border-bottom: 1px dashed #cbd5e1;
  padding-bottom: 12px;
  margin-bottom: 12px;
}
.level-name {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}
.detail-label {
  color: #64748b;
}
.detail-value {
  color: #334155;
  font-weight: 600;
}
.detail-value.highlight {
  color: #3b82f6;
  font-family: monospace;
  font-size: 15px;
}
</style>
