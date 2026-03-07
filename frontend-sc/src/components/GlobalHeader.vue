<template>
  <div class="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="260px">
        <router-link to="/">
          <div class="title-bar">
            <div class="logo-glow"></div>
            <img class="logo" src="../assets/logo.png" alt="logo">
            <div class="title">云图库 · CloudPic</div>
          </div>
        </router-link>
      </a-col>

      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
          class="custom-menu"
        />
      </a-col>

      <a-col flex="160px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown placement="bottomRight">
              <a-space class="user-info-box">
                <div :class="['avatar-wrapper', loginUserStore.loginUser.userRole]">
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="36" />
                </div>
                <div class="user-text-meta">
                  <span :class="['user-name', loginUserStore.loginUser.userRole]">
                    {{ loginUserStore.loginUser.userName ?? '无名' }}
                  </span>
                  <a-tag v-if="loginUserStore.loginUser.userRole === 'admin'" color="error" class="role-tag">管理员</a-tag>
                  <a-tag v-else-if="loginUserStore.loginUser.userRole === 'vip'" color="warning" class="role-tag">VIP</a-tag>
                </div>
              </a-space>

              <template #overlay>
                <a-menu class="dropdown-menu">
                  <a-menu-item key="my_space" @click="router.push('/my_space')">
                    <template #icon><UserOutlined /></template>
                    我的空间
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="doLogout" danger>
                    <template #icon><LogoutOutlined /></template>
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" shape="round" @click="router.push('/user/login')" class="login-btn">
              立即登录
            </a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons-vue';
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 未经过滤的菜单项
const originItems = [
  { key: '/', icon: () => h(HomeOutlined), label: '主页', title: '主页' },
  { key: '/add_picture', label: '上传图片到公共图库', title: '上传图片到公共图库' },
  { key: '/admin/userManage', label: '用户管理', title: '用户管理' },
  { key: '/admin/pictureManage', label: '图片管理', title: '图片管理' },
  { key: '/admin/spaceManage', label: '空间管理', title: '空间管理' },
]

// 过滤菜单项
const filterOriginItems = (originItems = [] as MenuProps['items']) => {
  return originItems?.filter((originItem) => {
    const loginUser = loginUserStore.loginUser

    // 1. 如果是管理员路径，非管理员直接 Pass
    if (originItem?.key.startsWith('/admin')) {
      if (!loginUser || loginUser.userRole !== 'admin') return false
    }

    // 2. 新增逻辑：如果是“上传图片”，且用户未登录（没有 id），直接隐藏
    if (originItem?.key === '/add_picture') {
      if (!loginUser || !loginUser.id) return false
    }

    return true
  })
}

const items = computed<MenuProps['items']>(() => filterOriginItems(originItems))

const doMenuClick = ({ key }: {key : string}) => {
  router.push({ path: key });
}

const current = ref<string[]>([]);
router.afterEach((to) => {
  current.value = [to.path];
})

const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0){
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    router.push({ path: '/user/login' })
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.globalHeader { transition: all 0.3s; }
.title-bar { display: flex; align-items: center; position: relative; padding-left: 12px; }
.logo-glow { position: absolute; width: 40px; height: 40px; background: #3b82f6; filter: blur(20px); opacity: 0.3; z-index: 0; }
.logo { height: 36px; z-index: 1; transition: transform 0.3s; }
.title-bar:hover .logo { transform: rotate(15deg) scale(1.1); }
.title {
  color: #1e293b; font-size: 18px; font-weight: 800; margin-left: 12px; letter-spacing: -0.5px;
  background: linear-gradient(135deg, #1e293b 0%, #3b82f6 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.custom-menu { line-height: 64px; background: transparent !important; border-bottom: none !important; }
:deep(.ant-menu-item) { font-weight: 500 !important; color: #64748b !important; transition: all 0.3s !important; }
:deep(.ant-menu-item-selected), :deep(.ant-menu-item:hover) { color: #3b82f6 !important; }
.user-info-box { cursor: pointer; padding: 4px 8px; border-radius: 12px; transition: background 0.3s; }
.user-info-box:hover { background: rgba(0, 0, 0, 0.04); }
.user-name { font-weight: 600; color: #334155; }
.login-btn { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border: none; box-shadow: 0 4px 10px rgba(37, 99, 235, 0.2); }
.dropdown-menu { border-radius: 12px; padding: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
.role-tag { font-size: 10px; zoom: 0.85; border-radius: 4px; margin-left: 4px; vertical-align: middle; }
.avatar-wrapper { padding: 2px; border-radius: 50%; display: flex; transition: all 0.3s; }
.avatar-wrapper.admin { background: linear-gradient(135deg, #f5222d, #fa541c); box-shadow: 0 0 10px rgba(245, 34, 45, 0.3); }
.avatar-wrapper.vip { background: linear-gradient(135deg, #ffec3d, #ffa940); box-shadow: 0 0 10px rgba(255, 169, 64, 0.4); }
.user-name.vip { color: #d48806; font-weight: bold; }
.user-text-meta { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.2; }
</style>
