<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between" align="center" class="page-header">
      <h2 class="page-title"><PictureOutlined /> 图片资源管理</h2>
      <a-space size="middle">
        <a-button type="primary" href="/add_picture" target="_blank" class="action-btn">
          <template #icon><PlusOutlined /></template>
          上传新图片
        </a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost class="action-btn">
          <template #icon><CloudUploadOutlined /></template>
          批量导入
        </a-button>
      </a-space>
    </a-flex>

    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="关键字">
          <a-input v-model:value="searchParams.searchText" placeholder="搜图片名或简介" allow-clear class="modern-input" />
        </a-form-item>
        <a-form-item label="分类类型">
          <a-input v-model:value="searchParams.category" placeholder="如：海报、插画" allow-clear class="modern-input" />
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-select
            v-model:value="searchParams.tags"
            mode="tags"
            placeholder="输入标签筛选"
            style="min-width: 180px"
            class="modern-input"
            allowClear
          />
        </a-form-item>
        <a-form-item label="审核状态" name="reviewStatus">
          <a-select
            v-model:value="searchParams.reviewStatus"
            :options="PIC_REVIEW_STATUS_OPTIONS"
            placeholder="全部状态"
            style="min-width: 140px"
            class="modern-input"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" class="search-btn">
              <template #icon><SearchOutlined /></template> 筛选
            </a-button>
            <a-button @click="resetSearch" class="reset-btn">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card :bordered="false" class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :scroll="{ x: 1500 }"
        @change="doTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">

          <template v-if="column.dataIndex === 'url'">
            <div class="image-preview-wrapper">
              <a-image
                :src="record.url"
                :width="120"
                :height="80"
                style="object-fit: cover; border-radius: 8px;"
                class="hover-zoom"
              />
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'tags'">
            <div class="tags-container">
              <a-tag
                v-for="tag in JSON.parse(record.tags || '[]')"
                :key="tag"
                color="cyan"
                style="margin-bottom: 4px;"
              >
                {{ tag }}
              </a-tag>
            </div>
          </template>

          <template v-if="column.dataIndex === 'picInfo'">
            <div class="pic-info-grid">
              <span class="info-item" title="格式"><FileOutlined /> {{ record.picFormat }}</span>
              <span class="info-item" title="大小"><DatabaseOutlined /> {{ (record.picSize / 1024).toFixed(1) }}KB</span>
              <span class="info-item" title="分辨率"><ExpandOutlined /> {{ record.picWidth }}x{{ record.picHeight }}</span>
              <span class="info-item" title="比例"><AppstoreOutlined /> {{ record.picScale }}</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="time-text">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'editTime'">
            <div class="time-text">{{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm') }}</div>
          </template>

          <template v-if="column.dataIndex === 'reviewMessage'">
            <div class="review-status-box">
              <a-tag :color="getReviewStatusColor(record.reviewStatus)">
                {{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}
              </a-tag>
              <div class="review-detail" v-if="record.reviewMessage">
                <span class="label">原因:</span> {{ record.reviewMessage }}
              </div>
              <div class="review-detail" v-if="record.reviewerId">
                <span class="label">处理人:</span> {{ record.reviewerId }}
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space size="small" direction="vertical" style="width: 100%">
              <a-space size="small">
                <a-button
                  v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
                  type="primary"
                  size="small"
                  @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
                >
                  通过
                </a-button>
                <a-button
                  v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
                  type="primary"
                  danger
                  size="small"
                  @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
                >
                  拒绝
                </a-button>
              </a-space>

              <a-space size="small">
                <a-button size="small" :href="`/add_picture?id=${record.id}`" target="_blank">
                  编辑
                </a-button>
                <a-popconfirm
                  title="确认删除这张图片？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="doDelete(record.id)"
                >
                  <a-button size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import {
  SearchOutlined, PictureOutlined, PlusOutlined, CloudUploadOutlined,
  FileOutlined, DatabaseOutlined, ExpandOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deletePictureUsingPost, listPictureByPageUsingPost, reviewPictureUsingPost } from '@/api/pictureController.ts'
import { PIC_REVIEW_STATUS_ENUM, PIC_REVIEW_STATUS_MAP, PIC_REVIEW_STATUS_OPTIONS } from '../../constants/picture.ts'

// 列定义保持不变，只调整了部分列宽保证横向排版
const columns = [
  { title: 'ID', dataIndex: 'id', width: 80, fixed: 'left' },
  { title: '图片预览', dataIndex: 'url', width: 140, fixed: 'left' },
  { title: '名称', dataIndex: 'name', width: 150 },
  { title: '审核详情', dataIndex: 'reviewMessage', width: 200 },
  { title: '类型', dataIndex: 'category', width: 100 },
  { title: '标签', dataIndex: 'tags', width: 180 },
  { title: '参数信息', dataIndex: 'picInfo', width: 200 },
  { title: '简介', dataIndex: 'introduction', width: 150, ellipsis: true },
  { title: '发布者ID', dataIndex: 'userId', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 140 },
  { title: '修改时间', dataIndex: 'editTime', width: 140 },
  { title: '操作区', key: 'action', width: 140, fixed: 'right', align: 'center' },
]

const dataList = ref<API.Picture[]>([])
const total = ref(0)

const searchParams = reactive<API.QueryPictureRequest>({
  current: 1,
  pageSize: 10,
})

const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
    nullSpaceId: true,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('数据获取失败：' + res.data.message)
  }
}

onMounted(() => fetchData())

const pagination = computed(() => ({
  current: searchParams.current ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `资源库共 ${total} 张图片`,
}))

const doTableChange = (pag: any) => {
  searchParams.current = pag.current
  searchParams.pageSize = pag.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

// 新增真实的重置方法（解决搜索条件卡死问题）
const resetSearch = () => {
  searchParams.searchText = undefined
  searchParams.category = undefined
  searchParams.tags = undefined
  searchParams.reviewStatus = undefined
  doSearch()
}

const doDelete = async (id: string) => {
  if (!id) return
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('图片已成功删除')
    fetchData()
  } else {
    message.error('删除失败：' + res.data.message)
  }
}

const handleReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage = reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员审核通过' : '违规图片，管理员拒绝'
  const res = await reviewPictureUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核处理完成')
    fetchData()
  } else {
    message.error('操作异常：' + res.data.message)
  }
}

// 辅助方法：动态获取审核状态的颜色
const getReviewStatusColor = (status: number) => {
  if (status === PIC_REVIEW_STATUS_ENUM.PASS) return 'green'
  if (status === PIC_REVIEW_STATUS_ENUM.REJECT) return 'red'
  return 'orange' // 待审核
}
</script>

<style scoped>
#pictureManagePage { padding: 0 4px; }

/* 页面头部 */
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 800; color: #1e293b; margin: 0; }
.action-btn { border-radius: 8px; font-weight: 600; }

/* 卡片样式 */
.search-card, .table-card {
  background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.modern-input { border-radius: 8px; }
.search-btn { border-radius: 8px; padding: 0 20px; font-weight: 600; }
.reset-btn { border-radius: 8px; margin-left: 8px; }

/* 图片预览优化 */
.image-preview-wrapper {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  display: inline-block;
  background: #fafafa;
  /* 👇 加上下面这三行，修复 GPU 渲染幽灵残影的 Bug */
  transform: translateZ(0);
  backface-visibility: hidden;
  perspective: 1000px;
}
.hover-zoom { transition: transform 0.3s ease; }
.hover-zoom:hover { transform: scale(1.05); }

/* 图片参数网格 */
.pic-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 8px;
  font-size: 12px;
  color: #64748b;
}
.info-item {
  background: #f8fafc;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 标签容器 */
.tags-container {
  max-width: 180px;
  display: flex;
  flex-wrap: wrap;
}

/* 审核信息区 */
.review-status-box { font-size: 12px; }
.review-detail { margin-top: 4px; color: #64748b; }
.review-detail .label { font-weight: 600; color: #94a3b8; }

/* 时间排版 */
.time-text { color: #64748b; font-size: 13px; font-family: monospace; }

/* 覆盖 AntD 表头 */
:deep(.ant-table-thead > tr > th) {
  background: #f8fafc; color: #475569; font-weight: 700;
}
</style>
