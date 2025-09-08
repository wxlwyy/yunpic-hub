import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<any>({
    userName: "未登录",
  })

  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser;
  }

  async function fetchLoginUser() {
    //测试
    setTimeout(() => {
      loginUser.value = { userName: "测试用户", id: 1}
    }, 3000)
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})
