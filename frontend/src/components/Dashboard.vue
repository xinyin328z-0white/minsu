<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card card-blue">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">注册用户</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card card-green">
          <div class="stat-icon">🏠</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalHomestays }}</div>
            <div class="stat-label">民宿数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card card-orange">
          <div class="stat-icon">📋</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalBookings }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card card-red">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-value">¥{{ stats.totalRevenue.toLocaleString() }}</div>
            <div class="stat-label">总营收</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行小统计 -->
    <el-row :gutter="16" class="stat-cards-secondary">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card-mini">
          <div class="mini-stat">
            <span class="mini-label">待确认订单</span>
            <span class="mini-value warning">{{ stats.pendingBookings }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card-mini">
          <div class="mini-stat">
            <span class="mini-label">已确认订单</span>
            <span class="mini-value success">{{ stats.confirmedBookings }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card-mini">
          <div class="mini-stat">
            <span class="mini-label">游玩项目</span>
            <span class="mini-value primary">{{ stats.totalActivities }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card-mini">
          <div class="mini-stat">
            <span class="mini-label">旅游路线</span>
            <span class="mini-value info">{{ stats.totalRoutes }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">订单状态分布</span></template>
          <div class="chart-container">
            <canvas ref="statusChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">各民宿营收对比</span></template>
          <div class="chart-container">
            <canvas ref="revenueChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">月度订单趋势</span></template>
          <div class="chart-container">
            <canvas ref="monthlyChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">各民宿订单量</span></template>
          <div class="chart-container">
            <canvas ref="homestayBookingChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">客户消费排行 (Top 5)</span></template>
          <div class="chart-container">
            <canvas ref="userSpendingChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="chart-title">房间入住率</span></template>
          <div class="chart-container">
            <canvas ref="occupancyChartRef"></canvas>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  users: { type: Array, default: () => [] },
  bookings: { type: Array, default: () => [] },
  homestays: { type: Array, default: () => [] },
  activities: { type: Array, default: () => [] },
  routes: { type: Array, default: () => [] }
})

const statusChartRef = ref(null)
const revenueChartRef = ref(null)
const monthlyChartRef = ref(null)
const homestayBookingChartRef = ref(null)
const userSpendingChartRef = ref(null)
const occupancyChartRef = ref(null)
const charts = []

const stats = computed(() => {
  const bookings = props.bookings || []
  const totalRevenue = bookings
    .filter(b => b.status === 'CONFIRMED' || b.status === 'COMPLETED')
    .reduce((sum, b) => sum + (Number(b.totalPrice) || 0), 0)
  return {
    totalUsers: (props.users || []).length,
    totalHomestays: (props.homestays || []).length,
    totalBookings: bookings.length,
    totalRevenue: Math.round(totalRevenue),
    pendingBookings: bookings.filter(b => b.status === 'PENDING').length,
    confirmedBookings: bookings.filter(b => b.status === 'CONFIRMED').length,
    totalActivities: (props.activities || []).length,
    totalRoutes: (props.routes || []).length
  }
})

const destroyCharts = () => {
  charts.forEach(c => c.destroy())
  charts.length = 0
}

const createCharts = () => {
  destroyCharts()
  if (!props.bookings.length && !props.homestays.length) return

  // 1. 订单状态饼图
  const statusCounts = {}
  const statusLabels = { PENDING: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELED: '已取消' }
  props.bookings.forEach(b => {
    const label = statusLabels[b.status] || b.status
    statusCounts[label] = (statusCounts[label] || 0) + 1
  })
  if (statusChartRef.value) {
    charts.push(new Chart(statusChartRef.value, {
      type: 'doughnut',
      data: {
        labels: Object.keys(statusCounts),
        datasets: [{
          data: Object.values(statusCounts),
          backgroundColor: ['#E6A23C', '#67C23A', '#409EFF', '#909399']
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
    }))
  }

  // 2. 各民宿营收柱状图
  const homestayMap = {}
  props.homestays.forEach(h => { homestayMap[h.id] = h.name })
  const revenueByHomestay = {}
  props.bookings
    .filter(b => b.status === 'CONFIRMED' || b.status === 'COMPLETED')
    .forEach(b => {
      const name = homestayMap[b.homestayId] || `民宿${b.homestayId}`
      revenueByHomestay[name] = (revenueByHomestay[name] || 0) + (Number(b.totalPrice) || 0)
    })
  if (revenueChartRef.value) {
    charts.push(new Chart(revenueChartRef.value, {
      type: 'bar',
      data: {
        labels: Object.keys(revenueByHomestay),
        datasets: [{
          label: '营收（元）',
          data: Object.values(revenueByHomestay).map(v => Math.round(v)),
          backgroundColor: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59B6', '#1ABC9C']
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
    }))
  }

  // 3. 月度订单趋势折线图
  const monthlyData = {}
  props.bookings.forEach(b => {
    const date = b.checkInDate || b.createdAt
    if (!date) return
    const month = date.substring(0, 7) // YYYY-MM
    monthlyData[month] = (monthlyData[month] || 0) + 1
  })
  const sortedMonths = Object.keys(monthlyData).sort()
  if (monthlyChartRef.value) {
    charts.push(new Chart(monthlyChartRef.value, {
      type: 'line',
      data: {
        labels: sortedMonths,
        datasets: [{
          label: '订单数',
          data: sortedMonths.map(m => monthlyData[m]),
          borderColor: '#409EFF',
          backgroundColor: 'rgba(64,158,255,0.1)',
          fill: true,
          tension: 0.3
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
    }))
  }

  // 4. 各民宿订单量
  const bookingsByHomestay = {}
  props.bookings.forEach(b => {
    const name = homestayMap[b.homestayId] || `民宿${b.homestayId}`
    bookingsByHomestay[name] = (bookingsByHomestay[name] || 0) + 1
  })
  if (homestayBookingChartRef.value) {
    charts.push(new Chart(homestayBookingChartRef.value, {
      type: 'bar',
      data: {
        labels: Object.keys(bookingsByHomestay),
        datasets: [{
          label: '订单数',
          data: Object.values(bookingsByHomestay),
          backgroundColor: ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C', '#9B59B6', '#1ABC9C']
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
    }))
  }

  // 5. 客户消费排行
  const userMap = {}
  props.users.forEach(u => { userMap[u.id] = u.realName || u.username })
  const userSpending = {}
  props.bookings
    .filter(b => b.status === 'CONFIRMED' || b.status === 'COMPLETED')
    .forEach(b => {
      const name = userMap[b.userId] || `用户${b.userId}`
      userSpending[name] = (userSpending[name] || 0) + (Number(b.totalPrice) || 0)
    })
  const topUsers = Object.entries(userSpending).sort((a, b) => b[1] - a[1]).slice(0, 5)
  if (userSpendingChartRef.value) {
    charts.push(new Chart(userSpendingChartRef.value, {
      type: 'bar',
      data: {
        labels: topUsers.map(u => u[0]),
        datasets: [{
          label: '消费金额（元）',
          data: topUsers.map(u => Math.round(u[1])),
          backgroundColor: '#E6A23C'
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, indexAxis: 'y', plugins: { legend: { display: false } }, scales: { x: { beginAtZero: true } } }
    }))
  }

  // 6. 房间入住率（已确认+已完成订单数 / 总房间数的模拟百分比）
  const occupancy = {}
  props.homestays.forEach(h => {
    const confirmedCount = props.bookings.filter(
      b => b.homestayId === h.id && (b.status === 'CONFIRMED' || b.status === 'COMPLETED')
    ).length
    occupancy[h.name] = Math.min(100, Math.round((confirmedCount / Math.max(h.availableRooms, 1)) * 100))
  })
  if (occupancyChartRef.value) {
    charts.push(new Chart(occupancyChartRef.value, {
      type: 'bar',
      data: {
        labels: Object.keys(occupancy),
        datasets: [{
          label: '入住率（%）',
          data: Object.values(occupancy),
          backgroundColor: Object.values(occupancy).map(v =>
            v >= 80 ? '#F56C6C' : v >= 50 ? '#E6A23C' : '#67C23A'
          )
        }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100, ticks: { callback: v => v + '%' } } } }
    }))
  }
}

watch(() => [props.bookings, props.homestays, props.users], () => {
  nextTick(createCharts)
}, { deep: true })

onMounted(() => {
  nextTick(createCharts)
})

onBeforeUnmount(() => {
  destroyCharts()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 16px;
}

.stat-cards-secondary {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
  overflow: hidden;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
  gap: 16px;
}

.card-blue { border-left: 4px solid #409EFF; }
.card-green { border-left: 4px solid #67C23A; }
.card-orange { border-left: 4px solid #E6A23C; }
.card-red { border-left: 4px solid #F56C6C; }

.stat-icon {
  font-size: 36px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stat-card-mini {
  border-radius: 8px;
}

.stat-card-mini :deep(.el-card__body) {
  padding: 14px 18px;
}

.mini-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mini-label {
  font-size: 13px;
  color: #606266;
}

.mini-value {
  font-size: 22px;
  font-weight: 700;
}

.mini-value.warning { color: #E6A23C; }
.mini-value.success { color: #67C23A; }
.mini-value.primary { color: #409EFF; }
.mini-value.info { color: #909399; }

.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  border-radius: 12px;
  margin-bottom: 0;
}

.chart-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.chart-container {
  height: 280px;
  position: relative;
}
</style>
