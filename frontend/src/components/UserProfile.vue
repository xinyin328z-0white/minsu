<template>
  <el-card>
    <template #header>
      <div class="profile-header">
        <span>个人信息</span>
        <el-button v-if="!editing" type="primary" @click="editing = true">编辑</el-button>
        <div v-else>
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </div>
      </div>
    </template>
    
    <el-form :model="form" label-width="100px">
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      
      <el-form-item label="真实姓名">
        <el-input v-model="form.realName" :disabled="!editing" placeholder="请输入真实姓名" />
      </el-form-item>
      
      <el-form-item label="手机号">
        <el-input v-model="form.phone" :disabled="!editing" placeholder="请输入手机号" />
      </el-form-item>
      
      <el-form-item label="邮箱">
        <el-input v-model="form.email" :disabled="!editing" placeholder="请输入邮箱" />
      </el-form-item>
      
      <el-form-item label="身份证号">
        <el-input v-model="form.idCard" :disabled="!editing" placeholder="请输入身份证号" />
      </el-form-item>
      
      <el-form-item label="地址">
        <el-input v-model="form.address" :disabled="!editing" type="textarea" :rows="2" placeholder="请输入地址" />
      </el-form-item>
      
      <el-form-item label="头像URL">
        <el-input v-model="form.avatar" :disabled="!editing" placeholder="请输入头像图片URL" />
        <el-image v-if="form.avatar" :src="form.avatar" fit="cover" style="width: 100px; height: 100px; margin-top: 10px;" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchUser, updateUserProfile } from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const form = ref({
  username: '',
  realName: '',
  phone: '',
  email: '',
  idCard: '',
  address: '',
  avatar: ''
})

const originalData = ref({})
const editing = ref(false)
const saving = ref(false)

const loadProfile = async () => {
  try {
    const res = await fetchUser(props.userId)
    form.value = { ...res.data }
    originalData.value = { ...res.data }
  } catch (err) {
    ElMessage.error('加载用户信息失败')
    console.error(err)
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateUserProfile(props.userId, form.value)
    originalData.value = { ...form.value }
    editing.value = false
    ElMessage.success('保存成功')
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data || err.message))
  } finally {
    saving.value = false
  }
}

const cancelEdit = () => {
  form.value = { ...originalData.value }
  editing.value = false
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
