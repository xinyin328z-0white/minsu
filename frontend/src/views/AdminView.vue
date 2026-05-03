<template>
  <el-container class="admin-layout">
    <el-header class="admin-header">
      <div class="title">系统管理后台</div>
      <el-button type="danger" text @click="logout">退出</el-button>
    </el-header>
    <el-container>
      <el-aside class="admin-aside">
        <el-menu :default-active="activeTab" @select="activeTab = $event">
          <el-menu-item index="dashboard">数据大屏</el-menu-item>
          <el-menu-item index="users">用户管理</el-menu-item>
          <el-menu-item index="homestays">民宿管理</el-menu-item>
          <el-menu-item index="settings">预订设置</el-menu-item>
          <el-menu-item index="activities">游玩项目</el-menu-item>
          <el-menu-item index="routes">旅游路线</el-menu-item>
          <el-menu-item index="chat">
            <el-badge :value="adminUnreadCount" :hidden="adminUnreadCount === 0" class="menu-badge">
              <span>客服聊天</span>
            </el-badge>
          </el-menu-item>
          <el-menu-item index="ai-settings">AI服务配置</el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="admin-main">
        <!-- Dashboard -->
        <div v-if="activeTab === 'dashboard'" key="tab-dashboard">
          <Dashboard :users="users" :bookings="bookings" :homestays="homestays" :activities="activities" :routes="routes" />
        </div>

        <!-- Users Management -->
        <div v-if="activeTab === 'users'" key="tab-users">
          <UserManagement />
        </div>
        
        <!-- Bookings Management -->
        <div v-if="activeTab === 'bookings'" key="tab-bookings">
          <el-card class="mb-16">
            <template #header>订单管理</template>
            <el-table :data="paginatedBookings" style="margin-top: 16px" size="small">
              <el-table-column prop="id" label="订单ID" width="80" />
              <el-table-column prop="userId" label="用户ID" width="80" />
              <el-table-column prop="homestayId" label="民宿ID" width="80" />
              <el-table-column prop="checkInDate" label="入住日期" width="120" />
              <el-table-column prop="checkOutDate" label="退房日期" width="120" />
              <el-table-column prop="guestCount" label="人数" width="70" />
              <el-table-column prop="contactPhone" label="联系电话" width="120" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getBookingStatusType(row.status)">{{ getBookingStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalPrice" label="金额" width="100" />
              <el-table-column label="操作" width="250">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'PENDING'" link type="success" size="small" @click="confirmBooking(row)">确认</el-button>
                  <el-button v-if="row.status === 'CONFIRMED'" link type="success" size="small" @click="completeBooking(row)">完成</el-button>
                  <el-button link type="primary" size="small" @click="viewBookingDetail(row)">详情</el-button>
                  <el-button link type="danger" size="small" @click="cancelBooking(row)">取消</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="bookingPage"
              v-model:page-size="bookingPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="bookings.length"
              layout="total, sizes, prev, pager, next, jumper"
              style="margin-top: 16px;"
            />
          </el-card>
        </div>

        <!-- Homestays Management -->
        <div v-if="activeTab === 'homestays'" key="tab-homestays">
          <el-card class="mb-16">
            <template #header>民宿管理</template>
            <el-button type="primary" @click="openHomestayDialog">新增民宿</el-button>
            <el-table 
              :data="homestays" 
              style="margin-top: 16px" 
              size="small"
              :default-expand-all="false"
              @expand-change="onHomestayExpandChange"
            >
              <el-table-column type="expand" width="50">
                <template #default="{ row }">
                  <div class="homestay-expand-detail">
                    <h4>民宿订单（{{ getHomestayBookings(row.id).length }}个）</h4>
                    <el-empty v-if="getHomestayBookings(row.id).length === 0" description="暂无订单" />
                    <el-table 
                      v-else
                      :data="getHomestayBookings(row.id)" 
                      style="margin-bottom: 20px"
                      size="small"
                    >
                      <el-table-column prop="id" label="订单ID" width="80" />
                      <el-table-column prop="userId" label="用户ID" width="80" />
                      <el-table-column prop="checkInDate" label="入住日期" width="120" />
                      <el-table-column prop="checkOutDate" label="退房日期" width="120" />
                      <el-table-column prop="guestCount" label="人数" width="70" />
                      <el-table-column prop="totalPrice" label="金额" width="100" />
                      <el-table-column prop="status" label="状态" width="100">
                        <template #default="{ row: booking }">
                          <el-tag :type="getBookingStatusType(booking.status)">
                            {{ getBookingStatusText(booking.status) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="300">
                        <template #default="{ row: booking }">
                          <el-button v-if="booking.status === 'PENDING'" link type="success" size="small" @click="openConfirmBookingDialog(booking, row)">确认</el-button>
                          <el-button v-if="booking.status === 'CONFIRMED'" link type="success" size="small" @click="completeBooking(booking)">完成</el-button>
                          <el-button link type="primary" size="small" @click="viewBookingDetail(booking)">详情</el-button>
                          <el-button link type="danger" size="small" @click="cancelBooking(booking)">取消</el-button>
                        </template>
                      </el-table-column>
                    </el-table>

                    <h4 style="margin-top: 20px;">房间日历</h4>
                    <RoomCalendar 
                      :homestay-id="row.id" 
                      :homestay-name="row.name"
                      :default-range-days="bookingWindowDays"
                      :max-advance-days="bookingWindowDays"
                    />
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="图片" width="80">
                <template #default="{ row }">
                  <div v-if="row.imageUrl" class="table-image-preview">
                    <img :src="getFullImageUrl(row.imageUrl)" alt="民宿图片" />
                  </div>
                  <span v-else class="no-image">无图</span>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="location" label="位置" />
              <el-table-column prop="availableRooms" label="房间数" width="80" />
              <el-table-column prop="pricePerNight" label="价格/晚" width="100" />
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="editHomestay(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="deleteHomestayConfirm(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- Booking Settings -->
        <div v-if="activeTab === 'settings'" key="tab-settings">
          <BookingSettings />
        </div>

        <!-- Activities Management -->
        <div v-if="activeTab === 'activities'" key="tab-activities">
          <ActivityManagement />
        </div>

        <!-- Routes Management -->
        <div v-if="activeTab === 'routes'" key="tab-routes">
          <RouteManagement />
        </div>

        <!-- Chat Management -->
        <div v-if="activeTab === 'chat'" key="tab-chat">
          <AdminChatWindow />
        </div>

        <!-- AI Settings Management -->
        <div v-if="activeTab === 'ai-settings'" key="tab-ai-settings">
          <AiSettings />
        </div>
      </el-main>
    </el-container>

    <!-- User Dialog -->
    <el-dialog v-model="userDialog" :title="editingUser?.id ? '编辑用户' : '新增用户'">
      <el-form :model="editingUser" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editingUser.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="editingUser.password" type="password" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editingUser.email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editingUser.role">
            <el-option label="客户" value="CUSTOMER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- Homestay Dialog -->
    <el-dialog v-model="homestayDialog" :title="editingHomestay?.id ? '编辑民宿' : '新增民宿'" width="600px">
      <el-form :model="editingHomestay" label-width="100px">
        <el-form-item label="民宿图片">
          <div class="image-upload-wrapper">
            <el-upload
              class="image-uploader"
              action=""
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleImageUpload"
              accept="image/*"
            >
              <div v-if="editingHomestay.imageUrl" class="image-preview">
                <img :src="getFullImageUrl(editingHomestay.imageUrl)" alt="民宿图片" />
                <div class="image-overlay">
                  <el-icon class="upload-icon"><Refresh /></el-icon>
                  <span>更换图片</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>上传图片</span>
              </div>
            </el-upload>
            <div v-if="imageUploading" class="upload-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>上传中...</span>
            </div>
            <el-button 
              v-if="editingHomestay.imageUrl" 
              type="danger" 
              size="small" 
              @click="removeHomestayImage"
              style="margin-top: 8px;"
            >
              删除图片
            </el-button>
            <div class="upload-tip">支持 JPG、PNG、GIF 格式，最大 10MB</div>
          </div>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editingHomestay.name" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="editingHomestay.location" />
        </el-form-item>
        <el-form-item label="价格/晚">
          <el-input v-model.number="editingHomestay.pricePerNight" type="number" />
        </el-form-item>
        <el-form-item label="房间数量">
          <el-input v-model.number="editingHomestay.availableRooms" type="number" :min="1" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingHomestay.description" type="textarea" />
        </el-form-item>
        <el-form-item label="房间注意">
          <el-input v-model="editingHomestay.roomNotes" type="textarea" :rows="3" placeholder="输入房间注意事项" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="homestayDialog = false">取消</el-button>
        <el-button type="primary" @click="saveHomestay" :loading="imageUploading">保存</el-button>
      </template>
    </el-dialog>

    <!-- Activity Dialog -->
    <el-dialog v-model="activityDialog" :title="editingActivity?.id ? '编辑项目' : '新增项目'">
      <el-form :model="editingActivity" label-width="80px">
        <el-form-item label="项目名">
          <el-input v-model="editingActivity.name" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="editingActivity.location" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingActivity.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialog = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">保存</el-button>
      </template>
    </el-dialog>

    <!-- Route Dialog -->
    <el-dialog v-model="routeDialog" :title="editingRoute?.id ? '编辑路线' : '新增路线'">
      <el-form :model="editingRoute" label-width="80px">
        <el-form-item label="路线名">
          <el-input v-model="editingRoute.name" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editingRoute.tags" placeholder="用逗号分隔" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingRoute.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="routeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRoute">保存</el-button>
      </template>
    </el-dialog>

    <!-- Reply Message Dialog -->
    <el-dialog v-model="messageDialog" title="回复客户">
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="客户">
          {{ replyForm.customerName }}
        </el-form-item>
        <el-form-item label="原文">
          {{ replyForm.content }}
        </el-form-item>
        <el-form-item label="回复">
          <el-input v-model="replyForm.reply" type="textarea" rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="messageDialog = false">取消</el-button>
        <el-button type="primary" @click="saveReply">发送</el-button>
      </template>
    </el-dialog>

    <!-- Booking Confirm With Impact Dialog -->
    <el-dialog v-model="confirmBookingDialog" title="确认订单" width="95%" :lock-scroll="true">
      <div v-if="selectedBookingForConfirm && selectedHomestayForConfirm" class="confirm-dialog-content">
        <el-row :gutter="20" class="mb-20">
          <el-col :xs="24" :md="8">
            <el-card>
              <template #header>订单详情</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="订单ID">{{ selectedBookingForConfirm.id }}</el-descriptions-item>
                <el-descriptions-item label="用户ID">{{ selectedBookingForConfirm.userId }}</el-descriptions-item>
                <el-descriptions-item label="民宿">{{ selectedHomestayForConfirm.name }}</el-descriptions-item>
                <el-descriptions-item label="入住日期">{{ selectedBookingForConfirm.checkInDate }}</el-descriptions-item>
                <el-descriptions-item label="退房日期">{{ selectedBookingForConfirm.checkOutDate }}</el-descriptions-item>
                <el-descriptions-item label="入住人数">{{ selectedBookingForConfirm.guestCount }}</el-descriptions-item>
                <el-descriptions-item label="总价">￥{{ selectedBookingForConfirm.totalPrice }}</el-descriptions-item>
                <el-descriptions-item label="特殊要求">{{ selectedBookingForConfirm.specialRequests || '无' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="16">
            <BookingImpactPreview 
              v-if="selectedBookingForConfirm && selectedHomestayForConfirm && currentAvailabilityForConfirm"
              :homestay-id="selectedHomestayForConfirm.id"
              :check-in-date="selectedBookingForConfirm.checkInDate"
              :check-out-date="selectedBookingForConfirm.checkOutDate"
              :current-availability="currentAvailabilityForConfirm"
              :available-rooms="selectedHomestayForConfirm.availableRooms"
            />
          </el-col>
        </el-row>
      </div>
      <template #footer>
        <el-button @click="confirmBookingDialog = false">取消</el-button>
        <el-button type="primary" :loading="confirmingBooking" @click="confirmBookingWithImpact">确认订单</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Loading } from '@element-plus/icons-vue'
import UserManagement from '../components/UserManagement.vue'
import ActivityManagement from '../components/ActivityManagement.vue'
import RouteManagement from '../components/RouteManagement.vue'
import BookingSettings from '../components/BookingSettings.vue'
import AiSettings from '../components/AiSettings.vue'
import AdminChatWindow from '../components/AdminChatWindow.vue'
import RoomCalendar from '../components/RoomCalendar.vue'
import BookingImpactPreview from '../components/BookingImpactPreview.vue'
import Dashboard from '../components/Dashboard.vue'
import {
  fetchUsers, deleteUser, createUser, updateUser,
  fetchHomestays, deleteHomestay, createHomestay, updateHomestay,
  fetchActivities, deleteActivity, createActivity, updateActivity,
  fetchRoutes, deleteRoute, createRoute, updateRoute,
  fetchMessages, deleteMessage, createMessage,
  fetchBookings, updateBooking, getRoomCalendar,
  getBookingWindowDays, updateBookingWindowDays,
  getAiConfig, updateAiConfig,
  uploadImage, deleteImage, getImageUrl
} from '../api'

const router = useRouter()
const activeTab = ref('dashboard')
const users = ref([])
const bookings = ref([])
const bookingPage = ref(1)
const bookingPageSize = ref(10)
const homestays = ref([])
const activities = ref([])
const routes = ref([])
const messages = ref([])

const userDialog = ref(false)
const editingUser = ref({})
const homestayDialog = ref(false)
const editingHomestay = ref({})
const activityDialog = ref(false)
const editingActivity = ref({})
const routeDialog = ref(false)
const editingRoute = ref({})
const messageDialog = ref(false)
const replyForm = ref({})
const confirmBookingDialog = ref(false)
const selectedBookingForConfirm = ref(null)
const selectedHomestayForConfirm = ref(null)
const currentAvailabilityForConfirm = ref({})
const confirmingBooking = ref(false)
const bookingWindowDays = ref(60)
const settingsLoading = ref(false)
const expandedHomestays = ref(new Set())
const adminUnreadCount = ref(0)
const aiConfig = ref({
  aiApiKey: '',
  aiModel: '',
  aiBaseUrl: '',
  aiEnabled: false
})
const savingAiConfig = ref(false)
const imageUploading = ref(false)

const paginatedBookings = computed(() => {
  const start = (bookingPage.value - 1) * bookingPageSize.value
  const end = start + bookingPageSize.value
  return bookings.value.slice(start, end)
})

const getHomestayBookings = (homestayId) => {
  return bookings.value.filter(b => b.homestayId === homestayId)
}

const loadData = async () => {
  users.value = (await fetchUsers()).data
  bookings.value = (await fetchBookings()).data
  homestays.value = (await fetchHomestays()).data
  activities.value = (await fetchActivities()).data
  routes.value = (await fetchRoutes()).data
  messages.value = (await fetchMessages()).data
  await loadSettings()
  await loadAiConfig()
  await loadAdminUnreadCount()
}

const loadSettings = async () => {
  try {
    const { data } = await getBookingWindowDays()
    bookingWindowDays.value = data.bookingWindowDays || 60
  } catch (err) {
    console.error('加载预订设置失败:', err)
  }
}

const loadAiConfig = async () => {
  try {
    const { data } = await getAiConfig()
    aiConfig.value = {
      aiApiKey: data.aiApiKey || '',
      aiModel: data.aiModel || '',
      aiBaseUrl: data.aiBaseUrl || '',
      aiEnabled: data.aiEnabled || false
    }
  } catch (err) {
    console.error('加载AI配置失败:', err)
  }
}

const saveAiConfig = async () => {
  savingAiConfig.value = true
  try {
    const { data } = await updateAiConfig(aiConfig.value)
    aiConfig.value = {
      aiApiKey: data.aiApiKey || '',
      aiModel: data.aiModel || '',
      aiBaseUrl: data.aiBaseUrl || '',
      aiEnabled: data.aiEnabled || false
    }
    ElMessage.success('AI配置已保存')
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    savingAiConfig.value = false
  }
}

const loadAdminUnreadCount = async () => {
  try {
    const res = await fetchMessages()
    adminUnreadCount.value = (res.data || []).filter(m => m.type === 'CUSTOMER_INQUIRY' && !m.isRead).length
  } catch (err) {
    console.error('加载未读消息数失败:', err)
  }
}

const saveSettings = async () => {
  settingsLoading.value = true
  try {
    const { data } = await updateBookingWindowDays(bookingWindowDays.value)
    bookingWindowDays.value = data.bookingWindowDays || bookingWindowDays.value
    ElMessage.success('设置已保存')
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    settingsLoading.value = false
  }
}

const viewUserDetail = (user) => {
  const details = `
用户ID: ${user.id}
用户名: ${user.username}
真实姓名: ${user.realName || '未填写'}
手机号: ${user.phone || '未填写'}
邮箱: ${user.email || '未填写'}
身份证: ${user.idCard || '未填写'}
地址: ${user.address || '未填写'}
角色: ${user.role}
  `.trim()
  ElMessageBox.alert(details, '用户详细信息', { confirmButtonText: '关闭' })
}

const viewBookingDetail = (booking) => {
  const details = `
订单ID: ${booking.id}
用户ID: ${booking.userId}
民宿ID: ${booking.homestayId}
入住日期: ${booking.checkInDate}
退房日期: ${booking.checkOutDate}
入住人数: ${booking.guestCount || '未填写'}
联系电话: ${booking.contactPhone || '未填写'}
特殊要求: ${booking.specialRequests || '无'}
总价: ${booking.totalPrice || '待定'}
状态: ${getBookingStatusText(booking.status)}
创建时间: ${booking.createdAt || ''}
  `.trim()
  ElMessageBox.alert(details, '订单详细信息', { confirmButtonText: '关闭' })
}

const confirmBooking = async (booking) => {
  try {
    await ElMessageBox.confirm('确认此订单？', '提示', { type: 'warning' })
    await updateBooking(booking.id, { ...booking, status: 'CONFIRMED' })
    await loadData()
    ElMessage.success('订单已确认')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const completeBooking = async (booking) => {
  try {
    await ElMessageBox.confirm('标记为已完成？', '提示', { type: 'warning' })
    await updateBooking(booking.id, { ...booking, status: 'COMPLETED' })
    await loadData()
    ElMessage.success('订单已完成')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const cancelBooking = async (booking) => {
  try {
    await ElMessageBox.confirm('取消此订单？', '提示', { type: 'warning' })
    await updateBooking(booking.id, { ...booking, status: 'CANCELED' })
    await loadData()
    ElMessage.success('订单已取消')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const getBookingStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'CONFIRMED': 'success',
    'CANCELED': 'info',
    'COMPLETED': 'success'
  }
  return types[status] || 'info'
}

const getBookingStatusText = (status) => {
  const texts = {
    'PENDING': '待确认',
    'CONFIRMED': '已确认',
    'CANCELED': '已取消',
    'COMPLETED': '已完成'
  }
  return texts[status] || status
}

const openUserDialog = () => {
  editingUser.value = {}
  userDialog.value = true
}

const editUser = (user) => {
  editingUser.value = { ...user }
  userDialog.value = true
}

const saveUser = async () => {
  if (editingUser.value.id) {
    await updateUser(editingUser.value.id, editingUser.value)
  } else {
    await createUser(editingUser.value)
  }
  userDialog.value = false
  await loadData()
  ElMessage.success('保存成功')
}

const deleteUserConfirm = (id) => {
  ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' }).then(async () => {
    await deleteUser(id)
    await loadData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const openHomestayDialog = () => {
  editingHomestay.value = {}
  homestayDialog.value = true
}

const editHomestay = (homestay) => {
  editingHomestay.value = { ...homestay }
  homestayDialog.value = true
}

const saveHomestay = async () => {
  if (editingHomestay.value.id) {
    await updateHomestay(editingHomestay.value.id, editingHomestay.value)
  } else {
    await createHomestay(editingHomestay.value)
  }
  homestayDialog.value = false
  await loadData()
  ElMessage.success('保存成功')
}

const deleteHomestayConfirm = (id) => {
  ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' }).then(async () => {
    await deleteHomestay(id)
    await loadData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 图片上传相关方法
const getFullImageUrl = (url) => {
  return getImageUrl(url)
}

const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  return true
}

const handleImageUpload = async (options) => {
  imageUploading.value = true
  try {
    const { data } = await uploadImage(options.file)
    editingHomestay.value.imageUrl = data.url
    ElMessage.success('图片上传成功')
  } catch (err) {
    ElMessage.error('图片上传失败: ' + (err.response?.data?.error || err.message))
  } finally {
    imageUploading.value = false
  }
}

const removeHomestayImage = async () => {
  if (!editingHomestay.value.imageUrl) return
  
  try {
    // 从 URL 中提取文件名
    const url = editingHomestay.value.imageUrl
    const filename = url.split('/').pop()
    await deleteImage(filename)
    editingHomestay.value.imageUrl = null
    ElMessage.success('图片已删除')
  } catch (err) {
    // 即使删除失败也清除 URL
    editingHomestay.value.imageUrl = null
    console.error('删除图片失败:', err)
  }
}

const openActivityDialog = () => {
  editingActivity.value = {}
  activityDialog.value = true
}

const editActivity = (activity) => {
  editingActivity.value = { ...activity }
  activityDialog.value = true
}

const saveActivity = async () => {
  if (editingActivity.value.id) {
    await updateActivity(editingActivity.value.id, editingActivity.value)
  } else {
    await createActivity(editingActivity.value)
  }
  activityDialog.value = false
  await loadData()
  ElMessage.success('保存成功')
}

const deleteActivityConfirm = (id) => {
  ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' }).then(async () => {
    await deleteActivity(id)
    await loadData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const openRouteDialog = () => {
  editingRoute.value = {}
  routeDialog.value = true
}

const editRoute = (route) => {
  editingRoute.value = { ...route }
  routeDialog.value = true
}

const saveRoute = async () => {
  if (editingRoute.value.id) {
    await updateRoute(editingRoute.value.id, editingRoute.value)
  } else {
    await createRoute(editingRoute.value)
  }
  routeDialog.value = false
  await loadData()
  ElMessage.success('保存成功')
}

const deleteRouteConfirm = (id) => {
  ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' }).then(async () => {
    await deleteRoute(id)
    await loadData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const replyMessage = (msg) => {
  replyForm.value = { ...msg, reply: '' }
  messageDialog.value = true
}

const saveReply = async () => {
  await createMessage({
    userId: replyForm.value.userId,
    customerName: replyForm.value.customerName,
    content: replyForm.value.reply,
    type: 'ADMIN_REPLY'
  })
  messageDialog.value = false
  await loadData()
  ElMessage.success('回复成功')
}

const deleteMessageConfirm = (id) => {
  ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' }).then(async () => {
    await deleteMessage(id)
    await loadData()
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}

const onHomestayExpandChange = async (row, expandedRows) => {
  if (expandedRows.includes(row)) {
    expandedHomestays.value.add(row.id)
  } else {
    expandedHomestays.value.delete(row.id)
  }
}

const openConfirmBookingDialog = async (booking, homestay) => {
  selectedBookingForConfirm.value = booking
  selectedHomestayForConfirm.value = homestay
  
  // 加载房间可用性数据
  try {
    const { data } = await getRoomCalendar(homestay.id, booking.checkInDate, booking.checkOutDate)
    currentAvailabilityForConfirm.value = data.dailyAvailability || {}
  } catch (err) {
    console.error('加载房间日历失败:', err)
    currentAvailabilityForConfirm.value = {}
  }
  
  confirmBookingDialog.value = true
}

const confirmBookingWithImpact = async () => {
  if (!selectedBookingForConfirm.value) return
  
  try {
    confirmingBooking.value = true
    await updateBooking(selectedBookingForConfirm.value.id, {
      ...selectedBookingForConfirm.value,
      status: 'CONFIRMED'
    })
    confirmBookingDialog.value = false
    await loadData()
    ElMessage.success('订单已确认')
  } catch (err) {
    ElMessage.error('操作失败: ' + (err.response?.data?.message || err.message))
  } finally {
    confirmingBooking.value = false
  }
}

onMounted(async () => {
  const role = localStorage.getItem('role')
  if (role !== 'ADMIN') {
    router.push('/customer')
    return
  }
  await loadData()
  setInterval(loadAdminUnreadCount, 30000)
})

watch(activeTab, (tab) => {
  if (tab === 'chat') {
    loadAdminUnreadCount()
  }
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.admin-aside {
  width: 200px;
  border-right: 1px solid #ddd;
}

.menu-badge :deep(.el-badge__content) {
  transform: translate(8px, -6px);
}

.admin-main {
  padding: 20px;
  max-height: calc(100vh - 60px);
  overflow-y: auto;
}

.mb-16 {
  margin-bottom: 16px;
}

.mb-20 {
  margin-bottom: 20px;
}

.homestay-expand-detail {
  padding: 20px 0;
  background: #f5f7fa;
}

.homestay-expand-detail h4 {
  margin: 20px 0 15px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.homestay-expand-detail h4:first-child {
  margin-top: 0;
}

.confirm-dialog-content {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.mb-20 {
  margin-bottom: 20px;
}

/* 图片上传样式 */
.image-upload-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.image-uploader {
  cursor: pointer;
}

.image-preview {
  width: 200px;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #dcdfe6;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-preview .image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-preview:hover .image-overlay {
  opacity: 1;
}

.upload-placeholder {
  width: 200px;
  height: 150px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  transition: border-color 0.3s, color 0.3s;
}

.upload-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.upload-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: #409eff;
  font-size: 14px;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

/* 表格图片预览 */
.table-image-preview {
  width: 50px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
}

.table-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
