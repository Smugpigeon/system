<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2>{{ isLogin ? '用户登录' : '用户注册' }}</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20位，字母数字下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6位，含字母和数字" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading" style="width: 100%">
            {{ isLogin ? '登录' : '注册' }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button link @click="toggleMode" style="width: 100%">
            {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/http.js'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const isLogin = ref(true)

const form = reactive({
  username: '',
  password: ''
})

const validateUsername = (rule, value, callback) => {
  const reg = /^[a-zA-Z0-9_]{4,20}$/
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (!reg.test(value)) {
    callback(new Error('用户名格式错误'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  const reg = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (!reg.test(value)) {
    callback(new Error('密码至少6位，含字母和数字'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }]
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const api = isLogin.value ? authApi.login : authApi.register
    const res = await api(form)
    
    if (isLogin.value) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.username)
      ElMessage.success('登录成功')
      router.push('/tasks')
    } else {
      ElMessage.success('注册成功，请登录')
      isLogin.value = true
      form.password = ''
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>