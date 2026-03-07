<template>
  <div id="userLoginPage">
    <div class="aurora-container">
      <div class="aurora-blob aurora-1"></div>
      <div class="aurora-blob aurora-2"></div>
      <div class="aurora-blob aurora-3"></div>
      <div class="aurora-blob aurora-4"></div>
    </div>

    <div class="login-card">
      <div class="header">
        <div class="logo-area">
          <img src="@/assets/logo.png" alt="logo" class="logo" />
          <h2 class="title">云图库</h2>
        </div>
        <div class="desc">智能协同 · 视觉资产管理专家</div>
      </div>

      <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="账号" size="large" class="custom-input">
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
          <a-input-password v-model:value="formState.userPassword" placeholder="密码" size="large" class="custom-input">
            <template #prefix><lock-outlined class="icon-color"/></template>
          </a-input-password>
        </a-form-item>

        <div class="tips">
          没有账号？<RouterLink to="/user/register" class="reg-link">立即注册</RouterLink>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" class="login-button" size="large">
            进入空间
          </a-button>
          <a-button
            block
            size="large"
            class="guest-button"
            @click="router.push('/')"
          >
            <compass-outlined /> 暂不登录，先去逛逛
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined, CompassOutlined } from '@ant-design/icons-vue' // 🚀 加了指南针图标

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: any) => {
  const res = await userLoginUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功，欢迎进入空间')
    router.push({ path: '/', replace: true })
  } else {
    message.error('验证失败：' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: #f0f4f8;
  overflow: hidden;
}

/* 惊艳核心：极光流体背景 */
.aurora-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  filter: blur(80px); /* 增加模糊度，让颜色融合更自然 */
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

/* 登录卡片美化 */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(25px); /* 强化毛玻璃 */
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 28px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center; /* 确保文字区域整体居中 */
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

.reg-link {
  color: #3498db;
  font-weight: 600;
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #2c3e50; /* 深蓝灰色，高级沉稳 */
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s;
}

.login-button:hover {
  background: #34495e;
  transform: translateY(-2px);
}

.login-button {
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

.login-button:hover {
  background: #34495e;
  transform: translateY(-2px);
}

/* 🚀 新增：访客按钮样式 */
.guest-button {
  margin-top: 16px;
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.guest-button:hover {
  color: #3b82f6;
  background: rgba(255, 255, 255, 0.9);
  border-color: #3b82f6;
}
</style>
