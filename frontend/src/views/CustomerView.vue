<template>
  <el-container class="customer-layout">
    <el-header class="customer-header">
      <div class="title">民宿综合管理平台（用户端）</div>
      <div class="user-info">
        <span>欢迎 {{ username }}</span>
        <el-button type="danger" text @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main>
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 为你推荐 Tab -->
        <el-tab-pane label="为你推荐" name="for-you">
          <ForYouRecommendation @book-homestay="bookHomestay" />
        </el-tab-pane>

        <!-- AI旅行规划 Tab -->
        <el-tab-pane label="AI旅行规划" name="ai-plan">
          <AiRecommendation />
        </el-tab-pane>

        <!-- 首页 Tab -->
        <el-tab-pane label="民宿浏览" name="home">
          <!-- 搜索和筛选区域 -->
          <div class="homestay-filter-section">
            <el-row :gutter="16" align="middle">
              <el-col :xs="24" :sm="8" :md="6">
                <el-input
                  v-model="homestaySearch"
                  placeholder="搜索民宿名称..."
                  clearable
                  prefix-icon="Search"
                  class="search-input"
                />
              </el-col>
              <el-col :xs="12" :sm="6" :md="4">
                <el-select v-model="locationFilter" placeholder="位置筛选" clearable style="width: 100%;">
                  <el-option
                    v-for="loc in uniqueLocations"
                    :key="loc"
                    :label="loc"
                    :value="loc"
                  />
                </el-select>
              </el-col>
              <el-col :xs="12" :sm="6" :md="4">
                <el-select v-model="priceSort" placeholder="价格排序" clearable style="width: 100%;">
                  <el-option label="价格从低到高" value="asc" />
                  <el-option label="价格从高到低" value="desc" />
                </el-select>
              </el-col>
              <el-col :xs="24" :sm="4" :md="4">
                <el-checkbox v-model="showAvailableOnly" label="仅显示有房" />
              </el-col>
              <el-col :xs="24" :sm="24" :md="6" class="view-toggle">
                <el-radio-group v-model="viewMode" size="small">
                  <el-radio-button value="card">
                    <el-icon><Grid /></el-icon> 卡片
                  </el-radio-button>
                  <el-radio-button value="list">
                    <el-icon><List /></el-icon> 列表
                  </el-radio-button>
                </el-radio-group>
              </el-col>
            </el-row>
          </div>

          <!-- 统计信息 -->
          <div class="homestay-stats">
            <span class="stats-text">
              共 <strong>{{ filteredHomestays.length }}</strong> 家民宿
              <template v-if="homestaySearch || locationFilter || showAvailableOnly">
                （筛选自 {{ homestays.length }} 家）
              </template>
            </span>
          </div>

          <!-- 卡片视图 -->
          <div v-if="viewMode === 'card'" class="homestay-grid">
            <el-row :gutter="20">
              <el-col 
                v-for="homestay in paginatedHomestays" 
                :key="homestay.id" 
                :xs="24" 
                :sm="12" 
                :md="8" 
                :lg="6"
                class="homestay-col"
              >
                <el-card class="homestay-card" :body-style="{ padding: '0' }" shadow="hover" @click="showHomestayDetail(homestay)">
                  <!-- 图片区域 -->
                  <div class="homestay-image" :style="getHomestayImageStyle(homestay)">
                    <div v-if="!homestay.imageUrl" class="homestay-image-overlay">
                      <el-icon class="homestay-icon"><House /></el-icon>
                    </div>
                    <!-- 点击查看提示 -->
                    <div class="click-hint">
                      <el-icon><View /></el-icon>
                      <span>点击查看详情</span>
                    </div>
                    <!-- 状态标签 -->
                    <div class="homestay-badges">
                      <el-tag 
                        v-if="getHomestayRoomStatus(homestay).isFull" 
                        type="danger" 
                        size="small"
                        effect="dark"
                      >
                        已满房
                      </el-tag>
                      <el-tag 
                        v-else-if="getAvailableRoomCount(homestay) <= 2" 
                        type="warning" 
                        size="small"
                        effect="dark"
                      >
                        仅剩{{ getAvailableRoomCount(homestay) }}间
                      </el-tag>
                      <el-tag 
                        v-else 
                        type="success" 
                        size="small"
                        effect="dark"
                      >
                        可预订
                      </el-tag>
                    </div>
                    <!-- 价格标签 -->
                    <div class="price-badge">
                      <span class="price-symbol">¥</span>
                      <span class="price-value">{{ homestay.pricePerNight }}</span>
                      <span class="price-unit">/晚</span>
                    </div>
                  </div>
                  
                  <!-- 内容区域 -->
                  <div class="homestay-content">
                    <h3 class="homestay-name">{{ homestay.name }}</h3>
                    <div class="homestay-location">
                      <el-icon><Location /></el-icon>
                      <span>{{ homestay.location }}</span>
                    </div>
                    <p class="homestay-desc">{{ homestay.description || '暂无描述' }}</p>
                    
                    <!-- 房间信息 -->
                    <div class="homestay-info">
                      <div class="info-item">
                        <el-icon><Key /></el-icon>
                        <span>{{ homestay.availableRooms }} 间房</span>
                      </div>
                      <div v-if="homestay.roomNotes" class="info-item">
                        <el-icon><InfoFilled /></el-icon>
                        <el-tooltip :content="homestay.roomNotes" placement="top">
                          <span class="notes-text">{{ truncateText(homestay.roomNotes, 10) }}</span>
                        </el-tooltip>
                      </div>
                    </div>
                    
                    <!-- 操作按钮 -->
                    <div class="homestay-actions">
                      <el-button
                        type="primary"
                        :disabled="getHomestayRoomStatus(homestay).isFull"
                        @click.stop="bookHomestay(homestay)"
                        class="book-btn"
                      >
                        <el-icon><Calendar /></el-icon>
                        {{ getHomestayRoomStatus(homestay).isFull ? '暂无空房' : '立即预订' }}
                      </el-button>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            
            <!-- 空状态 -->
            <el-empty v-if="filteredHomestays.length === 0" description="没有找到符合条件的民宿">
              <el-button type="primary" @click="resetFilters">重置筛选条件</el-button>
            </el-empty>
          </div>

          <!-- 列表视图 -->
          <div v-else class="homestay-list-view">
            <el-table :data="paginatedHomestays" size="default" stripe>
              <el-table-column prop="name" label="民宿名称" min-width="150">
                <template #default="{ row }">
                  <div class="list-name-cell">
                    <div class="list-avatar" :style="{ background: getHomestayGradient(row.id) }">
                      <el-icon><House /></el-icon>
                    </div>
                    <span class="list-name">{{ row.name }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="location" label="位置" width="120">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.location }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="availableRooms" label="房间数" width="90" align="center">
                <template #default="{ row }">
                  <span class="room-count">{{ row.availableRooms }} 间</span>
                </template>
              </el-table-column>
              <el-table-column prop="pricePerNight" label="价格/晚" width="120" align="right">
                <template #default="{ row }">
                  <span class="list-price">¥{{ row.pricePerNight }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag 
                    :type="getHomestayRoomStatus(row).isFull ? 'danger' : 'success'" 
                    size="small"
                  >
                    {{ getHomestayRoomStatus(row).isFull ? '已满房' : '可预订' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="bookHomestay(row)"
                    :disabled="getHomestayRoomStatus(row).isFull"
                  >
                    预订
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <!-- 空状态 -->
            <el-empty v-if="filteredHomestays.length === 0" description="没有找到符合条件的民宿">
              <el-button type="primary" @click="resetFilters">重置筛选条件</el-button>
            </el-empty>
          </div>

          <!-- 分页 -->
          <div v-if="filteredHomestays.length > 0" class="homestay-pagination">
            <el-pagination
              v-model:current-page="homestayPage"
              v-model:page-size="homestayPageSize"
              :page-sizes="[8, 12, 16, 24]"
              :total="filteredHomestays.length"
              layout="total, sizes, prev, pager, next"
              background
            />
          </div>
        </el-tab-pane>
        
        <!-- 我的订单 Tab -->
        <el-tab-pane label="我的订单" name="orders">
          <el-card>
            <template #header>
              <div style="display: flex; justify-content: space-between;">
                <span>订单管理</span>
                <el-button type="primary" size="small" @click="loadBookings">刷新</el-button>
              </div>
            </template>
            <el-table :data="paginatedBookings" size="default">
              <el-table-column prop="id" label="订单ID" width="80" />
              <el-table-column prop="homestayId" label="民宿ID" width="100" />
              <el-table-column prop="checkInDate" label="入住日期" width="120" />
              <el-table-column prop="checkOutDate" label="退房日期" width="120" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalPrice" label="金额" width="100" />
              <el-table-column prop="guestCount" label="人数" width="80" />
              <el-table-column prop="contactPhone" label="联系电话" width="120" />
              <el-table-column prop="createdAt" label="创建时间" width="160" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING'" link type="danger" size="small" @click="cancelBooking(row)">取消订单</el-button>
                  <el-button link type="primary" size="small" @click="viewBookingDetails(row)">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="bookingPage"
              v-model:page-size="bookingPageSize"
              :page-sizes="[5, 10, 20, 50]"
              :total="bookings.length"
              layout="total, sizes, prev, pager, next, jumper"
              style="margin-top: 16px; justify-content: center;"
            />
          </el-card>
        </el-tab-pane>
        
        <!-- 客服咨询 Tab -->
        <el-tab-pane name="chat">
          <template #label>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="tab-badge">
              <span>客服咨询</span>
            </el-badge>
          </template>
          <ChatWindow :userId="userId" title="客服消息" @messagesUpdated="handleMessagesUpdated" />
        </el-tab-pane>
        
        <!-- 个人中心 Tab -->
        <el-tab-pane label="个人中心" name="profile">
          <UserProfile :userId="userId" />
        </el-tab-pane>
      </el-tabs>
    </el-main>

    <!-- Homestay Detail Dialog -->
    <el-dialog v-model="detailDialog" :title="detailHomestay?.name || '民宿详情'" width="600px" class="detail-dialog">
      <div v-if="detailHomestay" class="detail-content">
        <!-- 头部图片区域 -->
        <div class="detail-header" :style="getHomestayImageStyle(detailHomestay)">
          <el-icon v-if="!detailHomestay.imageUrl" class="detail-icon"><House /></el-icon>
          <div class="detail-price">
            <span class="price-symbol">¥</span>
            <span class="price-value">{{ detailHomestay.pricePerNight }}</span>
            <span class="price-unit">/晚</span>
          </div>
        </div>
        
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="民宿名称" :span="2">
              <strong>{{ detailHomestay.name }}</strong>
            </el-descriptions-item>
            <el-descriptions-item label="位置">
              <el-icon><Location /></el-icon> {{ detailHomestay.location }}
            </el-descriptions-item>
            <el-descriptions-item label="房间数">
              <el-tag type="primary" size="small">{{ detailHomestay.availableRooms }} 间</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="价格">
              <span class="detail-price-text">¥{{ detailHomestay.pricePerNight }}/晚</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getHomestayRoomStatus(detailHomestay).isFull ? 'danger' : 'success'">
                {{ getHomestayRoomStatus(detailHomestay).isFull ? '已满房' : '可预订' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <!-- 描述信息 -->
        <div class="detail-section">
          <h4 class="section-title">民宿描述</h4>
          <div class="detail-desc-box">
            {{ detailHomestay.description || '暂无描述信息' }}
          </div>
        </div>
        
        <!-- 房间注意事项 -->
        <div v-if="detailHomestay.roomNotes" class="detail-section">
          <h4 class="section-title">房间注意事项</h4>
          <el-alert type="info" :closable="false" show-icon>
            <template #default>
              <div class="room-notes-content">{{ detailHomestay.roomNotes }}</div>
            </template>
          </el-alert>
        </div>
        
        <!-- 房间可用情况 -->
        <div class="detail-section">
          <h4 class="section-title">近期房间情况（{{ bookingWindowDays }}天内）</h4>
          <div v-if="detailCalendarLoading" class="loading-box">
            <el-icon class="is-loading"><Loading /></el-icon> 加载中...
          </div>
          <div v-else-if="detailEmptyDays > 0 || detailLimitedDays > 0" class="availability-summary">
            <el-tag v-if="detailEmptyDays > 0" type="danger" size="small" style="margin-right: 8px;">
              {{ detailEmptyDays }} 天满房
            </el-tag>
            <el-tag v-if="detailLimitedDays > 0" type="warning" size="small">
              {{ detailLimitedDays }} 天仅剩1间
            </el-tag>
          </div>
          <div v-else class="availability-summary">
            <el-tag type="success" size="small">近期房源充足</el-tag>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
        <el-button 
          type="primary" 
          @click="bookFromDetail"
          :disabled="detailHomestay && getHomestayRoomStatus(detailHomestay).isFull"
        >
          <el-icon><Calendar /></el-icon>
          {{ detailHomestay && getHomestayRoomStatus(detailHomestay).isFull ? '暂无空房' : '立即预订' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Booking Dialog -->
    <el-dialog v-model="bookingDialog" title="预订民宿">
      <el-form :model="bookingForm" label-width="100px">
        <el-form-item label="民宿">
          {{ bookingForm.homestayName }}
        </el-form-item>
        <el-form-item label="选择日期">
          <div style="margin-bottom: 8px; font-size: 12px; color: #909399;">
            仅可预订{{ bookingWindowDays }}天内 · 灰化日期为满房
          </div>
          <el-date-picker 
            v-model="bookingDateRange" 
            type="daterange"
            range-separator="至"
            start-placeholder="入住日期"
            end-placeholder="退房日期"
            style="width: 100%;"
            :disabled-date="disabledDate"
            @change="onBookingDateChange"
          />
        </el-form-item>
        <el-form-item v-if="!bookingDateRange || bookingDateRange.length === 0" label="房间情况">
          <el-alert v-if="emptyDaysCount > 0" type="warning" :closable="false" style="margin-bottom: 8px;">
            <template #default>
              <div>{{ bookingWindowDays }}天内有{{ emptyDaysCount }}天房间已满</div>
              <div style="margin-top: 4px; font-size: 12px;">满房日期：{{ emptyDates.join('、') }}</div>
            </template>
          </el-alert>
          <el-alert v-if="limitedDaysCount > 0" type="info" :closable="false" style="margin-bottom: 8px;">
            <template #default>
              <div>{{ bookingWindowDays }}天内有{{ limitedDaysCount }}天仅剩1间房</div>
              <div style="margin-top: 4px; font-size: 12px;">仅剩日期：{{ limitedDates.join('、') }}</div>
            </template>
          </el-alert>
        </el-form-item>
        <el-form-item v-if="calendarData.length > 0" label="房间日历">
          <el-table :data="calendarData" size="small" max-height="300" stripe>
            <el-table-column prop="date" label="日期" width="150" />
            <el-table-column label="余房数" width="100">
              <template #default="{ row }">
                <el-tag 
                  :type="getRoomStatusType(row.available)" 
                  :class="{ 'room-full': row.available === 0, 'room-limited': row.available === 1 }"
                >
                  {{ row.available === 0 ? '已满' : row.available === 1 ? '仅剩1间' : `${row.available}间` }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span v-if="row.available === 0" class="status-unavailable">不可选</span>
                <span v-else class="status-available">可选</span>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="入住人数">
          <el-input-number v-model="bookingForm.guestCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="bookingForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="特殊要求">
          <el-input v-model="bookingForm.specialRequests" type="textarea" :rows="3" placeholder="如有特殊要求请填写" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="bookingDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBooking">确认预订</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { House, Location, Key, InfoFilled, Calendar, Grid, List, Search, View, Loading } from '@element-plus/icons-vue'
import ChatWindow from '../components/ChatWindow.vue'
import UserProfile from '../components/UserProfile.vue'
import RoomCalendar from '../components/RoomCalendar.vue'
import AiRecommendation from '../components/AiRecommendation.vue'
import ForYouRecommendation from '../components/ForYouRecommendation.vue'
import {
  fetchHomestays,
  fetchRoutes,
  fetchActivities,
  fetchBookingsByUser,
  fetchMessagesByUser,
  checkRoomConflict,
  createBooking,
  updateBooking,
  getRoomCalendar,
  getBookingWindowDays,
  getImageUrl
} from '../api'

const router = useRouter()
const userId = ref(Number(localStorage.getItem('userId')))
const username = ref(localStorage.getItem('username'))

const activeTab = ref('for-you')
const homestays = ref([])
const routes = ref([])
const activities = ref([])
const bookings = ref([])
const recommendation = ref('')
const unreadCount = ref(0)
let unreadPollTimer = null

// 民宿浏览相关
const homestaySearch = ref('')
const locationFilter = ref('')
const priceSort = ref('')
const showAvailableOnly = ref(false)
const viewMode = ref('card')
const homestayPage = ref(1)
const homestayPageSize = ref(8)

// 计算唯一位置列表
const uniqueLocations = computed(() => {
  const locations = homestays.value.map(h => h.location).filter(Boolean)
  return [...new Set(locations)]
})

// 筛选和排序后的民宿列表
const filteredHomestays = computed(() => {
  let result = [...homestays.value]
  
  // 搜索过滤
  if (homestaySearch.value) {
    const keyword = homestaySearch.value.toLowerCase()
    result = result.filter(h => 
      h.name?.toLowerCase().includes(keyword) || 
      h.description?.toLowerCase().includes(keyword)
    )
  }
  
  // 位置过滤
  if (locationFilter.value) {
    result = result.filter(h => h.location === locationFilter.value)
  }
  
  // 仅显示有房
  if (showAvailableOnly.value) {
    result = result.filter(h => !getHomestayRoomStatus(h).isFull)
  }
  
  // 价格排序
  if (priceSort.value === 'asc') {
    result.sort((a, b) => (a.pricePerNight || 0) - (b.pricePerNight || 0))
  } else if (priceSort.value === 'desc') {
    result.sort((a, b) => (b.pricePerNight || 0) - (a.pricePerNight || 0))
  }
  
  return result
})

// 分页后的民宿列表
const paginatedHomestays = computed(() => {
  const start = (homestayPage.value - 1) * homestayPageSize.value
  const end = start + homestayPageSize.value
  return filteredHomestays.value.slice(start, end)
})

// 获取民宿渐变背景色
const homestayGradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
]

const getHomestayGradient = (id) => {
  return homestayGradients[id % homestayGradients.length]
}

// 获取民宿图片样式（真实图片或渐变色占位）
const getHomestayImageStyle = (homestay) => {
  if (homestay.imageUrl) {
    const url = getImageUrl(homestay.imageUrl)
    return {
      backgroundImage: `url(${url})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: getHomestayGradient(homestay.id)
  }
}

// 获取可用房间数
const getAvailableRoomCount = (homestay) => {
  const availability = homestayAvailabilityMap.value[homestay.id]
  if (!availability) return homestay.availableRooms
  const values = Object.values(availability)
  if (values.length === 0) return homestay.availableRooms
  // 返回未来日期中最小的可用房间数
  return Math.min(...values)
}

// 截断文本
const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

// 民宿详情相关
const detailDialog = ref(false)
const detailHomestay = ref(null)
const detailCalendarLoading = ref(false)
const detailEmptyDays = ref(0)
const detailLimitedDays = ref(0)

// 显示民宿详情
const showHomestayDetail = async (homestay) => {
  detailHomestay.value = homestay
  detailDialog.value = true
  detailCalendarLoading.value = true
  detailEmptyDays.value = 0
  detailLimitedDays.value = 0
  
  try {
    const { startDate, endDate } = getWindowRange()
    const { data } = await getRoomCalendar(homestay.id, startDate, endDate)
    const availability = data.dailyAvailability || {}
    
    // 统计满房和仅剩1间的天数
    Object.values(availability).forEach(available => {
      if (available === 0) detailEmptyDays.value++
      else if (available === 1) detailLimitedDays.value++
    })
  } catch (err) {
    console.error('加载房间日历失败:', err)
  } finally {
    detailCalendarLoading.value = false
  }
}

// 从详情页预订
const bookFromDetail = () => {
  if (detailHomestay.value) {
    detailDialog.value = false
    bookHomestay(detailHomestay.value)
  }
}

// 重置筛选条件
const resetFilters = () => {
  homestaySearch.value = ''
  locationFilter.value = ''
  priceSort.value = ''
  showAvailableOnly.value = false
}

// 分页相关
const bookingPage = ref(1)
const bookingPageSize = ref(10)

const paginatedBookings = computed(() => {
  const start = (bookingPage.value - 1) * bookingPageSize.value
  const end = start + bookingPageSize.value
  return bookings.value.slice(start, end)
})

const bookingDialog = ref(false)
const bookingForm = ref({})
const roomAvailabilityMap = ref({})
const homestayAvailabilityMap = ref({})
const bookingWindowDays = ref(60)
const bookingDateRange = ref([])
const calendarData = ref([])
const emptyDaysCount = ref(0)
const limitedDaysCount = ref(0)
const totalRooms = ref(0)
const emptyDates = ref([])
const limitedDates = ref([])

const getRoomStatusType = (available) => {
  if (available === 0) return 'danger'
  if (available === 1) return 'warning'
  return 'success'
}

const loadHomestays = async () => {
  try {
    const { data } = await fetchHomestays()
    homestays.value = data
    await loadHomestayAvailability(data)
  } catch (err) {
    console.error('加载民宿列表失败:', err)
  }
}

const loadRoutes = async () => {
  try {
    const { data } = await fetchRoutes()
    routes.value = data
  } catch (err) {
    console.error('加载路线失败:', err)
  }
}

const loadActivities = async () => {
  try {
    const { data } = await fetchActivities()
    activities.value = data
  } catch (err) {
    console.error('加载活动失败:', err)
  }
}

const loadBookings = async () => {
  try {
    const { data } = await fetchBookingsByUser(userId.value)
    bookings.value = data
  } catch (err) {
    console.error('加载订单失败:', err)
    ElMessage.error('加载订单失败')
  }
}

const loadUnreadCount = async () => {
  try {
    const { data } = await fetchMessagesByUser(userId.value)
    const newUnreadCount = (data || []).filter(m => m.type === 'ADMIN_REPLY' && !m.isRead).length
    
    // 如果未读消息增加了，说明收到了新消息
    if (newUnreadCount > unreadCount.value && activeTab.value !== 'chat') {
      ElMessage.info('您有新的客服消息')
    }
    
    unreadCount.value = newUnreadCount
  } catch (err) {
    console.error('加载未读消息数失败:', err)
  }
}

// 处理 ChatWindow 组件的消息更新事件
const handleMessagesUpdated = ({ unreadCount: count, hasNewMessages }) => {
  // 当用户在聊天窗口时，消息已被标记为已读
  if (activeTab.value === 'chat') {
    unreadCount.value = 0
  }
}

const bookHomestay = async (homestay) => {
  bookingForm.value = {
    homestayId: homestay.id,
    homestayName: homestay.name,
    checkInDate: null,
    checkOutDate: null,
    guestCount: 1,
    contactPhone: '',
    specialRequests: ''
  }
  roomAvailabilityMap.value = {}
  bookingDateRange.value = []
  calendarData.value = []
  // 预加载房间日历数据用于日期选择器禁用显示
  await preloadRoomAvailability(homestay.id)
  bookingDialog.value = true
}

const preloadRoomAvailability = async (homestayId) => {
  try {
    const { startDate, endDate } = getWindowRange()
    const { data } = await getRoomCalendar(homestayId, startDate, endDate)
    roomAvailabilityMap.value = data.dailyAvailability || {}
    
    // 统计空房和限制房的日期和数量
    emptyDates.value = []
    limitedDates.value = []
    Object.entries(roomAvailabilityMap.value).forEach(([date, available]) => {
      const dateStr = formatDateDisplay(new Date(date))
      const bookedRooms = totalRooms.value - available
      if (available === 0) {
        emptyDates.value.push(`${dateStr}（满${bookedRooms}间）`)
      } else if (available === 1) {
        limitedDates.value.push(dateStr)
      }
    })
    emptyDaysCount.value = emptyDates.value.length
    limitedDaysCount.value = limitedDates.value.length
  } catch (err) {
    console.error('预加载房间可用性失败:', err)
  }
}

const loadBookingWindowDays = async () => {
  try {
    const { data } = await getBookingWindowDays()
    bookingWindowDays.value = data.bookingWindowDays || 60
  } catch (err) {
    console.error('加载预订天数设置失败:', err)
  }
}

const getWindowRange = () => {
  const start = new Date()
  const end = new Date()
  end.setDate(end.getDate() + bookingWindowDays.value)
  return {
    startDate: toDateParam(start),
    endDate: toDateParam(end)
  }
}

const loadHomestayAvailability = async (items) => {
  if (!items || items.length === 0) return
  const { startDate, endDate } = getWindowRange()
  const results = await Promise.all(items.map(async (homestay) => {
    try {
      const { data } = await getRoomCalendar(homestay.id, startDate, endDate)
      return [homestay.id, data.dailyAvailability || {}]
    } catch (err) {
      console.error('加载房间日历失败:', err)
      return [homestay.id, null]
    }
  }))

  const map = {}
  results.forEach(([id, availability]) => {
    if (availability) {
      map[id] = availability
    }
  })
  homestayAvailabilityMap.value = map
}

const disabledDate = (time) => {
  // 禁用过去的日期
  if (time.getTime() < Date.now() - 24 * 60 * 60 * 1000) {
    return true
  }
  const maxDate = new Date()
  maxDate.setDate(maxDate.getDate() + bookingWindowDays.value)
  if (time.getTime() > maxDate.getTime()) {
    return true
  }
  // 检查房间可用性 - 如果该日期房间为0则禁用
  const dateKey = `${time.getFullYear()}-${String(time.getMonth() + 1).padStart(2, '0')}-${String(time.getDate()).padStart(2, '0')}`
  if (roomAvailabilityMap.value && Object.prototype.hasOwnProperty.call(roomAvailabilityMap.value, dateKey)) {
    return roomAvailabilityMap.value[dateKey] === 0
  }
  return false
}
const handleCalendarLoaded = (payload) => {
  roomAvailabilityMap.value = payload.dailyAvailability || {}
}

const onBookingDateChange = () => {
  if (bookingDateRange.value && bookingDateRange.value.length === 2) {
    const [checkIn, checkOut] = bookingDateRange.value
    bookingForm.value.checkInDate = checkIn
    bookingForm.value.checkOutDate = checkOut
    loadBookingCalendar()
  } else {
    calendarData.value = []
  }
}

const loadBookingCalendar = async () => {
  if (!bookingDateRange.value || bookingDateRange.value.length !== 2) {
    calendarData.value = []
    return
  }
  try {
    const [startDate, endDate] = bookingDateRange.value
    const { data } = await getRoomCalendar(
      bookingForm.value.homestayId,
      toDateParam(startDate),
      toDateParam(endDate)
    )
    const dailyData = data.dailyAvailability || {}
    calendarData.value = Object.entries(dailyData)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([date, available]) => ({
        date: formatDateDisplay(new Date(date)),
        available: available
      }))
    roomAvailabilityMap.value = dailyData
  } catch (err) {
    console.error('加载预订日历失败:', err)
  }
}

const formatDateDisplay = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = ['日', '一', '二', '三', '四', '五', '六'][date.getDay()]
  return `${year}-${month}-${day} 周${dayOfWeek}`
}

const submitBooking = async () => {
  if (!bookingForm.value.checkInDate || !bookingForm.value.checkOutDate) {
    ElMessage.error('请选择入住和退房日期')
    return
  }
  
  if (!bookingForm.value.contactPhone) {
    ElMessage.error('请填写联系电话')
    return
  }
  
  // 格式化日期为 yyyy-MM-dd 格式
  const formatDate = (date) => {
    if (!date) return null
    if (typeof date === 'string') return date
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  
  const checkInDateStr = formatDate(bookingForm.value.checkInDate)
  const checkOutDateStr = formatDate(bookingForm.value.checkOutDate)
  
  // 检查房间冲突
  try {
    const conflictRes = await checkRoomConflict(
      bookingForm.value.homestayId,
      checkInDateStr,
      checkOutDateStr
    )
    
    if (conflictRes.data.hasConflict) {
      ElMessage.warning(conflictRes.data.message)
      return
    }
  } catch (err) {
    console.error('检查房间冲突失败:', err)
    ElMessage.error('检查房间冲突失败: ' + (err.response?.data?.message || err.message))
    return
  }
  
  try {
    await createBooking({
      userId: userId.value,
      homestayId: bookingForm.value.homestayId,
      checkInDate: checkInDateStr,
      checkOutDate: checkOutDateStr,
      guestCount: bookingForm.value.guestCount,
      contactPhone: bookingForm.value.contactPhone,
      specialRequests: bookingForm.value.specialRequests,
      status: 'PENDING'
    })
    bookingDialog.value = false
    await loadBookings()
    ElMessage.success('预订成功！等待商家确认')
  } catch (err) {
    ElMessage.error('预订失败: ' + (err.response?.data || err.message))
  }
}

const cancelBooking = async (booking) => {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await updateBooking(booking.id, { ...booking, status: 'CANCELED' })
    await loadBookings()
    ElMessage.success('订单已取消')
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const viewBookingDetails = (booking) => {
  ElMessageBox.alert(
    `订单ID: ${booking.id}\n民宿ID: ${booking.homestayId}\n入住日期: ${booking.checkInDate}\n退房日期: ${booking.checkOutDate}\n入住人数: ${booking.guestCount || '未填写'}\n联系电话: ${booking.contactPhone || '未填写'}\n特殊要求: ${booking.specialRequests || '无'}\n总价: ${booking.totalPrice || '待确认'}\n状态: ${getStatusText(booking.status)}`,
    '订单详情',
    { confirmButtonText: '确定' }
  )
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'CONFIRMED': 'success',
    'CANCELED': 'info',
    'COMPLETED': 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'CANCELED': '已取消',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const getHomestayRoomStatus = (homestay) => {
  const availability = homestayAvailabilityMap.value[homestay.id]
  const values = availability ? Object.values(availability) : []
  const isFull = values.length > 0 && values.every(v => v === 0)
  return {
    isFull,
    windowDays: bookingWindowDays.value
  }
}

const toDateParam = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(async () => {
  await loadBookingWindowDays()
  await loadHomestays()
  loadRoutes()
  loadActivities()
  loadBookings()
  await loadUnreadCount()
  // 每3秒轮询一次未读消息，提供更好的实时体验
  unreadPollTimer = setInterval(loadUnreadCount, 3000)
})

onUnmounted(() => {
  if (unreadPollTimer) {
    clearInterval(unreadPollTimer)
    unreadPollTimer = null
  }
})

watch(activeTab, (tab) => {
  if (tab === 'chat') {
    setTimeout(loadUnreadCount, 500)
  }
})

// 筛选条件变化时重置分页到第一页
watch([homestaySearch, locationFilter, priceSort, showAvailableOnly], () => {
  homestayPage.value = 1
})
</script>

<style scoped>
.customer-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.customer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 民宿浏览筛选区域 */
.homestay-filter-section {
  background: white;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.search-input {
  width: 100%;
}

.view-toggle {
  display: flex;
  justify-content: flex-end;
}

.homestay-stats {
  margin-bottom: 16px;
  padding: 0 4px;
}

.stats-text {
  color: #606266;
  font-size: 14px;
}

.stats-text strong {
  color: #409eff;
  font-size: 16px;
}

/* 民宿卡片网格 */
.homestay-grid {
  margin-bottom: 20px;
}

.homestay-col {
  margin-bottom: 20px;
}

.homestay-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: 100%;
}

.homestay-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

/* 民宿图片区域 */
.homestay-image {
  height: 160px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.homestay-image-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
}

.homestay-icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.9);
}

.homestay-badges {
  position: absolute;
  top: 12px;
  left: 12px;
}

.price-badge {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 12px;
  margin-right: 2px;
}

.price-value {
  font-size: 20px;
  font-weight: 700;
}

.price-unit {
  font-size: 11px;
  opacity: 0.8;
  margin-left: 2px;
}

/* 民宿内容区域 */
.homestay-content {
  padding: 16px;
}

.homestay-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.homestay-location {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.homestay-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.homestay-info {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.notes-text {
  cursor: pointer;
  color: #409eff;
}

.homestay-actions {
  margin-top: 12px;
}

.book-btn {
  width: 100%;
  border-radius: 8px;
  font-weight: 500;
}

/* 列表视图 */
.homestay-list-view {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.list-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.list-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.list-name {
  font-weight: 500;
  color: #303133;
}

.list-price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 15px;
}

.room-count {
  color: #409eff;
  font-weight: 500;
}

/* 分页 */
.homestay-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.mt-16 {
  margin-top: 16px;
}

.recommendation {
  margin-top: 8px;
}

.recommendation .title {
  font-weight: 600;
  margin-bottom: 4px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translate(6px, -6px);
  animation: badge-pulse 1.5s infinite;
}

@keyframes badge-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.6);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(245, 108, 108, 0);
  }
}

.room-full {
  cursor: not-allowed;
  opacity: 0.6;
}

.room-limited {
  background-color: #fde7d6 !important;
  border-color: #f5b75d !important;
  color: #c37f1a !important;
}

.status-available {
  color: #67c23a;
  font-weight: 600;
}

.status-unavailable {
  color: #f56c6c;
  font-weight: 600;
}

/* 点击查看提示 */
.click-hint {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.homestay-card:hover .click-hint {
  opacity: 1;
}

.homestay-card {
  cursor: pointer;
}

/* 民宿详情对话框样式 */
.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-content {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-header {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.detail-icon {
  font-size: 56px;
  color: rgba(255, 255, 255, 0.9);
}

.detail-price {
  position: absolute;
  bottom: 12px;
  right: 16px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  display: flex;
  align-items: baseline;
}

.detail-section {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-child {
  border-bottom: none;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.detail-desc-box {
  background: #f9f9f9;
  padding: 12px 16px;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.room-notes-content {
  line-height: 1.6;
  white-space: pre-wrap;
}

.detail-price-text {
  color: #f56c6c;
  font-weight: 600;
  font-size: 15px;
}

.loading-box {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
}

.availability-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .homestay-filter-section {
    padding: 12px;
  }
  
  .homestay-filter-section .el-col {
    margin-bottom: 10px;
  }
  
  .view-toggle {
    justify-content: flex-start;
    margin-top: 8px;
  }
  
  .homestay-image {
    height: 140px;
  }
  
  .homestay-content {
    padding: 12px;
  }
}
</style>
