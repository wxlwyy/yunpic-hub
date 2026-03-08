<template>
  <div id="userManagePage">
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="用户账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号搜索" allow-clear class="modern-input">
            <template #prefix><UserOutlined style="color: #bfbfbf" /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear class="modern-input">
            <template #prefix><IdcardOutlined style="color: #bfbfbf" /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" class="search-btn">
              <template #icon><SearchOutlined /></template>
              精准搜索
            </a-button>
            <a-button @click="resetSearch" class="reset-btn">重置条件</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card :bordered="false" class="table-card">
      <template #title>
        <div class="table-header">
          <span class="title-text"><TeamOutlined /> 系统用户目录</span>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :scroll="{ x: 1000 }"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar
              :src="record.userAvatar"
              :size="48"
              class="user-avatar-shadow"
            >
              <template #icon><UserOutlined /></template>
            </a-avatar>
          </template>

          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="purple" class="role-tag">
              <template #icon><CrownOutlined /></template> 管理员
            </a-tag>
            <a-tag v-else-if="record.userRole === 'vip'" color="gold" class="role-tag">
              <template #icon><StarOutlined /></template> VIP用户
            </a-tag>
            <a-tag v-else color="blue" class="role-tag">
              <template #icon><UserOutlined /></template> 普通用户
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'userProfile'">
            <a-typography-paragraph
              :ellipsis="{ rows: 1, tooltip: record.userProfile }"
              style="margin-bottom: 0; max-width: 180px"
            >
              {{ record.userProfile || '这家伙很懒，什么都没留下' }}
            </a-typography-paragraph>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="time-text">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              title="确定要注销该用户吗？此操作不可恢复！"
              ok-text="确定删除"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="primary" danger size="small" shape="round">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import {
  SearchOutlined, UserOutlined, IdcardOutlined,
  TeamOutlined, CrownOutlined, StarOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUserUsingPost, getListUserVoByPageUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  { title: '系统ID', dataIndex: 'id', width: 120, fixed: 'left' },
  { title: '登录账号', dataIndex: 'userAccount', width: 140 },
  { title: '用户昵称', dataIndex: 'userName', width: 140 },
  { title: '当前头像', dataIndex: 'userAvatar', width: 100, align: 'center' },
  { title: '个人简介', dataIndex: 'userProfile', width: 200 },
  { title: '系统角色', dataIndex: 'userRole', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '危险操作', key: 'action', width: 120, fixed: 'right', align: 'center' },
]

const dataList = ref<API.UserVO[]>([])
const total = ref(0)

const searchParams = reactive<API.QueryUserRequest>({
  current: 1,
  pageSize: 10,
})

const fetchData = async () => {
  const res = await getListUserVoByPageUsingPost({ ...searchParams })
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
  showTotal: (total: number) => `系统共计 ${total} 名用户`,
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

// 修复：必须赋值为 undefined，防止 MyBatis 拿着空字符串 "" 去数据库查导致查不出人
const resetSearch = () => {
  searchParams.userAccount = undefined
  searchParams.userName = undefined
  doSearch()
}

const doDelete = async (id: string) => {
  if (!id) return
  const res = await deleteUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('用户数据已从库中物理清除')
    fetchData()
  } else {
    message.error('移除失败：' + res.data.message)
  }
}
</script>

<style scoped>
#userManagePage {
  padding: 0 4px;
}

/* 搜索卡片 */
.search-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.modern-input {
  border-radius: 8px;
  min-width: 180px;
}

.search-btn {
  border-radius: 8px;
  padding-left: 20px;
  padding-right: 20px;
  font-weight: 600;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.table-header {
  display: flex;
  align-items: center;
}

.title-text {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
}

.user-avatar-shadow {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
}

.role-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 600;
}

.time-text {
  color: #64748b;
  font-size: 14px;
  font-family: monospace;
}

/* 覆盖 AntD 表格样式 */
:deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}
</style>
