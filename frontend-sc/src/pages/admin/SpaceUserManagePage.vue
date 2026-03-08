<template>
  <div id="spaceUserManagePage">
    <div class="page-header">
      <div class="title-section">
        <team-outlined class="header-icon" />
        <div class="text-group">
          <h2>空间成员管理</h2>
          <p class="subtitle">管理当前空间的协作权限与成员名单</p>
        </div>
      </div>
      <a-space>
        <a-button class="ghost-btn" @click="router.push(`/space/${props.spaceId}`)">
          <template #icon><left-outlined /></template> 返回团队空间
        </a-button>
      </a-space>
    </div>

    <a-card class="action-card" :bordered="false">
      <template #title>
        <span class="card-title"><user-add-outlined /> 邀请新成员</span>
      </template>
      <a-form layout="vertical" :model="formData" @finish="handleSubmit">
        <a-row :gutter="24" align="bottom">
          <a-col :xs="24" :md="16" :lg="10">
            <a-form-item label="用户 ID" name="userId" extra="请向成员索取其账号唯一标识 ID">
              <a-input
                v-model:value="formData.userId"
                placeholder="请输入 19 位用户 ID"
                style="width: 100%"
                size="large"
                allow-clear
              >
                <template #prefix><idcard-outlined style="color: rgba(0,0,0,0.25)" /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8" :lg="4">
            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block class="add-btn">
                添加至空间
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <div style="margin-bottom: 24px" />

    <a-card class="list-card" :bordered="false">
      <template #title>
        <div class="card-header-flex">
          <span class="card-title"><ordered-list-outlined /> 成员名单 ({{ dataList.length }})</span>
          <a-button type="link" @click="fetchData"><reload-outlined /> 刷新</a-button>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="false"
        class="custom-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userInfo'">
            <div class="user-info-cell">
              <a-avatar :src="record.userVO?.userAvatar" :size="40" class="avatar-shadow" />
              <div class="user-meta">
                <span class="user-name">{{ record.userVO?.userName || '神秘访客' }}</span>
                <span class="user-account">@{{ record.userVO?.userAccount }}</span>
              </div>
            </div>
          </template>

          <template v-if="column.dataIndex === 'spaceRole'">
            <a-select
              v-model:value="record.spaceRole"
              :options="SPACE_ROLE_OPTIONS"
              @change="(value) => editSpaceRole(value, record)"
              class="role-select"
              :style="{ color: getRoleColor(record.spaceRole) }"
            >
              <template #suffixIcon><down-outlined /></template>
            </a-select>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="time-text">{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              title="确定要将该成员从空间移除吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="link" danger class="delete-btn">
                <template #icon><user-delete-outlined /></template> 移除
              </a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  TeamOutlined, UserAddOutlined, OrderedListOutlined,
  ReloadOutlined, IdcardOutlined, UserDeleteOutlined,
  LeftOutlined, DownOutlined
} from '@ant-design/icons-vue'
import { SPACE_ROLE_OPTIONS, SPACE_ROLE_MAP } from '../../constants/space.ts'
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserVoUsingPost
} from '@/api/spaceUserController.ts'
import router from "@/router";

interface Props {
  spaceId: string
}
const props = defineProps<Props>()

const columns = [
  { title: '成员', dataIndex: 'userInfo' },
  { title: '权限角色', dataIndex: 'spaceRole', width: '180px' },
  { title: '加入时间', dataIndex: 'createTime' },
  { title: '操作', key: 'action', width: '100px' },
]

const dataList = ref<API.SpaceUserVO[]>([])

const fetchData = async () => {
  const spaceId = props.spaceId
  if (!spaceId) return
  const res = await listSpaceUserVoUsingPost({ spaceId })
  if (res.data.data) {
    dataList.value = res.data.data ?? []
  }
}

onMounted(() => fetchData())

const editSpaceRole = async (value, record) => {
  const res = await editSpaceUserUsingPost({ id: record.id, spaceRole: value })
  if (res.data.code === 0) {
    message.success('角色权限已更新')
  } else {
    message.error('更新失败：' + res.data.message)
  }
}

const doDelete = async (id: string) => {
  const res = await deleteSpaceUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('成员已移除')
    fetchData()
  }
}

const formData = reactive<API.SpaceUserAddRequest>({})
const handleSubmit = async () => {
  if (!formData.userId) return message.warning('请输入用户 ID')
  const res = await addSpaceUserUsingPost({ spaceId: props.spaceId, ...formData })
  if (res.data.code === 0) {
    message.success('成员添加成功')
    formData.userId = undefined
    fetchData()
  } else {
    message.error('添加失败：' + res.data.message)
  }
}

// 辅助：获取角色对应的颜色
const getRoleColor = (role: string) => {
  if (role === 'admin') return '#faad14'
  if (role === 'editor') return '#1890ff'
  return '#8c8c8c'
}
</script>

<style scoped>
#spaceUserManagePage {
  padding: 0 10px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 32px;
  color: #1890ff;
  background: #e6f7ff;
  padding: 12px;
  border-radius: 14px;
}

.subtitle {
  color: #8c8c8c;
  margin: 0;
  font-size: 14px;
}

/* 卡片通用 */
.action-card, .list-card {
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.card-title {
  font-weight: 700;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 用户单元格 */
.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.user-account {
  font-size: 12px;
  color: #bfbfbf;
}

/* 角色选择器 */
.role-select :deep(.ant-select-selector) {
  border-radius: 6px !important;
  font-weight: 600;
}

.time-text {
  color: #8c8c8c;
  font-size: 13px;
}

.add-btn {
  border-radius: 8px;
  font-weight: 600;
}

.ghost-btn {
  border-radius: 8px;
  color: #8c8c8c;
}

.delete-btn {
  font-weight: 600;
}

.avatar-shadow {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
