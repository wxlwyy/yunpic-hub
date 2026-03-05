<template>
  <div class="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo">
<!--            <div class="title">智能协同云图库</div>-->
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick"/>
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
              <a-dropdown>
                <!-- 直接把头像和文字放一起，自动产生间距： 头像和用户名之间，会自动出现一个默认大小的、合适的水平间距（通常是8px）
                 自动垂直对齐： <a-space> 默认会把它的子元素在垂直方向上居中对齐，让头像和文字看起来更和谐，
                 还有一个作用就是将里面的两个内容放在一个根标签中，防止只显示一个内容-->
                <ASpace>
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                  {{ loginUserStore.loginUser.userName ?? '无名'}}
                </ASpace>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <router-link to="/my_space">
                        <UserOutlined />
                        我的空间
                      </router-link>
                    </a-menu-item>
                    <a-menu-item @click="doLogout">
                      <LogoutOutlined />退出登录
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue';
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

//const items = ref<MenuProps['items']>();

//未经过滤的菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '上传图片到公共图库',
    title: '上传图片到公共图库',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  /*{
    key: 'others',
    label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },*/
]

//过滤菜单项
const filterOriginItems = (originItems = [] as MenuProps['items']) => {
  return originItems?.filter((originItem) => {
    if (originItem?.key.startsWith('/admin')){
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

//展示在菜单的路由数组
const items = computed<MenuProps['items']>(() => {
  return filterOriginItems(originItems)
})

const router = useRouter()
//页面跳转
const doMenuClick = ({ key }: {key : string}) => {
  router.push({
    path: key
  });
}
//设置高亮（刷新页面高亮也不会消失）
const current = ref<string[]>([]);

router.afterEach((to, from) => {
  current.value = [to.path];
})

//退出登录
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0){
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    router.push({
      path: '/user/login'
    })
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

</script>

<style scoped>
#globalHeader .title-bar{
  display: flex;
  align-items: center;
}

.logo {
  height: 48px;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}
</style>

