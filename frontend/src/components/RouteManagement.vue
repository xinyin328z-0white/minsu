<template>
  <el-card class="mb-16">
    <template #header>旅游路线</template>
    <el-button type="primary" @click="openRouteDialog">新增路线</el-button>
    <el-table :data="routes" style="margin-top: 16px" size="small">
      <el-table-column prop="name" label="路线名" />
      <el-table-column prop="tags" label="标签" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editRoute(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="deleteRouteConfirm(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- Route Dialog -->
  <el-dialog v-model="routeDialog" :title="editingRoute?.id ? '编辑路线' : '新增路线'">
    <el-form :model="editingRoute" label-width="100px">
      <el-form-item label="路线名">
        <el-input v-model="editingRoute.name" />
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="editingRoute.tags" placeholder="多个标签用逗号分隔" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="editingRoute.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="routeDialog = false">取消</el-button>
      <el-button type="primary" @click="saveRoute">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchRoutes, createRoute, updateRoute, deleteRoute } from '../api'

const routes = ref([])
const routeDialog = ref(false)
const editingRoute = ref({
  name: '',
  tags: '',
  description: ''
})

const openRouteDialog = () => {
  editingRoute.value = {
    name: '',
    tags: '',
    description: ''
  }
  routeDialog.value = true
}

const editRoute = (route) => {
  editingRoute.value = { ...route }
  routeDialog.value = true
}

const saveRoute = async () => {
  try {
    if (editingRoute.value.id) {
      await updateRoute(editingRoute.value.id, editingRoute.value)
      ElMessage.success('路线已更新')
    } else {
      await createRoute(editingRoute.value)
      ElMessage.success('路线已创建')
    }
    routeDialog.value = false
    await loadData()
  } catch (err) {
    ElMessage.error('操作失败: ' + (err.response?.data?.message || err.message))
  }
}

const deleteRouteConfirm = (routeId) => {
  ElMessageBox.confirm('确定删除此路线吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteRoute(routeId)
      ElMessage.success('路线已删除')
      await loadData()
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

const loadData = async () => {
  try {
    const res = await fetchRoutes()
    routes.value = res.data || []
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
