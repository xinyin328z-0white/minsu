<template>
  <el-card class="mb-16">
    <template #header>游玩项目</template>
    <el-button type="primary" @click="openActivityDialog">新增项目</el-button>
    <el-table :data="activities" style="margin-top: 16px" size="small">
      <el-table-column prop="name" label="项目名" />
      <el-table-column prop="location" label="地点" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editActivity(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="deleteActivityConfirm(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- Activity Dialog -->
  <el-dialog v-model="activityDialog" :title="editingActivity?.id ? '编辑项目' : '新增项目'">
    <el-form :model="editingActivity" label-width="100px">
      <el-form-item label="项目名">
        <el-input v-model="editingActivity.name" />
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="editingActivity.location" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="editingActivity.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="activityDialog = false">取消</el-button>
      <el-button type="primary" @click="saveActivity">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchActivities, createActivity, updateActivity, deleteActivity } from '../api'

const activities = ref([])
const activityDialog = ref(false)
const editingActivity = ref({
  name: '',
  location: '',
  description: ''
})

const openActivityDialog = () => {
  editingActivity.value = {
    name: '',
    location: '',
    description: ''
  }
  activityDialog.value = true
}

const editActivity = (activity) => {
  editingActivity.value = { ...activity }
  activityDialog.value = true
}

const saveActivity = async () => {
  try {
    if (editingActivity.value.id) {
      await updateActivity(editingActivity.value.id, editingActivity.value)
      ElMessage.success('项目已更新')
    } else {
      await createActivity(editingActivity.value)
      ElMessage.success('项目已创建')
    }
    activityDialog.value = false
    await loadData()
  } catch (err) {
    ElMessage.error('操作失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteActivityConfirm = (activityId) => {
  ElMessageBox.confirm('确定删除此项目吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteActivity(activityId)
      ElMessage.success('项目已删除')
      await loadData()
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const loadData = async () => {
  try {
    const res = await fetchActivities()
    activities.value = res.data || []
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
