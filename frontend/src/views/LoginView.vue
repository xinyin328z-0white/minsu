<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="header-title">{{ isLogin ? '登录' : '注册' }}</div>
      </template>

      <el-form :model="form" label-width="80px" @keyup.enter="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading">{{ isLogin ? '登录' : '注册' }}</el-button>
          <el-button @click="toggleMode">{{ isLogin ? '去注册' : '去登录' }}</el-button>
        </el-form-item>
      </el-form>

      <div v-if="error" class="error-msg">{{ error }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api'

const router = useRouter()
const form = ref({ username: '', password: '' })
const isLogin = ref(true)
const loading = ref(false)
const error = ref('')

const submit = async () => {
  if (!form.value.username || !form.value.password) {
    error.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  error.value = ''

  try {
    let response
    if (isLogin.value) {
      response = await login(form.value.username, form.value.password)
    } else {
      response = await register(form.value.username, form.value.password)
    }

    const { token, userId, role } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('userId', userId)
    localStorage.setItem('role', role)
    localStorage.setItem('username', response.data.username)

    ElMessage.success(isLogin.value ? '登录成功' : '注册成功')
    router.push(role === 'ADMIN' ? '/admin' : '/customer')
  } catch (err) {
    console.error('Auth error:', err)
    let errorMsg = isLogin.value ? '登录失败' : '注册失败'
    
    // Check for specific error messages
    const responseData = err.response?.data
    if (typeof responseData === 'string' && responseData.includes('User not found')) {
      errorMsg = '用户不存在，请先注册'
    } else if (typeof responseData === 'string' && responseData.includes('Invalid password')) {
      errorMsg = '密码错误'
    } else if (responseData?.message) {
      errorMsg = responseData.message
    } else if (err.response?.statusText) {
      errorMsg = err.response.statusText
    } else if (err.message) {
      errorMsg = err.message
    }
    
    error.value = errorMsg
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.value = { username: '', password: '' }
  error.value = ''
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.header-title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
}

.error-msg {
  color: #f56c6c;
  margin-top: 8px;
  text-align: center;
}
</style>
