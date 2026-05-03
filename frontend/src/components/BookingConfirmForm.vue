<template>
  <div class="booking-confirm-form">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="民宿名称">
            <el-input :value="draft.homestayName" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="位置">
            <el-input :value="draft.homestayLocation" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="入住日期">
            <el-date-picker
              v-model="form.checkInDate"
              type="date"
              placeholder="选择入住日期"
              style="width: 100%"
              :disabled-date="disabledDate"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="退房日期">
            <el-date-picker
              v-model="form.checkOutDate"
              type="date"
              placeholder="选择退房日期"
              style="width: 100%"
              :disabled-date="disabledDate"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="入住人数" prop="guestCount">
            <el-input-number 
              v-model="form.guestCount" 
              :min="1" 
              :max="10"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input 
              v-model="form.contactPhone" 
              placeholder="请输入联系电话"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="总价">
        <div class="price-info">
          <span class="total-price">¥{{ calculatedTotalPrice }}</span>
          <span class="price-detail">（{{ calculatedNights }}晚 × ¥{{ draft.pricePerNight }}/晚）</span>
        </div>
      </el-form-item>

      <el-form-item label="特殊要求">
        <el-input
          v-model="form.specialRequests"
          type="textarea"
          :rows="3"
          placeholder="如有特殊需求请在此说明..."
        />
      </el-form-item>

      <el-form-item label="推荐理由">
        <div class="recommend-reason">{{ draft.recommendReason }}</div>
      </el-form-item>

      <!-- 不可用警告 -->
      <div v-if="!draft.isAvailable" class="availability-warning">
        <el-alert 
          title="注意：该时间段房间可能已满，建议联系客服确认或选择其他日期" 
          type="warning" 
          :closable="false" 
        />
      </div>
    </el-form>

    <div class="form-actions">
      <el-button @click="cancel">取消</el-button>
      <el-button 
        type="primary" 
        @click="confirm" 
        :loading="loading"
        :disabled="!isFormValid"
      >
        确认预订
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  draft: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const formRef = ref()
const loading = ref(false)

const form = ref({
  checkInDate: props.draft.checkInDate,
  checkOutDate: props.draft.checkOutDate,
  guestCount: props.draft.guestCount || 1,
  contactPhone: '',
  specialRequests: '',
  totalPrice: props.draft.totalPrice
})

const rules = {
  guestCount: [
    { required: true, message: '请输入入住人数', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

// 计算住宿天数
const calculatedNights = computed(() => {
  if (!form.value.checkInDate || !form.value.checkOutDate) return 0
  const checkIn = new Date(form.value.checkInDate)
  const checkOut = new Date(form.value.checkOutDate)
  const timeDiff = checkOut.getTime() - checkIn.getTime()
  return Math.max(1, Math.ceil(timeDiff / (1000 * 3600 * 24)))
})

// 计算总价
const calculatedTotalPrice = computed(() => {
  if (!props.draft.pricePerNight) return form.value.totalPrice
  return (props.draft.pricePerNight * calculatedNights.value).toFixed(2)
})

// 监听日期变化，更新总价
watch([() => form.value.checkInDate, () => form.value.checkOutDate], () => {
  if (props.draft.pricePerNight && calculatedNights.value > 0) {
    form.value.totalPrice = parseFloat(calculatedTotalPrice.value)
  }
})

// 表单验证
const isFormValid = computed(() => {
  return form.value.checkInDate && 
         form.value.checkOutDate && 
         form.value.guestCount > 0 && 
         form.value.contactPhone &&
         new Date(form.value.checkOutDate) > new Date(form.value.checkInDate)
})

// 日期禁用规则
const disabledDate = (time) => {
  // 禁用今天之前的日期
  return time.getTime() < Date.now() - 8.64e7
}

const confirm = async () => {
  try {
    await formRef.value.validate()
    
    if (new Date(form.value.checkOutDate) <= new Date(form.value.checkInDate)) {
      ElMessage.error('退房日期必须晚于入住日期')
      return
    }
    
    loading.value = true
    
    const bookingData = {
      userId: parseInt(localStorage.getItem('userId')),
      homestayId: props.draft.homestayId,
      checkInDate: form.value.checkInDate,
      checkOutDate: form.value.checkOutDate,
      guestCount: form.value.guestCount,
      contactPhone: form.value.contactPhone,
      specialRequests: form.value.specialRequests || '',
      totalPrice: parseFloat(calculatedTotalPrice.value)
    }
    
    emit('confirm', bookingData)
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    loading.value = false
  }
}

const cancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.booking-confirm-form {
  padding: 20px 0;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.total-price {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
}

.price-detail {
  font-size: 14px;
  color: #909399;
}

.recommend-reason {
  padding: 12px;
  background-color: #f5f7fa;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  color: #606266;
  line-height: 1.6;
}

.availability-warning {
  margin: 16px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
