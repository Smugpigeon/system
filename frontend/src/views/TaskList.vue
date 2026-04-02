<template>
  <div class="task-container">
    <!-- 顶部栏 -->
    <el-header class="header">
      <div class="header-left">
        <span class="title">📋 任务管理系统</span>
      </div>
      <div class="header-right">
        <span class="username">欢迎，{{ username }}</span>
        <el-button type="danger" size="small" @click="logout">退出</el-button>
      </div>
    </el-header>

    <!-- 主内容 -->
    <el-main>
      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="openDialog()">+ 新增任务</el-button>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 120px">
          <el-option label="待办" value="TODO" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="DONE" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索任务"
          clearable
          style="width: 200px"
        >
          <template #suffix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 任务列表 -->
      <el-table :data="filteredTasks" v-loading="loading" style="margin-top: 20px">
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.deadline) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="confirmDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-main>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新增任务'" width="500px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待办" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="LOW" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi } from '../api/http.js'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const filterStatus = ref('')
const searchKeyword = ref('')
const username = ref(localStorage.getItem('username') || '')

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const form = reactive({
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  deadline: null
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 过滤后的任务
const filteredTasks = computed(() => {
  let result = tasks.value
  if (filterStatus.value) {
    result = result.filter(t => t.status === filterStatus.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(t => 
      t.title.toLowerCase().includes(kw) || 
      t.description?.toLowerCase().includes(kw)
    )
  }
  return result.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))
})

// 加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const res = await taskApi.getList()
    tasks.value = res.data
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const statusText = (s) => ({ TODO: '待办', IN_PROGRESS: '进行中', DONE: '已完成' }[s])
const statusType = (s) => ({ TODO: 'info', IN_PROGRESS: 'warning', DONE: 'success' }[s])
const priorityType = (p) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[p])

const formatDate = (date) => date ? new Date(date).toLocaleString() : '-'

// 打开对话框
const openDialog = (row) => {
  isEdit.value = !!row
  currentId.value = row?.id || null
  
  if (row) {
    Object.assign(form, {
      title: row.title,
      description: row.description || '',
      status: row.status,
      priority: row.priority,
      deadline: row.deadline ? new Date(row.deadline) : null
    })
  } else {
    formRef.value?.resetFields()
    Object.assign(form, {
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      deadline: null
    })
  }
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      ...form,
      deadline: form.deadline ? form.deadline.toISOString() : null
    }
    
    if (isEdit.value) {
      await taskApi.update(currentId.value, data)
      ElMessage.success('更新成功')
    } else {
      await taskApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTasks()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除确认
const confirmDelete = (id) => {
  ElMessageBox.confirm('确定删除该任务吗？', '提示', { type: 'warning' })
    .then(async () => {
      await taskApi.delete(id)
      ElMessage.success('删除成功')
      loadTasks()
    })
    .catch(() => {})
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}

onMounted(loadTasks)
</script>

<style scoped>
.task-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  padding: 0 20px;
}

.title {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.username {
  margin-right: 20px;
  color: #666;
}

.toolbar {
  display: flex;
  gap: 15px;
  align-items: center;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
}
</style>