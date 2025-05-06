<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>家庭理财系统</h2>
        </div>
      </template>
      <el-form :model="loginForm" ref="loginFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import api from '../api'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

export default {
  name: 'LoginView',
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    const loginForm = reactive({
      username: '',
      password: ''
    })

    const handleLogin = async () => {
      if (!loginForm.username || !loginForm.password) {
        ElMessage.warning('请输入用户名和密码')
        return
      }

      loading.value = true
      try {
        const response = await api.login(loginForm)
        if (response.status === 200) {
          localStorage.setItem('isAuthenticated', 'true')
          router.push('/home')
          ElMessage.success('登录成功')
        }
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
        console.error('Login error:', error)
      } finally {
        loading.value = false
      }
    }

    return {
      loginForm,
      loginFormRef,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.login-card {
  width: 400px;
}

.card-header {
  display: flex;
  justify-content: center;
}
</style>