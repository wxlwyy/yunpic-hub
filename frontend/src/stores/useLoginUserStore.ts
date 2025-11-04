import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUserUsingGet } from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }

  async function fetchLoginUser() {//保存登录的用户信息
    const res = await getLoginUserUsingGet()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
    //测试
    /*setTimeout(() => {
      loginUser.value = { userName: "测试用户", id: 1}
    }, 3000)*/
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})
