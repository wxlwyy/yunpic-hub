<template>
  <div class="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      class="sider-container"
      width="220"
      breakpoint="lg"
      collapsed-width="0"
    >
      <div class="sider-wrapper">
        <a-menu
          mode="inline"
          v-model:selectedKeys="current"
          :items="menuItems"
          @click="doMenuClick"
          class="custom-menu"
        />
      </div>
    </a-layout-sider>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue'
import { PictureOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { MenuProps } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { SPACE_TYPE_ENUM, SPACE_ROLE_ENUM } from '@/constants/space.ts'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()

// 1. 固定菜单
const fixedMenuItems = [
  { key: '/', label: '公共图库', icon: () => h(PictureOutlined) },
  { key: '/my_space', label: '我的空间', icon: () => h(UserOutlined) },
  { key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM, label: '创建团队空间', icon: () => h(TeamOutlined) },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])

const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  }
}

// 2. 动态菜单逻辑
const menuItems = computed(() => {
  const validSpaces = teamSpaceList.value.filter(s => s.spaceId && s.spaceVO?.spaceName)
  const managedSpaces = validSpaces
    .filter(s => s.spaceRole === SPACE_ROLE_ENUM.ADMIN)
    .map(s => ({ key: '/space/' + s.spaceId, label: s.spaceVO.spaceName }))
  const joinedSpaces = validSpaces
    .filter(s => s.spaceRole !== SPACE_ROLE_ENUM.ADMIN)
    .map(s => ({ key: '/space/' + s.spaceId, label: s.spaceVO.spaceName }))

  const items = [...fixedMenuItems]
  if (managedSpaces.length > 0) {
    items.push({ type: 'divider' }, { type: 'group', label: '我管理的团队', key: 'managed', children: managedSpaces })
  }
  if (joinedSpaces.length > 0) {
    if (managedSpaces.length === 0) items.push({ type: 'divider' })
    items.push({ type: 'group', label: '我加入的团队', key: 'joined', children: joinedSpaces })
  }
  return items
})

const current = ref<string[]>([])

/**
 * 🚀 核心修复：全自动身份匹配逻辑
 */
watchEffect(() => {
  const { path, fullPath, params } = route

  // A. 如果是精确匹配（如主页、创建页）
  if (fullPath === '/' || fullPath.includes('/add_space')) {
    current.value = [fullPath]
    return
  }

  // B. 如果当前身处某个空间详情页 (/space/:id)
  if (path.startsWith('/space/')) {
    const spaceId = params.id
    // 检查这个 ID 是否在“团队列表”里
    const isTeamSpace = teamSpaceList.value.some(s => String(s.spaceId) === String(spaceId))

    if (isTeamSpace) {
      // 如果在团队里，高亮具体的团队项
      current.value = [path]
    } else {
      // 【关键点】如果不属于任何团队，它就是“我的空间”！
      current.value = ['/my_space']
    }
  } else {
    // C. 兜底匹配（如直接访问 /my_space 或其他页面）
    current.value = [path]
  }
})

const doMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}

watchEffect(() => {
  if (loginUserStore.loginUser.id) fetchTeamSpaceList()
})
</script>

<style scoped>
/* 保持那套牛逼的 CSS 不变 */
.sider-container { background: transparent !important; height: calc(100vh - 64px); }
.sider-wrapper { padding: 16px 12px; height: 100%; }
.custom-menu { background: transparent !important; border: none !important; }
:deep(.ant-menu-item) { height: 44px !important; line-height: 44px !important; margin-bottom: 4px !important; border-radius: 12px !important; color: #64748b !important; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important; }
:deep(.ant-menu-item:hover) { color: #3b82f6 !important; background-color: rgba(59, 130, 246, 0.08) !important; }
:deep(.ant-menu-item-selected) { color: #fff !important; background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25); font-weight: 600; }
:deep(.ant-menu-item-selected .anticon) { color: #fff !important; }
:deep(.ant-menu-item-group-title) { color: #94a3b8 !important; font-size: 11px !important; font-weight: 700 !important; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 12px; }
:deep(.ant-menu-divider) { margin: 12px 16px; border-color: rgba(0, 0, 0, 0.04); }
</style>
