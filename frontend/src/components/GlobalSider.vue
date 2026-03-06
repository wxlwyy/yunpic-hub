<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      class="sider-container"
      width="220"
      breakpoint="lg"
      collapsed-width="0"
      :trigger="null"
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
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 1. 核心菜单逻辑 (保留原有业务逻辑)
const fixedMenuItems = [
  { key: '/', label: '公共图库', icon: () => h(PictureOutlined) },
  { key: '/my_space', label: '我的空间', icon: () => h(UserOutlined) },
  { key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM, label: '创建团队', icon: () => h(TeamOutlined) },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])
const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  }
}

const menuItems = computed(() => {
  const teamSpaceSubMenus = teamSpaceList.value
    .filter(spaceUser => spaceUser.spaceId && spaceUser.spaceVO?.spaceName)
    .map((spaceUser) => ({
      key: '/space/' + spaceUser.spaceId,
      label: spaceUser.spaceVO.spaceName,
    }))

  if (teamSpaceSubMenus.length === 0) return fixedMenuItems;

  return [
    ...fixedMenuItems,
    { type: 'divider' }, // 增加一条分割线增加呼吸感
    {
      type: 'group',
      label: '我的团队',
      key: 'teamSpace',
      children: teamSpaceSubMenus,
    }
  ]
})

const current = ref<string[]>([])
router.afterEach((to) => { current.value = [to.path] })

const doMenuClick = ({ key }: { key: string }) => { router.push(key) }

watchEffect(() => {
  if (loginUserStore.loginUser.id) fetchTeamSpaceList()
})
</script>

<style scoped>
/* 侧边栏整体容器 */
.sider-container {
  background: transparent !important;
  height: calc(100vh - 64px); /* 减去顶部导航高度 */
}

.sider-wrapper {
  padding: 16px 12px;
  height: 100%;
}

/* 彻底重构 Ant Design 菜单样式 */
.custom-menu {
  background: transparent !important;
  border: none !important;
}

/* 每一个菜单项的基础样式 */
:deep(.ant-menu-item) {
  height: 44px !important;
  line-height: 44px !important;
  margin-bottom: 4px !important;
  border-radius: 12px !important; /* 胶囊形状 */
  color: #64748b !important; /* 默认未选中的灰蓝色 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* 菜单项悬浮状态 */
:deep(.ant-menu-item:hover) {
  color: #3b82f6 !important;
  background-color: rgba(59, 130, 246, 0.08) !important;
}

/* 核心：菜单项选中状态 (高亮) */
:deep(.ant-menu-item-selected) {
  color: #fff !important;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  font-weight: 600;
}

/* 选中时的图标颜色 */
:deep(.ant-menu-item-selected .anticon) {
  color: #fff !important;
}

/* 菜单组标题样式 (我的团队) */
:deep(.ant-menu-item-group-title) {
  color: #94a3b8 !important;
  font-size: 12px !important;
  font-weight: 700 !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 16px;
  padding-left: 16px !important;
}

/* 分割线样式 */
:deep(.ant-menu-divider) {
  margin: 12px 16px;
  border-color: rgba(0, 0, 0, 0.04);
}

/* 针对图标大小的微调 */
:deep(.ant-menu-item .anticon) {
  font-size: 16px;
}
</style>
