<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between" align="center" class="page-header">
      <h2 class="page-title"><CloudServerOutlined /> 空间资源管理</h2>
      <a-space size="middle">
        <a-button type="primary" @click="router.push('/add_space')" class="action-btn create-btn">
          <template #icon><PlusOutlined /></template>
          开通新空间
        </a-button>
        <a-button ghost type="primary" @click="router.push('/space_analyze?queryPublic=1')" class="action-btn">
          <template #icon><PieChartOutlined /></template>
          公共图库分析
        </a-button>
        <a-button ghost type="primary" @click="router.push('/space_analyze?queryAll=1')" class="action-btn">
          <template #icon><GlobalOutlined /></template>
          全站空间分析
        </a-button>
      </a-space>
    </a-flex>

    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="空间名称" name="spaceName">
          <a-input v-model:value="searchParams.spaceName" placeholder="输入名称搜索" allow-clear class="modern-input" />
        </a-form-item>
        <a-form-item label="空间级别" name="spaceLevel">
          <a-select
            v-model:value="searchParams.spaceLevel"
            :options="SPACE_LEVEL_OPTIONS"
            placeholder="全部分类"
            style="min-width: 140px"
            class="modern-input"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="空间类别" name="spaceType">
          <a-select
            v-model:value="searchParams.spaceType"
            :options="SPACE_TYPE_OPTIONS"
            placeholder="全部类型"
            style="min-width: 140px"
            class="modern-input"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="归属者ID" name="userId">
          <a-input v-model:value="searchParams.userId" placeholder="输入用户ID" allow-clear class="modern-input">
            <template #prefix><UserOutlined style="color: #bfbfbf" /></template>
          </a-input>
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
        :scroll="{ x: 1200 }"
        @change="doTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">

          <template v-if="column.dataIndex === 'spaceLevel'">
            <a-tag :color="getSpaceLevelColor(record.spaceLevel)" class="custom-tag">
              {{ SPACE_LEVEL_MAP[record.spaceLevel] }}
            </a-tag>
          </template>

          <template v-if="column.dataIndex === 'spaceType'">
            <a-tag :color="record.spaceType === 1 ? 'cyan' : 'blue'" class="custom-tag">
              {{ SPACE_TYPE_MAP[record.spaceType] }}
            </a-tag>
          </template>

          <template v-if="column.dataIndex === 'spaceUseInfo'">
            <div class="usage-container">
              <div class="usage-item">
                <div class="usage-header">
                  <span class="usage-title">存储容量</span>
                  <span class="usage-text">{{ formatSize(record.totalSize) }} / {{ formatSize(record.maxSize) }}</span>
                </div>
                <a-progress
                  :percent="calculatePercent(record.totalSize, record.maxSize)"
                  size="small"
                  :stroke-color="getStorageColor(record.totalSize, record.maxSize)"
                  :show-info="false"
                />
              </div>
              <div class="usage-item">
                <div class="usage-header">
                  <span class="usage-title">图片数量</span>
                  <span class="usage-text">{{ record.totalCount }} / {{ record.maxCount }}</span>
                </div>
                <a-progress
                  :percent="calculatePercent(record.totalCount, record.maxCount)"
                  size="small"
                  stroke-color="#10b981"
                  :show-info="false"
                />
              </div>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="time-text">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'editTime'">
            <div class="time-text">{{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm') }}</div>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="primary" size="small" ghost @click="router.push({ path: '/space_analyze', query: { spaceId: record.id } })">
                <template #icon><BarChartOutlined /></template> 分析
              </a-button>
              <a-button size="small" @click="router.push({ path: '/add_space', query: { id: record.id } })">
                <template #icon><EditOutlined /></template> 编辑
              </a-button>

              <a-popconfirm title="确认销毁此空间？内部图片将一并删除！" ok-text="确认销毁" cancel-text="取消" @confirm="doDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import {
  CloudServerOutlined, PlusOutlined, PieChartOutlined, GlobalOutlined,
  SearchOutlined, UserOutlined, BarChartOutlined, EditOutlined
} from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deleteSpaceUsingPost, listSpaceByPageUsingPost } from '@/api/spaceController.ts'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS, SPACE_TYPE_MAP, SPACE_TYPE_OPTIONS } from '../../constants/space.ts'
import { formatSize } from '../../utils'
import router from "@/router";

const columns = [
  { title: '空间ID', dataIndex: 'id', width: 100, fixed: 'left' },
  { title: '空间名称', dataIndex: 'spaceName', width: 160 },
  { title: '级别', dataIndex: 'spaceLevel', width: 100 },
  { title: '类别', dataIndex: 'spaceType', width: 100 },
  { title: '资源使用率', dataIndex: 'spaceUseInfo', width: 280 },
  { title: '归属者ID', dataIndex: 'userId', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 140 },
  { title: '最后活跃', dataIndex: 'editTime', width: 140 },
  { title: '操作选项', key: 'action', width: 240, fixed: 'right', align: 'center' },
]

const dataList = ref<API.Space[]>([])
const total = ref(0)

const searchParams = reactive<API.QuerySpaceRequest>({
  current: 1,
  pageSize: 10,
})

const fetchData = async () => {
  const res = await listSpaceByPageUsingPost({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('数据拉取失败：' + res.data.message)
  }
}

onMounted(() => fetchData())

const pagination = computed(() => ({
  current: searchParams.current ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共运营 ${total} 个空间节点`,
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

// 补上完美的重置搜索逻辑
const resetSearch = () => {
  searchParams.spaceName = undefined
  searchParams.spaceLevel = undefined
  searchParams.spaceType = undefined
  searchParams.userId = undefined
  doSearch()
}

const doDelete = async (id: string) => {
  if (!id) return
  const res = await deleteSpaceUsingPost({ id })
  if (res.data.code === 0) {
    message.success('空间已被成功销毁')
    fetchData()
  } else {
    message.error('销毁失败：' + res.data.message)
  }
}

// ================= UI 辅助方法 =================

// 计算进度条百分比
const calculatePercent = (current: number, max: number) => {
  if (!max || max === 0) return 0
  const percent = (current / max) * 100
  return Number(percent.toFixed(1))
}

// 存储快满时，进度条变红变黄预警
const getStorageColor = (current: number, max: number) => {
  const percent = calculatePercent(current, max)
  if (percent > 90) return '#ff4d4f' // 红色危险
  if (percent > 70) return '#faad14' // 黄色警告
  return '#3b82f6' // 正常蓝色
}

// 空间级别给不同颜色 (假定：0普通，1专业，2旗舰)
const getSpaceLevelColor = (level: number) => {
  if (level === 2) return 'gold'    // 旗舰版金色
  if (level === 1) return 'purple'  // 专业版紫色
  return 'default'                  // 基础版默认
}
</script>

<style scoped>
#spaceManagePage { padding: 0 4px; }

/* 页面头部 */
.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 800; color: #1e293b; margin: 0; }
.action-btn { border-radius: 8px; font-weight: 600; }
.create-btn { background: linear-gradient(135deg, #10b981 0%, #059669 100%); border: none; }
.create-btn:hover { background: linear-gradient(135deg, #34d399 0%, #10b981 100%); }

/* 卡片样式 */
.search-card, .table-card {
  background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.modern-input { border-radius: 8px; }
.search-btn { border-radius: 8px; padding: 0 20px; font-weight: 600; }
.reset-btn { border-radius: 8px; margin-left: 8px; }

/* 标签优化 */
.custom-tag {
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 600;
}

/* 进度条看板优化 */
.usage-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 16px;
}
.usage-item {
  display: flex;
  flex-direction: column;
}
.usage-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
  font-size: 12px;
}
.usage-title { color: #64748b; font-weight: 600; }
.usage-text { color: #1e293b; font-family: monospace; }

/* 时间排版 */
.time-text { color: #64748b; font-size: 13px; font-family: monospace; }

/* 覆盖 AntD 表头 */
:deep(.ant-table-thead > tr > th) {
  background: #f8fafc; color: #475569; font-weight: 700;
}
</style>
