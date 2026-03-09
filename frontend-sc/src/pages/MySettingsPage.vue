<template>
  <div id="mySettingsPage">
    <a-card :bordered="false" class="settings-card">
      <a-tabs v-model:activeKey="activeKey" tab-position="left" :style="{ minHeight: '600px' }">

        <a-tab-pane key="basic" tab="基本设置">
          <div class="tab-content">
            <h3 class="tab-title">个人资料</h3>
            <a-form layout="vertical" :model="userForm" @finish="handleUpdate">
              <a-row :gutter="48">
                <a-col :xs="24" :md="16">
                  <a-form-item label="用户昵称" name="userName">
                    <a-input v-model:value="userForm.userName" placeholder="起个响亮的昵称吧" />
                  </a-form-item>
                  <a-form-item label="个人简介" name="userProfile">
                    <a-textarea v-model:value="userForm.userProfile" :rows="4" placeholder="一句话介绍你自己" />
                  </a-form-item>
                  <a-button type="primary" html-type="submit" :loading="submitting">更新资料</a-button>
                </a-col>

                <a-col :xs="24" :md="8" class="avatar-col">
                  <a-form-item label="个人头像">
                    <AvatarUploader
                      :picture="{ url: userForm.userAvatar }"
                      @onSuccess="handleAvatarSuccess"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </div>
        </a-tab-pane>

        <a-tab-pane key="vip" tab="会员中心">
          <div class="tab-content">
            <h3 class="tab-title">会员状态</h3>
            <div class="vip-status-box">
              <div class="vip-info">
                <span class="label">当前身份：</span>
                <a-tag :color="isAdmin ? 'orange' : (isVip ? 'purple' : 'default')">
                  {{ isAdmin ? '超级管理员' : (isVip ? '尊贵 VIP' : '普通用户') }}
                </a-tag>
              </div>
              <a-divider />
              <div class="exchange-section">
                <p>使用兑换码升级为 VIP，解锁更多空间和高级功能：</p>
                <a-input-search
                  v-model:value="vipCode"
                  placeholder="请输入兑换码"
                  enter-button="立即兑换"
                  size="large"
                  @search="handleExchangeVip"
                  :loading="exchanging"
                />
              </div>
            </div>
          </div>
        </a-tab-pane>

      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { updateMyUserUsingPost, exchangeVipUsingPost } from '@/api/userController'
import AvatarUploader from '@/components/AvatarUploader.vue' // 稍后微调这个组件

const loginUserStore = useLoginUserStore()
const activeKey = ref('basic')
const submitting = ref(false)
const exchanging = ref(false)
const vipCode = ref('')

// 表单数据
const userForm = ref({
  userName: '',
  userAvatar: '',
  userProfile: ''
})

// 初始化数据
onMounted(() => {
  const { loginUser } = loginUserStore
  userForm.value = {
    userName: loginUser.userName,
    userAvatar: loginUser.userAvatar,
    userProfile: loginUser.userProfile
  }
})

// 身份判断
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const isVip = computed(() => loginUserStore.loginUser.userRole === 'vip')

// 头像上传成功后的回调
const handleAvatarSuccess = (url: string) => {
  userForm.value.userAvatar = url
  // 🚀 小技巧：上传完头像直接存一下，体验更丝滑
  handleUpdate()
}

// 提交个人资料更新
const handleUpdate = async () => {
  submitting.value = true
  try {
    const res = await updateMyUserUsingPost(userForm.value)
    if (res.data.code === 0) {
      message.success('更新成功')
      // 🚀 重要：更新全局 Store 里的用户信息，这样导航栏头像才会变
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

// VIP 兑换逻辑
const handleExchangeVip = async () => {
  if (!vipCode.value) return message.warning('请输入兑换码')
  exchanging.value = true
  try {
    const res = await exchangeVipUsingPost({ vipCode: vipCode.value })
    if (res.data.code === 0) {
      message.success('恭喜你，兑换成功！')
      vipCode.value = ''
      await loginUserStore.fetchLoginUser()
    } else {
      message.error(res.data.message)
    }
  } finally {
    exchanging.value = false
  }
}
</script>

<style scoped>
.settings-card {
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.tab-content {
  padding: 0 40px;
}

.tab-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 32px;
  color: #1a1a1a;
}

.avatar-col {
  display: flex;
  justify-content: center;
  border-left: 1px solid #f0f0f0;
}

.vip-status-box {
  background: #fcfaff;
  padding: 32px;
  border-radius: 12px;
  border: 1px solid #efdbff;
}

.label {
  color: #666;
  font-size: 14px;
}
</style>
