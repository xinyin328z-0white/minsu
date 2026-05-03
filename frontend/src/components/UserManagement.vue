<template>
  <el-card class="mb-16">
    <template #header>用户管理</template>
    <el-button type="primary" @click="openUserDialog">新增用户</el-button>
    <el-table :data="users" style="margin-top: 16px" size="small">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewUserDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="editUser(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="deleteUserConfirm(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- User Dialog -->
  <el-dialog v-model="userDialog" :title="editingUser?.id ? '编辑用户' : '新增用户'">
    <el-form :model="editingUser" label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="editingUser.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="editingUser.password" type="password" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="editingUser.email" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="editingUser.role">
          <el-option label="客户" value="CUSTOMER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userDialog = false">取消</el-button>
      <el-button type="primary" @click="saveUser">保存</el-button>
    </template>
  </el-dialog>

  <!-- User Detail Dialog -->
  <el-dialog v-model="userDetailDialog" title="用户详情">
    <div v-if="selectedUser">
      <el-form label-width="100px" :model="selectedUser">
        <el-form-item label="ID">{{ selectedUser.id }}</el-form-item>
        <el-form-item label="用户名">{{ selectedUser.username }}</el-form-item>
        <el-form-item label="真实姓名">{{ selectedUser.realName }}</el-form-item>
        <el-form-item label="邮箱">{{ selectedUser.email }}</el-form-item>
        <el-form-item label="手机号">{{ selectedUser.phone }}</el-form-item>
        <el-form-item label="角色">{{ selectedUser.role }}</el-form-item>
        <el-form-item label="创建时间">{{ formatDate(selectedUser.createdAt) }}</el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="userDetailDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchUsers, createUser, updateUser, deleteUser } from '../api'

const users = ref([])
const userDialog = ref(false)
const userDetailDialog = ref(false)
const editingUser = ref({
  username: '',
  password: '',
  email: '',
  role: 'CUSTOMER'
})
const selectedUser = ref(null)

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const openUserDialog = () => {
  editingUser.value = {
    username: '',
    password: '',
    email: '',
    role: 'CUSTOMER'
  }
  userDialog.value = true
}

const viewUserDetail = (user) => {
  selectedUser.value = user
  userDetailDialog.value = true
}

const editUser = (user) => {
  editingUser.value = { ...user }
  userDialog.value = true
}

const saveUser = async () => {
  try {
    if (editingUser.value.id) {
      await updateUser(editingUser.value.id, editingUser.value)
      ElMessage.success('用户已更新')
    } else {
      await createUser(editingUser.value)
      ElMessage.success('用户已创建')
    }
    userDialog.value = false
    await loadData()
  } catch (err) {
    ElMessage.error('操作失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteUserConfirm = (userId) => {
  ElMessageBox.confirm('确定删除此用户吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteUser(userId)
      ElMessage.success('用户已删除')
      await loadData()
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const loadData = async () => {
  try {
    const res = await fetchUsers()
    users.value = res.data || []
  } catch (err) {
    ElMessage.error('加载数据失败: ' + (err.response?.data?.message || err.message))
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>
