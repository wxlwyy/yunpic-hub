<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between">
      <h2>空间管理</h2>
      <a-space>
        <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px"></div>
    <!--  搜索框  -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="空间名称" name="spaceName">
        <a-input v-model:value="searchParams.spaceName" placeholder="请输入空间名称" allow-clear />
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="searchParams.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="用户 id" name="userId">
        <a-input v-model:value="searchParams.userId" placeholder="请输入用户 id" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!--  表格  -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 空间级别 -->
        <template v-if="column.dataIndex === 'spaceLevel'">
          <a-tag>{{ SPACE_LEVEL_MAP[record.spaceLevel] }}</a-tag>
        </template>
        <!-- 使用情况 （这里的formatSize方法涉及到了单位换算，将后端的字节数据转为了MB数据）-->
        <template v-if="column.dataIndex === 'spaceUseInfo'">
          <div>大小：{{ formatSize(record.totalSize) }} / {{ formatSize(record.maxSize) }}</div>
          <div>数量：{{ record.totalCount }} / {{ record.maxCount }}</div>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" :href="`/add_space?id=${record.id}`" target="_blank">
              编辑
            </a-button>
            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deleteSpaceUsingPost, listSpaceByPageUsingPost } from '@/api/spaceController.ts'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS } from '../../constants/space.ts'
import { formatSize } from '../../utils'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '空间名称',
    dataIndex: 'spaceName',
  },
  {
    title: '空间级别',
    dataIndex: 'spaceLevel',
  },
  {
    title: '使用情况',
    dataIndex: 'spaceUseInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

//数据
const dataList = ref<API.Space[]>([])
const total = ref(0)

//搜索条件
const searchParams = reactive<API.QuerySpaceRequest>({
  current: 1,
  pageSize: 10,
})

//获取数据
const fetchData = async () => {
  const res = await listSpaceByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []   //dataList数据变化，页面图片数据立马刷新
    total.value = res.data.data.total ?? 0  //total变了，pagination这个计算属性重新，更新total
  } else {
    message.error('数据获取失败' + res.data.message)
  }
}

//页面挂载好之后获取数据
onMounted(() => {
  fetchData()
})

//分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,   //允许切换页码
    showTotal: (total) => `共${total}条`,
  }
})

//表格变化更新数据，点击表格下的页码或条数，表格自动将新的pagination对象传到这个方法的参数中
const doTableChange = (pagination: any) => {
  searchParams.current = pagination.current
  searchParams.pageSize = pagination.pageSize
  fetchData()
}

//搜索数据
const doSearch = () => {
  //重置页码
  searchParams.current = 1
  fetchData()
}

//根据id删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteSpaceUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    //重新获取数据
    fetchData()
  } else {
    message.error('删除失败' + res.data.message)
  }
}
</script>

<style scoped>
#spaceManagePage {
}
</style>
