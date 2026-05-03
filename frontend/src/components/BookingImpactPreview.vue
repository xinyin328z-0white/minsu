<template>
  <div class="impact-preview">
    <!-- 影响日期日历卡片 -->
    <div class="impact-summary">
      <h4 v-if="impactedDates.length > 0">
        预订影响日期总计 (共 {{ impactedDates.length }} 天)
      </h4>
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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { format, eachDayOfInterval, parseISO } from 'date-fns'
import zhCN from 'date-fns/locale/zh-CN'

const props = defineProps({
  homestayId: {
    type: Number,
    required: true
  },
  checkInDate: {
    type: String,
    required: true
  },
  checkOutDate: {
    type: String,
    required: true
  },
  currentAvailability: {
    type: Object,
    required: true
  },
  availableRooms: {
    type: Number,
    required: true
  }
})

const getDatesBetween = () => {
  const start = parseISO(props.checkInDate)
  const end = parseISO(props.checkOutDate)
  return eachDayOfInterval({ start, end }).slice(0, -1) // 排除退房日期
}

const impactedDates = computed(() => {
  const dates = getDatesBetween()
  const impact = []
  
  dates.forEach(date => {
    const dateStr = format(date, 'yyyy-MM-dd')
    const currentAvailable = props.currentAvailability[dateStr] || props.availableRooms
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

const getAvailabilityType = (available) => {
  if (available === 0) return 'danger'
  if (available === 1) return 'warning'
  return 'success'
}
</script>

<style scoped>
.impact-preview {
  padding: 0;
  background: transparent;
}

h4 {
  margin: 0 0 20px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.impact-summary {
  background: transparent;
  padding: 0;
  border: none;
}

.affected-dates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.date-card {
  border: 1px solid #DCDFE6;
  border-radius: 4px;
  overflow: hidden;
  background: white;
  transition: all 0.3s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.date-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.date-header {
  background: linear-gradient(135deg, #F56C6C, #FF9999);
  color: white;
  padding: 12px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
}

.date-details {
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  line-height: 1.4;
}

.detail-label {
  color: #606266;
  font-weight: 500;
  min-width: 45px;
}

.warning-row {
  color: #F56C6C;
  font-weight: 600;
  font-size: 13px;
  text-align: center;
  padding: 8px;
  background-color: #FFF1F0;
  border-radius: 3px;
  margin-top: 5px;
}

.no-impact-message {
  padding: 40px 20px;
  text-align: center;
}
</style>
