<template>
  <div id="spaceDetailPage">
    <a-card class="dashboard-banner" :bordered="false">
      <div class="banner-content">
        <div class="space-info-section">
          <a-tooltip :title="`已用 ${formatSize(space.totalSize)} / 总计 ${formatSize(space.maxSize)}`">
            <a-progress
              type="circle"
              :percent="usagePercent"
              :size="72"
              :stroke-width="8"
              :stroke-color="getStorageColor(usagePercent)"
              :format="(percent) => percent >= 100 ? '已满' : `${percent}%`"
              class="usage-circle"
            />
          </a-tooltip>
          <div class="space-text-info">
            <h2 class="space-title">
              {{ space.spaceName }}
              <a-space :size="8">
                <a-tag :color="space.spaceType === SPACE_TYPE_ENUM.TEAM ? 'cyan' : 'blue'" class="type-tag">
                  {{ SPACE_TYPE_MAP[space.spaceType] }}
                </a-tag>

                <a-tooltip title="您在该空间的当前角色权限">
                  <a-tag :color="currentRoleInfo.color" class="role-tag-premium">
                    <template #icon>
                      <SafetyCertificateOutlined v-if="currentRoleInfo.text === '管理员'" />
                      <UserOutlined v-else />
                    </template>
                    {{ currentRoleInfo.text }}
                  </a-tag>
                </a-tooltip>
              </a-space>
            </h2>
            <p class="space-stats">
              当前包含 <strong class="highlight">{{ total }}</strong> 张图片，上限 {{ space.maxCount }} 张
            </p>
          </div>
        </div>

        <div class="action-section">
          <a-space size="middle" wrap>
            <a-button
              v-if="canAnalyzeSpace"
              type="primary"
              ghost
              class="modern-btn"
              @click="router.push({ path: '/space_analyze', query: { spaceId: id } })"
            >
              <template #icon><BarChartOutlined /></template> 空间分析
            </a-button>
            <a-button
              v-if="canManageSpaceUser"
              type="primary"
              ghost
              class="modern-btn"
              @click="router.push(`/spaceUserManage/${id}`)"
            >
              <template #icon><TeamOutlined /></template> 成员管理
            </a-button>
            <a-button v-if="canEditPicture" class="modern-btn" @click="openBatchEditModal">
              <template #icon><EditOutlined /></template> 批量编辑
            </a-button>
            <a-button
              v-if="canUploadPicture"
              type="primary"
              class="modern-btn upload-btn"
              @click="router.push({ path: '/add_picture', query: { spaceId: id } })"
            >
              <template #icon><CloudUploadOutlined /></template> 上传图片
            </a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card class="filter-card" :bordered="false">
      <template #title>
        <span class="card-title"><SearchOutlined /> 智能检索与筛选</span>
      </template>

      <PictureSearchForm :onSearch="handleSearch" />

      <a-divider style="margin: 16px 0;" />

      <div class="color-search-area">
        <span class="color-search-label"><BgColorsOutlined /> 按主色调搜索：</span>
        <color-picker
          format="hex"
          @pureColorChange="handleColorSearch"
          shape="square"
          class="custom-color-picker"
        />
        <span class="color-search-tip">（选取颜色后，按照颜色相近的图片排序）</span>
      </div>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card class="content-card" :bordered="false">
      <PictureList
        :canEdit="canEditPicture"
        :canDelete="canDeletePicture"
        :dataList="pictureList"
        :loading="loading"
        showOp
        :onReload="fetchPictureVoList"
      />

      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="searchParams.current"
          v-model:pageSize="searchParams.pageSize"
          :total="total"
          :show-total="() => `图库总数 ${total} / ${space.maxCount}`"
          @change="onPageChange"
          show-size-changer
        />
      </div>
    </a-card>

    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="pictureList"
      :onSuccess="handleBatchEditPictureSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '../utils'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import {
  EditOutlined, BarChartOutlined, TeamOutlined,
  CloudUploadOutlined, SearchOutlined, BgColorsOutlined
} from '@ant-design/icons-vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import {
  SPACE_ROLE_ENUM,
  SPACE_ROLE_MAP,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP,
  SPACE_PERMISSION_ENUM
} from '../constants/space.ts'
import { SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons-vue'
import router from "@/router";

// 🚀 核心修复：通过权限列表【反推】当前用户的角色勋章
const currentRoleInfo = computed(() => {
  const perms = space.value.permissionList ?? [];

  // 1. 如果有【管理空间用户】的权限，那绝对是最高级别的“管理员”
  if (perms.includes(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)) {
    return { text: SPACE_ROLE_MAP[SPACE_ROLE_ENUM.ADMIN], color: 'orange' };
  }
  // 2. 如果没有管理权限，但有【编辑图片】的权限，那就是“编辑者”
  if (perms.includes(SPACE_PERMISSION_ENUM.PICTURE_EDIT)) {
    return { text: SPACE_ROLE_MAP[SPACE_ROLE_ENUM.EDITOR], color: 'blue' };
  }
  // 3. 如果啥高级权限都没，保底就是个“浏览者”
  return { text: SPACE_ROLE_MAP[SPACE_ROLE_ENUM.VIEWER], color: 'green' };
})

const props = defineProps<{ id: string | number }>()
const space = ref<API.SpaceVO>({})

// 计算容量百分比
const usagePercent = computed(() => {
  if (!space.value.maxSize || space.value.maxSize === 0) return 0
  const percent = (space.value.totalSize * 100) / space.value.maxSize
  return Number(percent.toFixed(1))
})

// 根据容量百分比动态变色（红黄绿）
const getStorageColor = (percent: number) => {
  if (percent >= 90) return '#ff4d4f' // 爆仓红
  if (percent >= 70) return '#faad14' // 预警黄
  return '#10b981' // 健康绿
}


function createPermissionChecker(permission: string) {
  return computed(() => (space.value.permissionList ?? []).includes(permission))
}

const canManageSpaceUser = computed(() => {
  const isTeamSpace = space.value.spaceType === SPACE_TYPE_ENUM.TEAM
  const hasPermission = (space.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
  return isTeamSpace && hasPermission
})
const canAnalyzeSpace = computed(() => {
  return (space.value.permissionList ?? []).includes(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
})
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

const pictureList = ref([])
const total = ref(0)
const loading = ref(true)

const searchParams = ref<API.QueryPictureRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchPictureVoList()
}

const fetchPictureVoList = async () => {
  loading.value = true
  const params = { spaceId: props.id, ...searchParams.value }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    pictureList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

const handleSearch = (newSearchParams: API.QueryPictureRequest) => {
  searchParams.value = { ...searchParams.value, ...newSearchParams, current: 1 }
  fetchPictureVoList()
}

onMounted(() => {
  fetchSpaceDetail()
  fetchPictureVoList()
})

const handleColorSearch = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({ picColor: color, spaceId: props.id })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    pictureList.value = data
    total.value = data.length
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

const batchEditPictureModalRef = ref()
const handleBatchEditPictureSuccess = () => fetchPictureVoList()
const openBatchEditModal = () => batchEditPictureModalRef.value?.openModal()

watch(() => props.id, () => {
  fetchSpaceDetail()
  fetchPictureVoList()
})
</script>

<style scoped>
#spaceDetailPage {
  padding: 0 4px;
}

/* 顶部 Dashboard Banner */
.dashboard-banner {
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}

.space-info-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.usage-circle {
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.space-text-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.space-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.type-tag {
  font-size: 14px;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 600;
  transform: translateY(-2px);
}

.space-stats {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

.highlight {
  color: #3b82f6;
  font-size: 16px;
  font-family: monospace;
}

/* 按钮样式 */
.modern-btn {
  border-radius: 8px;
  font-weight: 600;
  height: 40px;
  display: flex;
  align-items: center;
}

.upload-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

/* 检索与列表卡片 */
.filter-card, .content-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #334155;
}

/* 颜色搜索区整合 */
.color-search-area {
  display: flex;
  align-items: center;
  background: #f8fafc;
  padding: 12px 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.color-search-label {
  font-weight: 600;
  color: #475569;
  margin-right: 12px;
}

.color-search-tip {
  color: #94a3b8;
  font-size: 13px;
  margin-left: 12px;
}

.custom-color-picker {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 分页居中优化 */
.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}
</style>
