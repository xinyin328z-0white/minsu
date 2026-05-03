<template>
  <el-card class="room-calendar">
    <template #header>
      <div class="calendar-header">
        <span>房间日历 - {{ homestayName }}</span>
        <div class="controls">
          <el-date-picker
            v-model="calendarRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="onDateRangeChange"
            :disabled-date="disabledCalendarDate"
            style="width: 280px"
          />
          <el-button type="primary" @click="loadCalendar" :loading="loading">查询</el-button>
        </div>
      </div>
    </template>

    <el-table :data="calendarData" size="small" max-height="400">
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column label="余房数" width="100">
        <template #default="{ row }">
          <el-tag 
            :type="getRoomStatusType(row.available)" 
            :class="{ 'room-full': row.available === 0 }"
          >
            {{ row.available === 0 ? '已满' : `${row.available}间` }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span v-if="row.available === 0" class="status-text unavailable">不可选择</span>
          <span v-else class="status-text available">可预订</span>
        </template>
      </el-table-column>
      <el-table-column prop="remarks" label="备注" show-overflow-tooltip />
    </el-table>

    <div v-if="calendarData.length === 0" class="empty-state">
      请选择日期范围查询
    </div>
    <div v-else class="full-dates">
      <span class="full-dates-label">已满日期：</span>
      <template v-if="fullDates.length">
        <el-tag v-for="date in fullDates" :key="date" type="danger" class="full-date-tag">
          {{ date }}
        </el-tag>
      </template>
      <span v-else class="full-dates-empty">无</span>
    </div>
  </el-card>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoomCalendar } from '../api'

const props = defineProps({
  homestayId: {
    type: Number,
    required: true
  },
  homestayName: {
    type: String,
    default: '房间日历'
  },
  defaultRangeDays: {
    type: Number,
    default: 30
  },
  maxAdvanceDays: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['loaded'])

const calendarRange = ref([])
const calendarData = ref([])
const loading = ref(false)

const fullDates = computed(() =>
  calendarData.value
    .filter(item => item.available === 0)
    .map(item => item.date)
)

const getRoomStatusType = (available) => {
  if (available === 0) return 'danger'
  if (available === 1) return 'warning'
  return 'success'
}

const onDateRangeChange = () => {
  if (props.homestayId) {
    loadCalendar()
  }
}

const disabledCalendarDate = (time) => {
  if (time.getTime() < Date.now() - 24 * 60 * 60 * 1000) {
    return true
  }
  if (props.maxAdvanceDays) {
    const maxDate = new Date()
    maxDate.setDate(maxDate.getDate() + props.maxAdvanceDays)
    return time.getTime() > maxDate.getTime()
  }
  return false
}

const loadCalendar = async () => {
  if (!calendarRange.value || calendarRange.value.length !== 2) {
    ElMessage.warning('请选择完整的日期范围')
    return
  }

  loading.value = true
  try {
    const [startDate, endDate] = calendarRange.value
    const response = await getRoomCalendar(
      props.homestayId,
      toDateParam(startDate),
      toDateParam(endDate)
    )
    
    // 将返回的数据转换为表格格式
    const dailyData = response.data.dailyAvailability
    calendarData.value = Object.entries(dailyData)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([date, available]) => ({
        date: formatDate(new Date(date)),
        available: available,
        remarks: available === 0 ? '房间已满，暂不可预订' : '房间充足'
      }))
    emit('loaded', {
      dailyAvailability: dailyData,
      startDate: toDateParam(startDate),
      endDate: toDateParam(endDate)
    })
  } catch (err) {
    console.error('加载房间日历失败:', err)
    const errorMessage = err.response?.data?.message
      || err.response?.data?.error
      || err.message
      || '未知错误'
    ElMessage.error('加载房间日历失败: ' + errorMessage)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = ['日', '一', '二', '三', '四', '五', '六'][date.getDay()]
  return `${year}-${month}-${day} 周${dayOfWeek}`
}

const toDateParam = (date) => {
  if (date instanceof Date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return date
}

const setDefaultRange = () => {
  const start = new Date()
  const end = new Date()
  let rangeDays = props.defaultRangeDays
  if (props.maxAdvanceDays && rangeDays > props.maxAdvanceDays) {
    rangeDays = props.maxAdvanceDays
  }
  end.setDate(end.getDate() + rangeDays)
  calendarRange.value = [start, end]
}

const tryAutoLoad = () => {
  if (props.homestayId && calendarRange.value.length === 2) {
    loadCalendar()
  }
}

if (!calendarRange.value.length) {
  setDefaultRange()
  tryAutoLoad()
}

watch(
  () => props.homestayId,
  () => {
    tryAutoLoad()
  }
)
</script>

<style scoped>
.room-calendar {
  margin-bottom: 16px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.room-full {
  cursor: not-allowed;
  opacity: 0.6;
}

.status-text {
  font-weight: 600;
}

.status-text.available {
  color: #67c23a;
}

.status-text.unavailable {
  color: #f56c6c;
}

.empty-state {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
}

.full-dates {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.full-dates-label {
  font-size: 13px;
  color: #606266;
}

.full-date-tag {
  font-size: 12px;
}

.full-dates-empty {
  font-size: 12px;
  color: #909399;
}
</style>
