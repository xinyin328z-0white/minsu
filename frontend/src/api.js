import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Auth
export const register = (username, password) => api.post('/auth/register', { username, password, role: 'CUSTOMER' })
export const login = (username, password) => api.post('/auth/login', { username, password })
export const verify = () => api.get('/auth/verify')

// Users
export const fetchUsers = () => api.get('/users')
export const fetchUser = (id) => api.get(`/users/${id}`)
export const createUser = (user) => api.post('/users', user)
export const updateUser = (id, user) => api.put(`/users/${id}`, user)
export const updateUserProfile = (id, profile) => api.put(`/users/${id}/profile`, profile)
export const deleteUser = (id) => api.delete(`/users/${id}`)

// Homestays
export const fetchHomestays = () => api.get('/homestays')
export const createHomestay = (homestay) => api.post('/homestays', homestay)
export const updateHomestay = (id, homestay) => api.put(`/homestays/${id}`, homestay)
export const deleteHomestay = (id) => api.delete(`/homestays/${id}`)

// Routes
export const fetchRoutes = () => api.get('/routes')
export const createRoute = (route) => api.post('/routes', route)
export const updateRoute = (id, route) => api.put(`/routes/${id}`, route)
export const deleteRoute = (id) => api.delete(`/routes/${id}`)

// Activities
export const fetchActivities = () => api.get('/activities')
export const createActivity = (activity) => api.post('/activities', activity)
export const updateActivity = (id, activity) => api.put(`/activities/${id}`, activity)
export const deleteActivity = (id) => api.delete(`/activities/${id}`)

// Bookings
export const fetchBookingsByUser = (userId) => api.get(`/bookings/user/${userId}`)
export const fetchBookings = () => api.get('/bookings')
export const fetchConfirmedBookingsByHomestay = (homestayId) => api.get(`/bookings/homestay/${homestayId}/confirmed`)
export const checkRoomConflict = (homestayId, checkInDate, checkOutDate) => 
  api.post('/bookings/check-conflict', null, {
    params: { homestayId, checkInDate, checkOutDate }
  })
export const getRoomCalendar = (homestayId, startDate, endDate) => 
  api.get(`/bookings/room-calendar/${homestayId}`, {
    params: { startDate, endDate }
  })
export const createBooking = (payload) => api.post('/bookings', payload)
export const updateBooking = (id, booking) => api.put(`/bookings/${id}`, booking)
export const deleteBooking = (id) => api.delete(`/bookings/${id}`)
export const getUserBookingsForReview = (userId) => api.get(`/bookings/user/${userId}/for-review`)
export const reviewBooking = (id, status, notes) => api.post(`/bookings/${id}/review`, { status, notes })

// Messages
export const fetchMessages = () => api.get('/messages')
export const fetchMessagesByUser = (userId) => api.get(`/messages/user/${userId}`)
export const createMessage = (message) => api.post('/messages', message)
export const updateMessage = (id, message) => api.put(`/messages/${id}`, message)
export const markUserMessagesAsRead = (userId) => api.put(`/messages/user/${userId}/mark-read`)
export const markUserAdminMessagesAsRead = (userId) => api.put(`/messages/user/${userId}/mark-admin-read`)
export const fillAllCustomerNames = () => api.post('/messages/fill-customer-names')
export const deleteMessage = (id) => api.delete(`/messages/${id}`)

// AI
export const requestRecommendation = (userId) => api.post('/ai/recommendations', { userId })
export const getAiRecommendation = (params) => api.post('/homestays/ai-recommend', params)
export const generateAiBookingDraft = (params) => api.post('/homestays/ai-booking-draft', params)
export const confirmAiBookingDraft = (booking) => api.post('/bookings/confirm-ai-draft', booking)

// For You Recommendations
export const getUserPreference = (userId) => api.get(`/recommendations/preferences/${userId}`)
export const saveUserPreference = (userId, preference) => api.post(`/recommendations/preferences/${userId}`, preference)
export const fetchForYouRecommendations = (userId) => api.get(`/recommendations/for-you/${userId}`)

// Chat AI - 客服AI助手
export const suggestChatReply = (userId, context) => api.post('/chat/ai/suggest-reply', { context }, { params: { userId } })
export const queryUserBookings = (userId) => api.get(`/chat/ai/query-bookings/${userId}`)
export const queryAvailableRooms = (homestayId) => api.get(`/chat/ai/query-rooms/${homestayId}`)
export const queryBookingsByDateRange = (homestayId, startDate, endDate) => 
  api.get('/chat/ai/query-bookings-by-date', { params: { homestayId, startDate, endDate } })

// Settings
export const getBookingWindowDays = () => api.get('/settings/booking-window-days')
export const updateBookingWindowDays = (bookingWindowDays) => api.put('/settings/booking-window-days', { bookingWindowDays })
export const getAiConfig = () => api.get('/settings/ai-config')
export const updateAiConfig = (config) => api.put('/settings/ai-config', config)

// File Upload
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
export const deleteImage = (filename) => api.delete(`/upload/images/${filename}`)
export const getImageUrl = (url) => {
  if (!url) return null
  if (url.startsWith('http')) return url
  // 如果URL已经以/api开头，直接返回
  if (url.startsWith('/api')) return url
  // 否则添加/api前缀
  return `/api${url}`
}
