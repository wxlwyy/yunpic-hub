<template>
  <div id="homePage">
    <div class="search-hero">
      <div class="search-bar">
        <a-input-search
          v-model:value="searchParams.searchText"
          placeholder="探索你的视觉灵感..."
          enter-button="搜 索"
          size="large"
          @search="doSearch"
        />
      </div>
    </div>

    <div class="filter-wrapper">
      <a-tabs
        v-model:activeKey="selectedCategory"
        @change="doSearch"
        class="category-tabs"
      >
        <a-tab-pane key="all" tab="✨ 全部" />
        <a-tab-pane v-for="category in categoryList" :key="category" :tab="category" />
      </a-tabs>

      <div class="tag-container">
        <span class="tag-label">
          <FilterOutlined style="margin-right: 4px" /> 筛选标签：
        </span>
        <a-space :size="[12, 12]" wrap>
          <a-checkable-tag
            v-for="(tag, index) in tagList"
            :key="tag"
            v-model:checked="selectedTagList[index]"
            class="custom-checkable-tag"
            @change="doSearch"
          >
            {{ tag }}
          </a-checkable-tag>
        </a-space>
      </div>
    </div>

    <div class="picture-list-area">
      <PictureList :dataList="dataList" :loading="loading" />

      <div class="pagination-wrapper" v-if="total > 0">
        <a-pagination
          v-model:current="searchParams.current"
          v-model:pageSize="searchParams.pageSize"
          :total="Number(total)"
          @change="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { FilterOutlined } from '@ant-design/icons-vue';
import { onMounted, reactive, ref } from 'vue'
import { listPictureTagCategoryUsingGet, listPictureVoByPageUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import PictureList from '@/components/PictureList.vue'

const router = useRouter()
const route = useRoute()

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 初始搜索条件：强制转换为 plain object 且确保类型正确
const searchParams = reactive<API.QueryPictureRequest>({
  current: Number(route.query.current) || 1,
  pageSize: Number(route.query.pageSize) || 12,
  searchText: (route.query.searchText as string) || '',
})

const selectedCategory = ref<string>((route.query.category as string) || 'all')
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 更新 URL 的逻辑（把复杂对象转化成纯字符串，防止路由报错）
const updateUrl = () => {
  const tags = tagList.value.filter((_, i) => selectedTagList.value[i])
  router.replace({
    query: {
      ...searchParams,
      category: selectedCategory.value !== 'all' ? selectedCategory.value : undefined,
      tags: tags.length > 0 ? tags.join(',') : undefined,
    }
  })
}

// 获取核心数据
const fetchData = async () => {
  loading.value = true
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // 安全检查：确保 selectedTagList 已经初始化
  if (selectedTagList.value.length > 0) {
    selectedTagList.value.forEach((useTag, index) => {
      if (useTag && tagList.value[index]) {
        params.tags.push(tagList.value[index])
      }
    })
  }

  try {
    const res = await listPictureVoByPageUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('数据获取失败：' + res.data.message)
    }
  } catch (error) {
    message.error('请求接口出错')
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  updateUrl()
  fetchData()
}

const doSearch = () => {
  searchParams.current = 1
  updateUrl()
  fetchData()
}

const getCategoryTagOptions = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      categoryList.value = res.data.data.categoryList ?? []
      tagList.value = res.data.data.tagList ?? []

      // 初始化标签勾选状态
      const urlTags = (route.query.tags as string)?.split(',') || []
      selectedTagList.value = tagList.value.map(tag => urlTags.includes(tag))

      // 等标签初始化完后再拉取数据，防止标签过滤失效
      await fetchData()
    }
  } catch (error) {
    message.error('加载选项失败')
  }
}

onMounted(() => {
  getCategoryTagOptions()
})
</script>

<style scoped>
/* 此处保留刚才那套精美的 CSS 即可 */
.search-hero { padding: 40px 0 24px; text-align: center; }
.search-bar { max-width: 600px; margin: 0 auto; transition: transform 0.3s; }
.filter-wrapper { background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(10px); padding: 16px 24px; border-radius: 20px; border: 1px solid rgba(0, 0, 0, 0.05); margin-bottom: 32px; }
:deep(.category-tabs .ant-tabs-nav) { margin-bottom: 16px !important; }
:deep(.category-tabs .ant-tabs-tab) { font-size: 16px !important; font-weight: 500; }
.tag-container { display: flex; align-items: flex-start; padding-top: 8px; border-top: 1px dashed rgba(0, 0, 0, 0.08); }
.tag-label { flex-shrink: 0; color: #64748b; font-weight: 600; margin-top: 4px; font-size: 13px; }
.custom-checkable-tag { padding: 4px 12px !important; font-size: 13px !important; border-radius: 8px !important; background: #f1f5f9 !important; border: 1px solid transparent !important; transition: all 0.2s !important; }
:deep(.ant-tag-checkable-checked) { background: #3b82f6 !important; color: #ffffff !important; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.pagination-wrapper { margin-top: 40px; display: flex; justify-content: flex-end; }
</style>
