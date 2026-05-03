<template>
  <el-card class="mb-16">
    <template #header>预订设置</template>
    <el-form label-width="160px">
      <el-form-item label="仅可预订N天内">
        <el-input-number v-model="bookingWindowDays" :min="1" :max="365" />
      </el-form-item>
    </el-form>
    <el-button type="primary" :loading="settingsLoading" @click="saveSettings">保存设置</el-button>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBookingWindowDays, updateBookingWindowDays } from '../api'

const bookingWindowDays = ref(30)
const settingsLoading = ref(false)

const loadSettings = async () => {
  try {
    const res = await getBookingWindowDays()
    if (res.data) {
      bookingWindowDays.value = res.data
    }
  } catch (err) {
    console.error('加载设置失败:', err)
  }
}

const saveSettings = async () => {
  settingsLoading.value = true
  try {
    await updateBookingWindowDays(bookingWindowDays.value)
    ElMessage.success('设置已保存')
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    settingsLoading.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>
