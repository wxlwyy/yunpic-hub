<template>
  <div id="userRegisterPage">
    <div class="aurora-container">
      <div class="aurora-blob aurora-1"></div>
      <div class="aurora-blob aurora-2"></div>
      <div class="aurora-blob aurora-3"></div>
      <div class="aurora-blob aurora-4"></div>
    </div>

    <div class="register-card">
      <div class="header">
        <div class="logo-area">
          <img src="@/assets/logo.png" alt="logo" class="logo" />
          <h2 class="title">云图库</h2>
        </div>
        <div class="desc">开启你的视觉管理之旅</div>
      </div>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" class="custom-input">
            <template #prefix><user-outlined class="icon-color"/></template>
          </a-input>
        </a-form-item>

        <a-form-item
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于8位' }
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large" class="custom-input">
            <template #prefix><lock-outlined class="icon-color"/></template>
          </a-input-password>
        </a-form-item>

        <a-form-item
          name="checkPassword"
          :rules="[
            { required: true, message: '请输入确认密码' },
            { min: 8, message: '确认密码不能小于8位' }
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="确认密码" size="large" class="custom-input">
            <template #prefix><check-circle-outlined class="icon-color"/></template>
          </a-input-password>
        </a-form-item>

        <div class="tips">
          已有账号？<RouterLink to="/user/login" class="login-link">去登录</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" class="register-button" size="large">
            立即注册
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userRegisterUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const router = useRouter()

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  // 保持原有逻辑：判断两次密码是否一致
  if (formState.userPassword !== formState.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  const res = await userRegisterUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功，欢迎加入')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败：' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: #f0f4f8;
  overflow: hidden;
}

/* 极光背景 (完全同步登录页) */
.aurora-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  filter: blur(80px);
}

.aurora-blob {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  opacity: 0.6;
  animation: fluid 25s infinite alternate ease-in-out;
}

.aurora-1 { background: #00d2ff; top: -10%; left: -10%; }
.aurora-2 { background: #92fe9d; bottom: -10%; right: -10%; animation-delay: -5s; }
.aurora-3 { background: #ff758c; top: 40%; left: 30%; animation-delay: -10s; }
.aurora-4 { background: #ff7eb3; bottom: 10%; left: 10%; animation-delay: -15s; }

@keyframes fluid {
  0% { transform: translate(0, 0) scale(1) rotate(0deg); }
  33% { transform: translate(50px, 100px) scale(1.1) rotate(120deg); }
  66% { transform: translate(-30px, 50px) scale(0.9) rotate(240deg); }
  100% { transform: translate(0, 0) scale(1) rotate(360deg); }
}

/* 注册卡片 */
.register-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 28px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.logo {
  height: 42px;
}

.title {
  font-size: 30px;
  font-weight: 800;
  margin: 0;
  background: linear-gradient(135deg, #2c3e50, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.desc {
  color: #7f8c8d;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.custom-input {
  border-radius: 12px !important;
}

.tips {
  text-align: right;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 24px;
}

.login-link {
  color: #3498db;
  font-weight: 600;
}

.register-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #2c3e50;
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s;
}

.register-button:hover {
  background: #34495e;
  transform: translateY(-2px);
}
</style>
