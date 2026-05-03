<template>
  <!-- 订单审核列表对话框 -->
  <el-dialog 
    v-model="visible"
    title="订单审核"
    width="70%"
    @close="handleClose"
  >
    <div v-if="bookings.length > 0" class="booking-review-list">
      <el-table :data="bookings" size="small">
        <el-table-column prop="id" label="订单ID" width="100" />
        <el-table-column prop="homestayName" label="民宿" />
        <el-table-column prop="checkInDate" label="入住" width="120" />
        <el-table-column prop="checkOutDate" label="退房" width="120" />
        <el-table-column prop="guestCount" label="人数" width="80" />
        <el-table-column prop="totalPrice" label="金额" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getBookingStatusType(row.status)">
              {{ getBookingStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'PENDING'"
              link 
              type="primary" 
              size="small" 
              @click="viewBookingDetail(row)"
            >
              审核
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else class="empty-bookings">
      暂无待审核订单
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 订单详情对话框 -->
  <el-dialog 
    v-model="showDetailDialog"
    title="订单审核详情"
    width="80%"
    @close="handleDetailClose"
  >
    <div v-if="selectedBooking" class="booking-detail">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="订单ID">{{ selectedBooking.id }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ customerName }}</el-descriptions-item>
            <el-descriptions-item label="民宿">{{ selectedBooking.homestayName }}</el-descriptions-item>
            <el-descriptions-item label="入住日期">{{ selectedBooking.checkInDate }}</el-descriptions-item>
            <el-descriptions-item label="退房日期">{{ selectedBooking.checkOutDate }}</el-descriptions-item>
            <el-descriptions-item label="人数">{{ selectedBooking.guestCount }}</el-descriptions-item>
            <el-descriptions-item label="总价">¥{{ selectedBooking.totalPrice }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getBookingStatusType(selectedBooking.status)">
                {{ getBookingStatusText(selectedBooking.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ selectedBooking.contactPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="特殊需求">
              {{ selectedBooking.specialRequests || '无' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :xs="24" :sm="12">
          <!-- 预计影响日期 -->
          <div class="impact-preview" v-if="selectedBooking.status === 'PENDING'">
            <h4>预计影响日期</h4>
            <div v-if="impactedDates.length > 0" class="affected-dates-grid">
              <div v-for="dateInfo in impactedDates" :key="dateInfo.dateStr" class="date-card">
                <div class="date-header">{{ dateInfo.dateStr }}</div>
                <div class="date-details">
                  <div class="detail-row">
                    <span class="detail-label">当前:</span>
                    <el-tag :type="getAvailabilityType(dateInfo.current)" size="small">
                      {{ dateInfo.current }}间
                    </el-tag>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">之后:</span>
                    <el-tag type="danger" size="small">{{ dateInfo.after }}间</el-tag>
                  </div>
                  <div v-if="dateInfo.after === 0" class="warning-row">
                    ⚠️ 将满房
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="no-impact-message">
              <el-empty description="此订单对房间余量无影响" />
            </div>
          </div>
        </el-col>
      </el-row>
      
      <div v-if="selectedBooking.status === 'PENDING'" style="margin-top: 20px;">
        <el-form :model="reviewForm" label-width="100px">
          <el-form-item label="审核意见">
            <el-input 
              v-model="reviewForm.notes" 
              type="textarea" 
              :rows="3" 
              placeholder="输入审核意见或拒绝原因"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>
    <template #footer>
      <el-button @click="showDetailDialog = false">关闭</el-button>
      <el-button 
        v-if="selectedBooking && selectedBooking.status === 'PENDING'"
        type="success" 
        :loading="reviewLoading"
        @click="handleApprove"
      >
        批准订单
      </el-button>
      <el-button 
        v-if="selectedBooking && selectedBooking.status === 'PENDING'"
        type="danger" 
        :loading="reviewLoading"
        @click="handleReject"
      >
        拒绝订单
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewBooking } from '../api'
import { format, eachDayOfInterval, parseISO } from 'date-fns'
import zhCN from 'date-fns/locale/zh-CN'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  bookings: {
    type: Array,
    default: () => []
  },
  customerName: {
    type: String,
    default: ''
  },
  currentAvailability: {
    type: Object,
    default: () => ({})
  },
  availableRooms: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['update:modelValue', 'booking-approved', 'booking-rejected', 'refresh'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const showDetailDialog = ref(false)
const selectedBooking = ref(null)
const reviewForm = ref({
  notes: ''
})
const reviewLoading = ref(false)

const getDatesBetween = () => {
  if (!selectedBooking.value) return []
  const start = parseISO(selectedBooking.value.checkInDate)
  const end = parseISO(selectedBooking.value.checkOutDate)
  return eachDayOfInterval({ start, end }).slice(0, -1) // 排除退房日期
}

const impactedDates = computed(() => {
  const dates = getDatesBetween()
  const impact = []
  const availability = props.currentAvailability || {}
  const totalRooms = props.availableRooms || 1
  
  dates.forEach(date => {
    const dateStr = format(date, 'yyyy-MM-dd')
    const currentAvailable = availability[dateStr] || totalRooms
    const afterAvailable = Math.max(0, currentAvailable - 1)
    
    if (currentAvailable !== afterAvailable) {
      impact.push({
        date: dateStr,
        dateStr: format(date, 'M月d日 EEEE', { locale: zhCN }),
        current: currentAvailable,
        after: afterAvailable
      })
    }
  })
  
  return impact
})

const viewBookingDetail = (booking) => {
  selectedBooking.value = booking
  reviewForm.value = {
    notes: ''
  }
  showDetailDialog.value = true
}

const handleApprove = () => {
  ElMessageBox.confirm(
    '确认通过此订单吗？客户将收到确认通知。',
    '确认审核',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'success'
    }
  ).then(async () => {
    await approveBooking()
  }).catch(() => {
    ElMessage.info('已取消操作')
  })
}

const handleReject = () => {
  if (!reviewForm.value.notes.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  ElMessageBox.confirm(
    '确认拒绝此订单吗？客户将收到拒绝通知。',
    '确认审核',
    {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await rejectBooking()
  }).catch(() => {
    ElMessage.info('已取消操作')
  })
}

const approveBooking = async () => {
  if (!selectedBooking.value) return

  reviewLoading.value = true
  try {
    await reviewBooking(selectedBooking.value.id, 'CONFIRMED', '')
    ElMessage.success('订单已通过')
    emit('booking-approved', selectedBooking.value)
    showDetailDialog.value = false
    emit('refresh')
  } catch (err) {
    console.error('更新订单失败:', err)
    ElMessage.error('更新订单失败: ' + (err.response?.data?.message || err.message || '未知错误'))
  } finally {
    reviewLoading.value = false
  }
}

const rejectBooking = async () => {
  if (!selectedBooking.value) return

  reviewLoading.value = true
  try {
    await reviewBooking(selectedBooking.value.id, 'CANCELED', reviewForm.value.notes)
    ElMessage.success('订单已拒绝')
    emit('booking-rejected', selectedBooking.value)
    showDetailDialog.value = false
    emit('refresh')
  } catch (err) {
    console.error('更新订单失败:', err)
    ElMessage.error('更新订单失败: ' + (err.response?.data?.message || err.message || '未知错误'))
  } finally {
    reviewLoading.value = false
  }
}

const handleClose = () => {
  // 对话框关闭时的处理
}

const handleDetailClose = () => {
  selectedBooking.value = null
  reviewForm.value = { notes: '' }
}

const getBookingStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'CONFIRMED': 'success',
    'CANCELED': 'danger',
    'COMPLETED': 'info'
  }
  return typeMap[status] || 'info'
}

const getBookingStatusText = (status) => {
  const textMap = {
    'PENDING': '待审核',
    'CONFIRMED': '已确认',
    'CANCELED': '已拒绝',
    'COMPLETED': '已完成'
  }
  return textMap[status] || status
}

const getAvailabilityType = (available) => {
  if (available === 0) return 'danger'
  if (available === 1) return 'warning'
  return 'success'
}
</script>

<style scoped>
.booking-review-list {
  min-height: 300px;
}

.empty-bookings {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 14px;
}

.booking-detail {
  padding: 20px 0;
}

.impact-preview {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  background-color: #f5f7fa;
}

.impact-preview h4 {
  margin: 0 0 16px 0;
  color: #303133;
}

.affected-dates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.date-card {
  border: 1px solid #e1e4e8;
  border-radius: 4px;
  padding: 12px;
  background: white;
  transition: box-shadow 0.3s;
}

.date-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.date-header {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.date-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  color: #909399;
  min-width: 40px;
}

.warning-row {
  color: #f56c6c;
  font-weight: 600;
  margin-top: 4px;
}

.no-impact-message {
  text-align: center;
  padding: 20px;
  color: #909399;
}
</style>
