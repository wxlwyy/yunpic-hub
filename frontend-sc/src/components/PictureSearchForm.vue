<template>
  <div class="search-form-container">
    <a-form :model="searchParams" @finish="doSearch" class="refined-search-form">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="关键词">
            <a-input
              v-model:value="searchParams.searchText"
              placeholder="搜索名称或简介..."
              allow-clear
              size="large"
            >
              <template #prefix><search-outlined style="color: #bfbfbf" /></template>
            </a-input>
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="分类">
            <a-auto-complete
              v-model:value="searchParams.category"
              :options="categoryOptions"
              placeholder="请选择分类"
              allowClear
              size="large"
            />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="标签">
            <a-select
              v-model:value="searchParams.tags"
              :options="tagOptions"
              mode="tags"
              placeholder="选择标签"
              allowClear
              size="large"
            />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <div class="action-buttons">
            <a-button type="primary" html-type="submit" size="large" class="search-btn">
              <template #icon><search-outlined /></template> 搜索
            </a-button>
            <a-button size="large" @click="doClear" class="reset-btn">
              <template #icon><reload-outlined /></template> 重置
            </a-button>
            <a-button type="link" @click="isExpanded = !isExpanded" class="expand-btn">
              {{ isExpanded ? '收起' : '高级' }}
              <up-outlined v-if="isExpanded" />
              <down-outlined v-else />
            </a-button>
          </div>
        </a-col>
      </a-row>

      <transition name="expand">
        <div v-show="isExpanded" class="advanced-section">
          <a-divider dashed style="margin: 16px 0" />
          <a-row :gutter="[16, 24]">
            <a-col :xs="24" :md="12" :lg="10">
              <a-form-item label="编辑日期">
                <a-range-picker
                  v-model:value="dateRange"
                  style="width: 100%"
                  show-time
                  format="YYYY-MM-DD HH:mm"
                  :presets="rangePresets"
                  @change="onRangeChange"
                  :placeholder="['起始时间', '结束时间']"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6" :lg="4">
              <a-form-item label="最小宽度">
                <a-input-number v-model:value="searchParams.picWidth" style="width: 100%" placeholder="px" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6" :lg="4">
              <a-form-item label="最小高度">
                <a-input-number v-model:value="searchParams.picHeight" style="width: 100%" placeholder="px" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6" :lg="3">
              <a-form-item label="格式">
                <a-input v-model:value="searchParams.picFormat" placeholder="webp/jpg" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6" :lg="3">
              <a-form-item label="重命名">
                <a-input v-model:value="searchParams.name" placeholder="名称关键字" />
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </transition>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SearchOutlined, ReloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons-vue'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'

interface Props {
  onSearch?: (searchParams: API.QueryPictureRequest) => void
}

const props = defineProps<Props>()
const isExpanded = ref(false)
const searchParams = reactive<API.QueryPictureRequest>({})
const dateRange = ref<any>(null)

// 🚀 补齐并增强的预设日期
const rangePresets = [
  { label: '最近 7 天', value: [dayjs().subtract(7, 'd'), dayjs()] },
  { label: '最近 14 天', value: [dayjs().subtract(14, 'd'), dayjs()] },
  { label: '最近 30 天', value: [dayjs().subtract(30, 'd'), dayjs()] },
  { label: '最近 90 天', value: [dayjs().subtract(90, 'd'), dayjs()] },
  { label: '最近一年', value: [dayjs().subtract(1, 'y'), dayjs()] },
]

const doSearch = () => {
  props.onSearch?.(searchParams)
}

const categoryOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((t: string) => ({ value: t, label: t }))
    categoryOptions.value = (res.data.data.categoryList ?? []).map((c: string) => ({ value: c, label: c }))
  }
}

onMounted(() => getTagCategoryOptions())

const onRangeChange = (dates: any[]) => {
  if (dates && dates.length >= 2) {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  } else {
    dateRange.value = null
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
  }
}

const doClear = () => {
  Object.keys(searchParams).forEach((key) => delete searchParams[key])
  dateRange.value = null
  props.onSearch?.(searchParams)
}
</script>

<style scoped>
.search-form-container {
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
}

.search-btn {
  border-radius: 8px;
  background: #1890ff;
  flex: 1;
}

.reset-btn {
  border-radius: 8px;
}

.expand-btn {
  padding: 0 4px;
  color: #8c8c8c;
  font-size: 13px;
}

.expand-enter-active, .expand-leave-active {
  transition: all 0.3s ease;
  max-height: 300px;
  overflow: hidden;
}
.expand-enter-from, .expand-leave-to {
  max-height: 0;
  opacity: 0;
}

:deep(.ant-form-item) {
  margin-bottom: 0;
}

:deep(.ant-form-item-label) {
  font-weight: 600;
  color: #4b5563;
  padding-bottom: 4px;
}
</style>
