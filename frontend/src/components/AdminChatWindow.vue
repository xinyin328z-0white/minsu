<template>
  <el-container class="admin-chat-layout">
    <!-- Customer List -->
    <el-aside class="customer-list">
      <div class="list-header">
        <span>客户列表</span>
        <div>
          <el-button size="small" @click="openInitiateChatDialog">+ 发起</el-button>
          <el-button size="small" @click="loadCustomers">刷新</el-button>
        </div>
      </div>
      <el-input 
        v-model="searchQuery" 
        placeholder="搜索客户..." 
        size="small"
        style="margin-bottom: 8px;"
      />
      <div class="customers">
        <div 
          v-for="customer in filteredCustomers" 
          :key="customer.userId"
          :class="['customer-item', { active: selectedUserId === customer.userId }]"
          @click="selectCustomer(customer)"
        >
          <div class="customer-name">{{ customer.customerName }}</div>
          <div class="customer-id">ID: {{ customer.userId }}</div>
          <div v-if="customer.unreadCount > 0" class="unread-badge">
            {{ customer.unreadCount }}
          </div>
        </div>
        <div v-if="customers.length === 0" class="empty-customers">
          暂无客户
        </div>
      </div>
    </el-aside>

    <!-- Chat Window -->
    <el-main class="chat-main">
      <div v-if="selectedUserId" class="chat-content">
        <div class="chat-header">
          <span>{{ selectedCustomerName }}</span>
          <el-button size="small" @click="refreshMessages">刷新</el-button>
        </div>
        
        <div class="messages-container" ref="messageContainer">
          <div v-for="msg in messages" :key="msg.id" :class="['message', msg.type]">
            <div class="message-meta">
              <span class="sender">{{ msg.type === 'CUSTOMER_INQUIRY' ? '客户' : '我' }}</span>
              <span class="time">{{ formatTime(msg.createdAt) }}</span>
            </div>
            <div class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
          </div>
          <div v-if="messages.length === 0" class="empty-messages">
            暂无消息
          </div>
        </div>
        
        <!-- AI建议面板 -->
        <div v-if="showAiSuggestion" class="ai-suggestion-panel">
          <div class="suggestion-header">
            <span>💡 AI建议回复</span>
            <el-button text size="small" @click="showAiSuggestion = false">关闭</el-button>
          </div>
          <div class="suggestion-content">
            <div v-if="aiSuggestLoading" class="loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              AI正在思考中...
            </div>
            <div v-else>{{ aiSuggestedReply }}</div>
          </div>
          <div class="suggestion-actions">
            <el-button size="small" @click="useAiSuggestion">使用建议</el-button>
            <el-button size="small" @click="regenerateAiSuggestion" :loading="aiSuggestLoading">重新生成</el-button>
            <el-button text size="small" @click="copyAiSuggestion">复制</el-button>
          </div>
        </div>
        
        <div class="message-input">
          <!-- 查询工具面板 - 常驻显示 -->
          <div class="query-tools-panel">
            <div class="tools-header">快速查询工具</div>
            <el-button size="small" @click="queryCustomerBookings" :loading="queryLoading">
              📋 查看订单
            </el-button>
            <el-button size="small" @click="showRoomQueryDialog = true" :loading="queryLoading">
              🏠 查询房间
            </el-button>
            <el-button size="small" @click="loadUserBookingsForReview" :loading="reviewLoading" type="warning">
              📝 订单审核
            </el-button>
          </div>
          
          <!-- AI智能生成面板 -->
          <div class="ai-generate-panel">
            <div class="tools-header">✨ AI智能回复</div>
            <el-input
              v-model="aiGuidance"
              type="textarea"
              :rows="2"
              placeholder="指导AI回复方向（可选），例如：婉拒客户请求并推荐其他日期、热情推荐海景房、告知优惠活动详情、表示歉意并提供补偿方案..."
              class="ai-guidance-input"
            />
            <el-button 
              size="small" 
              type="primary"
              @click="getAiSuggestionWithContext" 
              :loading="aiSuggestLoading"
            >
              生成AI回复
            </el-button>
            <el-button 
              v-if="aiGuidance"
              size="small" 
              text
              @click="aiGuidance = ''"
            >
              清空指导
            </el-button>
          </div>
          
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            placeholder="输入回复..."
            @keyup.ctrl.enter="sendMessage"
          />
          <el-button type="primary" @click="sendMessage" :loading="sending">
            回复 (Ctrl+Enter)
          </el-button>
        </div>
      </div>
      <div v-else class="no-selection">
        请从左侧选择客户开始聊天
      </div>
    </el-main>

    <!-- Initiate Chat Dialog -->
    <el-dialog v-model="initiateChatDialog" title="发起客户对话">
      <el-form :model="initiateForm" label-width="100px">
        <el-form-item label="选择用户">
          <el-select 
            v-model="initiateForm.userId" 
            filterable
            placeholder="请选择用户"
            @change="onUserSelected"
          >
            <el-option 
              v-for="user in allUsers" 
              :key="user.id"
              :label="`${user.realName || user.username} (ID: ${user.id})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称" v-if="initiateForm.userId">
          <el-input v-model="initiateForm.customerName" disabled />
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input 
            v-model="initiateForm.content" 
            type="textarea"
            :rows="4"
            placeholder="输入要发送给客户的消息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="initiateChatDialog = false">取消</el-button>
        <el-button type="primary" :loading="initiatingChat" @click="initiateChat">发送</el-button>
      </template>
    </el-dialog>

    <!-- Room Query Dialog -->
    <el-dialog v-model="showRoomQueryDialog" title="查询房间信息" width="600px">
      <el-form :model="roomQueryForm" label-width="100px">
        <el-form-item label="选择民宿">
          <el-select 
            v-model="roomQueryForm.homestayId" 
            filterable
            placeholder="请选择民宿"
          >
            <el-option 
              v-for="homestay in homestays" 
              :key="homestay.id"
              :label="`${homestay.name} (${homestay.location})`"
              :value="homestay.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="查询类型">
          <el-radio-group v-model="roomQueryForm.queryType">
            <el-radio value="available">查询可用房间</el-radio>
            <el-radio value="dateRange">查询日期范围预订</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="roomQueryForm.queryType === 'dateRange'">
          <el-form-item label="开始日期">
            <el-date-picker
              v-model="roomQueryForm.startDate"
              type="date"
              placeholder="选择开始日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker
              v-model="roomQueryForm.endDate"
              type="date"
              placeholder="选择结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="showRoomQueryDialog = false">取消</el-button>
        <el-button type="primary" :loading="queryLoading" @click="executeRoomQuery">查询</el-button>
      </template>
    </el-dialog>

    <!-- Booking Review Dialog - 使用复用组件 -->
    <BookingReviewDialog
      v-model="showBookingReviewDialog"
      :bookings="userBookingsForReview"
      :customer-name="selectedCustomerName"
      :current-availability="currentAvailability"
      :available-rooms="selectedHomestay.availableRooms || 1"
      @booking-approved="handleBookingApproved"
      @booking-rejected="handleBookingRejected"
      @refresh="loadMessages"
    />
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { fetchMessages, fetchMessagesByUser, createMessage, markUserMessagesAsRead, fillAllCustomerNames, fetchUsers, suggestChatReply, queryUserBookings as apiQueryUserBookings, queryAvailableRooms as apiQueryAvailableRooms, queryBookingsByDateRange as apiQueryBookingsByDateRange, fetchHomestays, getUserBookingsForReview, getRoomCalendar } from '../api'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import BookingReviewDialog from './BookingReviewDialog.vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

// 渲染 markdown 并净化 HTML
const renderMarkdown = (content) => {
  if (!content) return ''
  const html = marked.parse(content)
  return DOMPurify.sanitize(html)
}

const messages = ref([])
const newMessage = ref('')
const sending = ref(false)
const messageContainer = ref(null)
const customers = ref([])
const selectedUserId = ref(null)
const selectedCustomerName = ref('')
const searchQuery = ref('')
const initiateChatDialog = ref(false)
const initiatingChat = ref(false)
const initiateForm = ref({
  userId: null,
  customerName: '',
  content: ''
})
const allUsers = ref([])

// AI建议相关
const showAiSuggestion = ref(false)
const aiSuggestedReply = ref('')
const aiSuggestLoading = ref(false)
const queryLoading = ref(false)
const showRoomQueryDialog = ref(false)
const aiGuidance = ref('')  // AI生成指导输入

// 房间查询表单
const roomQueryForm = ref({
  homestayId: null,
  queryType: 'available',
  startDate: '',
  endDate: ''
})
const homestays = ref([])

// 订单审核相关
const showBookingReviewDialog = ref(false)
const userBookingsForReview = ref([])
const currentAvailability = ref({})
const selectedHomestay = ref({})
const reviewLoading = ref(false)

const filteredCustomers = computed(() => {
  return customers.value.filter(customer =>
    customer.customerName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    customer.userId.toString().includes(searchQuery.value)
  )
})

const loadCustomers = async () => {
  try {
    const res = await fetchMessages()
    console.log('获取到的消息数据:', res.data)
    
    // 从所有消息中提取唯一的客户
    const customerMap = new Map()
    
    if (res.data && Array.isArray(res.data)) {
      res.data.forEach(msg => {
        // 只处理有 userId 的消息
        if (msg.userId) {
          if (!customerMap.has(msg.userId)) {
            // 统计未读消息数
            const unreadCount = res.data.filter(m => 
              m.userId === msg.userId && 
              m.type === 'CUSTOMER_INQUIRY' && 
              !m.isRead
            ).length
            
            // 使用后端返回的 customerName（已自动填充）
            const displayName = msg.customerName || `客户${msg.userId}`
            
            customerMap.set(msg.userId, {
              userId: msg.userId,
              customerName: displayName,
              unreadCount: unreadCount
            })
          }
        }
      })
    }
    
    customers.value = Array.from(customerMap.values())
      .sort((a, b) => b.unreadCount - a.unreadCount)
    
    console.log('处理后的客户列表:', customers.value)
  } catch (err) {
    console.error('加载客户列表失败:', err)
    ElMessage.error('加载客户列表失败: ' + (err.response?.data || err.message))
  }
}

const selectCustomer = async (customer) => {
  selectedUserId.value = customer.userId
  selectedCustomerName.value = customer.customerName
  
  // 标记该客户的所有未读消息为已读
  try {
    await markUserMessagesAsRead(customer.userId)
  } catch (err) {
    console.error('标记消息为已读失败:', err)
  }
  
  await loadMessages()
  await loadCustomers() // 重新加载客户列表以更新未读标记
}

const loadMessages = async () => {
  if (!selectedUserId.value) return
  try {
    const res = await fetchMessagesByUser(selectedUserId.value)
    messages.value = res.data.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    await nextTick()
    scrollToBottom()
  } catch (err) {
    console.error('加载消息失败:', err)
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  if (!selectedUserId.value) {
    ElMessage.warning('请先选择客户')
    return
  }

  sending.value = true
  try {
    await createMessage({
      userId: selectedUserId.value,
      customerName: selectedCustomerName.value,
      content: newMessage.value,
      type: 'ADMIN_REPLY',
      isRead: false
    })
    newMessage.value = ''
    await loadMessages()
    await loadCustomers()
    ElMessage.success('回复成功')
  } catch (err) {
    ElMessage.error('发送失败: ' + (err.response?.data || err.message))
  } finally {
    sending.value = false
  }
}

const refreshMessages = () => {
  loadMessages()
}

const formatTime = (datetime) => {
  if (!datetime) return ''
  return new Date(datetime).toLocaleString('zh-CN')
}

const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

// AI建议处理函数
const getAiSuggestion = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择客户')
    return
  }

  aiSuggestLoading.value = true
  try {
    // 构建对话上下文
    const recentMessages = messages.value.slice(-5) // 取最近5条消息
    const context = recentMessages
      .map(msg => `${msg.type === 'CUSTOMER_INQUIRY' ? '客户' : '我'}: ${msg.content}`)
      .join('\n')
    
    const response = await suggestChatReply(selectedUserId.value, context)
    aiSuggestedReply.value = response.data.suggestedReply || '无法生成建议'
    showAiSuggestion.value = true
    ElMessage.success('AI建议已生成')
  } catch (err) {
    ElMessage.error('获取AI建议失败: ' + (err.response?.data?.message || err.message))
  } finally {
    aiSuggestLoading.value = false
  }
}

// AI智能回复 - 包含查询上下文和客服指导
const getAiSuggestionWithContext = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择客户')
    return
  }

  aiSuggestLoading.value = true
  try {
    // 构建对话上下文
    const recentMessages = messages.value.slice(-5) // 取最近5条消息
    let context = recentMessages
      .map(msg => `${msg.type === 'CUSTOMER_INQUIRY' ? '客户' : '我'}: ${msg.content}`)
      .join('\n')
    
    // 如果输入框有内容，也加入上下文（包括查询结果）
    if (newMessage.value.trim()) {
      context += '\n\n当前已查询到的信息:\n' + newMessage.value
    }
    
    // 如果有客服指导，加入上下文
    if (aiGuidance.value.trim()) {
      context += '\n\n【客服指导要求】：' + aiGuidance.value.trim()
    }
    
    const response = await suggestChatReply(selectedUserId.value, context)
    const suggestion = response.data.suggestedReply || '无法生成建议'
    
    // 将AI建议追加到输入框
    newMessage.value = suggestion
    
    ElMessage.success('AI建议已生成并插入')
  } catch (err) {
    ElMessage.error('获取AI建议失败: ' + (err.response?.data?.message || err.message))
  } finally {
    aiSuggestLoading.value = false
  }
}

const regenerateAiSuggestion = () => {
  getAiSuggestion()
}

const useAiSuggestion = () => {
  newMessage.value = aiSuggestedReply.value
  showAiSuggestion.value = false
}

const copyAiSuggestion = () => {
  navigator.clipboard.writeText(aiSuggestedReply.value)
  ElMessage.success('已复制')
}

const queryCustomerBookings = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择客户')
    return
  }

  queryLoading.value = true
  try {
    const response = await apiQueryUserBookings(selectedUserId.value)
    
    // 处理新格式的响应
    let bookingData = []
    let bookingText = '📋 客户订单信息:\n'
    
    if (response.data.success) {
      if (response.data.data) {
        try {
          bookingData = JSON.parse(response.data.data)
        } catch (e) {
          console.warn('数据解析失败，使用原始数据')
          bookingData = response.data.data
        }
      }
      
      if (Array.isArray(bookingData) && bookingData.length > 0) {
        bookingData.forEach((booking, index) => {
          bookingText += `${index + 1}. 订单#${booking.orderId || booking.id}: ${booking.homestayName || '民宿'} (${booking.checkIn} - ${booking.checkOut}, ${booking.status}, ¥${booking.totalPrice})\n`
        })
      } else if (response.data.message) {
        bookingText = response.data.message
      } else {
        bookingText += '暂无订单'
      }
    } else {
      bookingText = response.data.message || '查询失败'
    }
    
    // 将订单信息追加到输入框
    newMessage.value = (newMessage.value ? newMessage.value + '\n\n' : '') + bookingText
    ElMessage.success('订单信息已插入')
  } catch (err) {
    console.error('查询订单失败:', err)
    ElMessage.error('查询失败: ' + (err.response?.data?.message || err.message || '未知错误'))
  } finally {
    queryLoading.value = false
  }
}

// 执行房间查询
const executeRoomQuery = async () => {
  if (!roomQueryForm.value.homestayId) {
    ElMessage.warning('请选择民宿')
    return
  }

  queryLoading.value = true
  try {
    let response
    let resultText = ''

    if (roomQueryForm.value.queryType === 'available') {
      // 查询可用房间
      response = await apiQueryAvailableRooms(roomQueryForm.value.homestayId)
      
      let roomData = {}
      if (response.data.success && response.data.data) {
        try {
          roomData = JSON.parse(response.data.data)
        } catch (e) {
          console.warn('房间数据解析失败')
          roomData = response.data.data
        }
      }
      
      resultText = `🏠 民宿房间信息:\n`
      resultText += `民宿: ${roomData.name || '未知'} (${roomData.location || ''})\n`
      resultText += `可用房间数: ${roomData.availableRooms || 0}\n`
      resultText += `价格: ¥${roomData.pricePerNight || 0}/晚\n`
      if (roomData.description) {
        resultText += `简介: ${roomData.description}\n`
      }
      if (roomData.roomNotes) {
        resultText += `房间说明: ${roomData.roomNotes}\n`
      }
    } else {
      // 查询日期范围预订
      if (!roomQueryForm.value.startDate || !roomQueryForm.value.endDate) {
        ElMessage.warning('请选择开始和结束日期')
        queryLoading.value = false
        return
      }

      response = await apiQueryBookingsByDateRange(
        roomQueryForm.value.homestayId,
        roomQueryForm.value.startDate,
        roomQueryForm.value.endDate
      )
      
      let bookingData = {}
      if (response.data.success && response.data.data) {
        try {
          bookingData = JSON.parse(response.data.data)
        } catch (e) {
          console.warn('日期范围数据解析失败')
          bookingData = response.data.data
        }
      }

      const homestay = homestays.value.find(h => h.id === roomQueryForm.value.homestayId)
      resultText = `📅 预订情况查询:\n`
      resultText += `民宿: ${homestay?.name || '未知'}\n`
      resultText += `日期范围: ${bookingData.dateRange || roomQueryForm.value.startDate + ' 至 ' + roomQueryForm.value.endDate}\n`
      resultText += `预订数: ${bookingData.totalBookings || 0}笔\n`
      if (bookingData.bookings && bookingData.bookings.length > 0) {
        resultText += `详情:\n`
        bookingData.bookings.forEach((booking, index) => {
          resultText += `  ${index + 1}. 订单#${booking.orderId} (${booking.checkIn} - ${booking.checkOut}, ${booking.status})\n`
        })
      } else {
        resultText += `该时间段暂无预订\n`
      }
    }

    // 将查询结果追加到输入框
    newMessage.value = (newMessage.value ? newMessage.value + '\n\n' : '') + resultText
    ElMessage.success('查询信息已插入')
    showRoomQueryDialog.value = false
    
    // 重置表单
    roomQueryForm.value = {
      homestayId: null,
      queryType: 'available',
      startDate: '',
      endDate: ''
    }
  } catch (err) {
    console.error('查询失败:', err)
    ElMessage.error('查询失败: ' + (err.response?.data?.message || err.message || '未知错误'))
  } finally {
    queryLoading.value = false
  }
}

const openInitiateChatDialog = async () => {
  // 加载所有用户信息，供选择
  try {
    const res = await fetchUsers()
    allUsers.value = res.data || []
  } catch (err) {
    console.error('加载用户列表失败:', err)
  }
  initiateForm.value = {
    userId: null,
    customerName: '',
    content: ''
  }
  initiateChatDialog.value = true
}

const onUserSelected = (userId) => {
  // 当选择用户时，自动填充客户名称
  const selectedUser = allUsers.value.find(u => u.id === userId)
  if (selectedUser) {
    initiateForm.value.customerName = selectedUser.realName || selectedUser.username
  }
}

const initiateChat = async () => {
  if (!initiateForm.value.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  
  if (!initiateForm.value.customerName.trim()) {
    ElMessage.warning('客户名称不能为空')
    return
  }
  
  if (!initiateForm.value.content.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  initiatingChat.value = true
  try {
    // 第一步：发送客服消息
    await createMessage({
      userId: initiateForm.value.userId,
      customerName: initiateForm.value.customerName,
      content: initiateForm.value.content,
      type: 'ADMIN_REPLY',
      isRead: false
    })
    
    ElMessage.success('已向用户发起对话')
    initiateChatDialog.value = false
    
    // 刷新客户列表和消息
    await loadCustomers()
  } catch (err) {
    ElMessage.error('发起对话失败: ' + (err.response?.data?.message || err.message))
  } finally {
    initiatingChat.value = false
  }
}

// 订单审核功能
const loadUserBookingsForReview = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择客户')
    return
  }
  
  reviewLoading.value = true
  try {
    const response = await getUserBookingsForReview(selectedUserId.value)
    userBookingsForReview.value = response.data || []
    
    if (userBookingsForReview.value.length === 0) {
      ElMessage.info('该用户暂无待审核订单')
    } else {
      // 为了显示房间影响，我们获取第一个订单的房间可用性信息
      if (userBookingsForReview.value.length > 0) {
        const firstBooking = userBookingsForReview.value[0]
        try {
          const { data } = await getRoomCalendar(
            firstBooking.homestayId,
            firstBooking.checkInDate,
            firstBooking.checkOutDate
          )
          currentAvailability.value = data.dailyAvailability || {}
          selectedHomestay.value = data.homestay || {}
        } catch (err) {
          console.error('加载房间日历失败:', err)
          currentAvailability.value = {}
          selectedHomestay.value = {}
        }
      }
      showBookingReviewDialog.value = true
    }
  } catch (err) {
    console.error('加载订单失败:', err)
    ElMessage.error('加载订单失败: ' + (err.response?.data?.message || err.message || '未知错误'))
  } finally {
    reviewLoading.value = false
  }
}

// 订单批准事件处理
const handleBookingApproved = (booking) => {
  ElMessage.success(`订单 ${booking.id} 已批准`)
  // 更新列表
  userBookingsForReview.value = userBookingsForReview.value.filter(b => b.id !== booking.id)
}

// 订单拒绝事件处理
const handleBookingRejected = (booking) => {
  ElMessage.success(`订单 ${booking.id} 已拒绝`)
  // 更新列表
  userBookingsForReview.value = userBookingsForReview.value.filter(b => b.id !== booking.id)
}

onMounted(async () => {
  // 第一次加载为所有消息填充客户名（修复历史数据）
  try {
    await fillAllCustomerNames()
    console.log('已填充所有消息的客户名')
  } catch (err) {
    console.error('填充客户名失败:', err)
  }
  
  // 加载民宿列表（用于房间查询）
  try {
    const res = await fetchHomestays()
    homestays.value = res.data || []
  } catch (err) {
    console.error('加载民宿列表失败:', err)
  }
  
  await loadCustomers()
  // Auto refresh every 30 seconds
  setInterval(() => {
    loadCustomers()
    if (selectedUserId.value) {
      loadMessages()
    }
  }, 30000)
})
</script>

<style scoped>
.admin-chat-layout {
  height: 700px;
  background: white;
}

.customer-list {
  width: 200px;
  border-right: 1px solid #dcdfe6;
  background: #f5f7fa;
  padding: 12px;
  overflow-y: auto;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
}

.list-header > div {
  display: flex;
  gap: 4px;
}

.customers {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.customer-item {
  padding: 8px 12px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.customer-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.customer-item.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.customer-name {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 2px;
  word-break: break-all;
}

.customer-id {
  font-size: 12px;
  opacity: 0.7;
}

.unread-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #f56c6c;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.empty-customers {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 13px;
}

.chat-main {
  padding: 16px;
  overflow-y: auto;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 16px;
  max-height: 450px;
}

.message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  max-width: 70%;
}

.message.CUSTOMER_INQUIRY {
  background-color: white;
  border: 1px solid #dcdfe6;
  margin-right: auto;
  text-align: left;
}

.message.ADMIN_REPLY {
  background-color: #409eff;
  color: white;
  margin-left: auto;
  text-align: left;
}

.message-meta {
  font-size: 12px;
  margin-bottom: 4px;
  opacity: 0.8;
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

/* Markdown 样式 */
.message-content.markdown-body :deep(p) {
  margin: 0 0 8px 0;
}

.message-content.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content.markdown-body :deep(ul),
.message-content.markdown-body :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}

.message-content.markdown-body :deep(li) {
  margin: 2px 0;
}

.message-content.markdown-body :deep(code) {
  background-color: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message-content.markdown-body :deep(pre) {
  background-color: rgba(0, 0, 0, 0.15);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-content.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.message-content.markdown-body :deep(blockquote) {
  border-left: 3px solid rgba(255, 255, 255, 0.5);
  margin: 8px 0;
  padding-left: 12px;
  opacity: 0.9;
}

.message-content.markdown-body :deep(a) {
  color: inherit;
  text-decoration: underline;
}

.message-content.markdown-body :deep(strong) {
  font-weight: 600;
}

.message-content.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.message-content.markdown-body :deep(th),
.message-content.markdown-body :deep(td) {
  border: 1px solid rgba(0, 0, 0, 0.2);
  padding: 6px 10px;
  text-align: left;
}

.message.CUSTOMER_INQUIRY .message-content.markdown-body :deep(code) {
  background-color: #f0f0f0;
}

.message.CUSTOMER_INQUIRY .message-content.markdown-body :deep(pre) {
  background-color: #f5f5f5;
}

.message.CUSTOMER_INQUIRY .message-content.markdown-body :deep(blockquote) {
  border-left-color: #409eff;
}

.empty-messages {
  text-align: center;
  color: #909399;
  padding: 40px;
}

/* AI建议面板样式 */
.ai-suggestion-panel {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}

.suggestion-content {
  background: rgba(255, 255, 255, 0.15);
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 8px;
  min-height: 40px;
  max-height: 150px;
  overflow-y: auto;
  line-height: 1.6;
}

.suggestion-content .loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.suggestion-content .is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.suggestion-actions {
  display: flex;
  gap: 8px;
}

/* 输入工具栏 */
.query-tools-panel {
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* AI智能生成面板 */
.ai-generate-panel {
  background: linear-gradient(135deg, #e8f4fd 0%, #f0e6ff 100%);
  border: 1px solid #c9e0f5;
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.ai-generate-panel .tools-header {
  width: 100%;
  margin-bottom: 4px;
  color: #409eff;
}

.ai-guidance-input {
  flex: 1;
  min-width: 200px;
}

.ai-guidance-input :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.tools-header {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  white-space: nowrap;
  margin-right: 8px;
}

.message-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.no-selection {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
}
</style>
