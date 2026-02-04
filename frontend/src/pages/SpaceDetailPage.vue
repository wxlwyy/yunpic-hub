<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（{{ SPACE_TYPE_MAP[space.spaceType] }}）</h2>
      <a-space size="middle">
        <a-button
          v-if="canUploadPicture"
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
          target="_blank"
        >
          + 创建图片
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage/${id}`"
          target="_blank"
        >
          成员管理
        </a-button>
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
          target="_blank"
        >
          空间分析
        </a-button>
        <a-button v-if="canEditPicture" :icon="h(EditOutlined)" @click="openBatchEditModal"> 批量编辑</a-button>
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            type="circle"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
            :size="42"
          />
        </a-tooltip>
      </a-space>
    </a-flex>
    <PictureSearchForm :onSearch="handleSearch" />
    <!-- 按颜色搜索 -->
    <a-form-item label="按颜色搜索" style="margin-top: 16px">
      <color-picker format="hex" @pureColorChange="handleColorSearch" />
    </a-form-item>
    <!-- 图片列表 -->
    <PictureList :canEdit="canEditPicture" :canDelete="canDeletePicture"
      :dataList="pictureList" :loading="loading" showOp :onReload="fetchPictureVoList" />
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
      @change="onPageChange"
    />
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="pictureList"
      :onSuccess="handleBatchEditPictureSuccess"
    />
  </div>
</template>

<script setup lang="ts">
//数据
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
import { EditOutlined, BarChartOutlined, TeamOutlined } from '@ant-design/icons-vue'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_MAP } from '../constants/space.ts'

const props = defineProps<{
  id: string | number
}>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// 获取空间详情
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

// 数据
const pictureList = ref([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.QueryPictureRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 分页参数
const onPageChange = (page, pageSize) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchPictureVoList()
}

// 获取数据
const fetchPictureVoList = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    pictureList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 搜索
const handleSearch = (newSearchParams: API.QueryPictureRequest) => {
  console.log('new', newSearchParams)
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchPictureVoList()
}

// 页面加载时请求一次
onMounted(() => {
  fetchPictureVoList()
})

const handleColorSearch = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    pictureList.value = data
    total.value = data.length
    loading.value = false
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 批量编辑弹窗引用
const batchEditPictureModalRef = ref()

// 批量编辑成功后，刷新数据，以 handle 或 on 开头的函数名，通常用于处理事件或回调（比如 handleSubmit, handleClick, onChange）
const handleBatchEditPictureSuccess = () => {
  fetchPictureVoList()
}

// 打开批量编辑弹窗
const openBatchEditModal = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

//空间 id 改变时，必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchPictureVoList()
  },
)
</script>

<style scoped></style>
