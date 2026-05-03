<template>
  <div class="ai-recommendation">
    <!-- 推荐设置面板 -->
    <el-card class="mb-20">
      <template #header>AI旅行规划助手</template>
      
      <el-form :model="recommendForm" label-width="100px" size="small">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="旅行天数">
              <el-input-number 
                v-model="recommendForm.days" 
                :min="1" 
                :max="30"
                placeholder="3"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="开始日期">
              <el-select v-model="recommendForm.startDate" placeholder="选择开始日期">
                <el-option label="今天" :value="0" />
                <el-option label="明天" :value="1" />
                <el-option label="后天" :value="2" />
                <el-option label="3天后" :value="3" />
                <el-option label="一周后" :value="7" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="预算偏好">
              <el-select v-model="recommendForm.budget" placeholder="选择预算范围">
                <el-option label="经济型" value="budget" />
                <el-option label="中等" value="moderate" />
                <el-option label="高档" value="luxury" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="特殊需求">
          <el-input 
            v-model="recommendForm.customPrompt"
            type="textarea"
            :rows="3"
            placeholder="如：喜欢户外活动、需要安静环境、请推荐有温泉的民宿等"
          />
        </el-form-item>
      </el-form>

      <div class="button-group">
        <el-button type="primary" :loading="loading" @click="getRecommendation">
          ✨ 生成旅行计划
        </el-button>
        <el-button @click="resetForm">重置</el-button>
        <span v-if="loading" class="loading-tip">
          <span class="ai-thinking-icon">🤖</span>
          AI正在思考中，为您生成最优方案...
        </span>
      </div>
    </el-card>

    <!-- 智能生成订单按钮 - 顶部 -->
    <div v-if="recommendation" class="booking-action-top mb-20">
      <el-button 
        type="success" 
        size="large"
        :loading="bookingLoading" 
        @click="generateBookingDraft"
        class="booking-button-prominent"
      >
        <el-icon v-if="!bookingLoading"><Calendar /></el-icon>
        {{ bookingLoading ? 'AI正在生成订单...' : '🎯 智能生成预订订单' }}
      </el-button>
      
      <!-- Function Calling 过程显示 -->
      <div v-if="bookingLoading" class="function-calling-process">
        <div class="process-header">
          <span class="ai-thinking-icon">🤖</span>
          <span>AI智能分析进行中...</span>
        </div>
        <div class="process-steps">
          <div 
            v-for="(step, index) in functionCallSteps" 
            :key="index"
            class="step-item"
            :class="{ 'active': step.active, 'completed': step.completed }"
          >
            <span class="step-icon">{{ step.completed ? '✅' : (step.active ? '⏳' : '⭕') }}</span>
            <span class="step-text">{{ step.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐结果 -->
    <el-card v-if="recommendation" class="mb-20">
      <template #header>
        <span>AI推荐方案</span>
        <el-button text @click="copyRecommendation">复制</el-button>
      </template>

      <el-tabs>
        <el-tab-pane label="完整推荐">
          <div class="recommendation-content" v-html="renderMarkdown(recommendation.recommendation)"></div>
        </el-tab-pane>
        
        <el-tab-pane v-if="recommendation.homestayAdvice" label="民宿建议">
          <div class="recommendation-content" v-html="renderMarkdown(recommendation.homestayAdvice)"></div>
        </el-tab-pane>
        
        <el-tab-pane v-if="recommendation.itinerary" label="行程规划">
          <div class="recommendation-content" v-html="renderMarkdown(recommendation.itinerary)"></div>
        </el-tab-pane>
      </el-tabs>

      <div class="action-buttons">
        <el-button 
          type="success" 
          size="large"
          :loading="bookingLoading" 
          @click="generateBookingDraft"
          class="booking-button-prominent"
        >
          <el-icon v-if="!bookingLoading"><Calendar /></el-icon>
          {{ bookingLoading ? 'AI正在生成订单...' : '🎯 智能生成预订订单' }}
        </el-button>
        <el-button @click="getRecommendation" :disabled="bookingLoading">
          <el-icon><Refresh /></el-icon>
          重新生成计划
        </el-button>
      </div>
    </el-card>

    <!-- 订单草稿确认对话框 -->
    <el-dialog 
      v-model="showBookingDialog" 
      title="确认预订订单" 
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="bookingDrafts && bookingDrafts.length > 0">
        <!-- 单个方案 - 直接显示确认表单 -->
        <div v-if="bookingDrafts.length === 1" class="single-booking">
          <h3>预订确认</h3>
          <BookingConfirmForm 
            :draft="bookingDrafts[0]" 
            @confirm="confirmBooking" 
            @cancel="closeBookingDialog" 
          />
        </div>
        
        <!-- 多个方案 - 显示选择列表 -->
        <div v-else class="multiple-bookings">
          <h3>请选择您心仪的民宿（{{ bookingDrafts.length }} 个方案）</h3>
          <div class="booking-options">
            <el-row :gutter="20">
              <el-col 
                v-for="(draft, index) in bookingDrafts" 
                :key="draft.optionId || index"
                :xs="24" 
                :md="12" 
                :lg="8"
              >
                <el-card 
                  class="booking-option-card" 
                  :class="{ 'selected': selectedDraftIndex === index }"
                  @click="selectDraft(index)"
                  shadow="hover"
                >
                  <div class="option-content">
                    <h4>{{ draft.homestayName }}</h4>
                    <p class="location">📍 {{ draft.homestayLocation }}</p>
                    <p class="dates">📅 {{ draft.checkInDate }} - {{ draft.checkOutDate }}</p>
                    <p class="price">💰 ¥{{ draft.totalPrice }} ({{ draft.nights }}晚)</p>
                    <p class="reason">{{ draft.recommendReason }}</p>
                    <div v-if="!draft.isAvailable" class="warning">
                      <el-alert title="房间已满" type="warning" :closable="false" />
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
          
          <div v-if="selectedDraftIndex !== null" class="selected-booking">
            <h3>预订确认</h3>
            <BookingConfirmForm 
              :draft="bookingDrafts[selectedDraftIndex]" 
              @confirm="confirmBooking" 
              @cancel="closeBookingDialog" 
            />
          </div>
        </div>
      </div>
      
      <div v-else class="no-drafts">
        <el-empty description="暂无订单草稿" />
      </div>
    </el-dialog>

    <!-- 游玩项目和路线展示 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>热门游玩项目</template>
          <el-empty v-if="activities.length === 0" description="暂无游玩项目" />
          <div v-else class="list-box">
            <div v-for="item in activities" :key="item.id || item.name" class="activity-item">
              <div class="activity-name">{{ item.name }}</div>
              <div class="activity-location">📍 {{ item.location }}</div>
              <div v-if="item.description" class="activity-desc">{{ item.description }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>推荐旅游路线</template>
          <el-empty v-if="routes.length === 0" description="暂无旅游路线" />
          <div v-else class="list-box">
            <div v-for="item in routes" :key="item.id || item.name" class="route-item">
              <div class="route-name">{{ item.name }}</div>
              <div v-if="item.tags" class="route-tags">
                <el-tag v-for="tag in item.tags.split(',')" :key="tag" size="small" class="mr-5">
                  {{ tag.trim() }}
                </el-tag>
              </div>
              <div v-if="item.description" class="route-desc">{{ item.description }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Refresh, Loading, Calendar } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { fetchActivities, fetchRoutes, getAiRecommendation, generateAiBookingDraft, confirmAiBookingDraft } from '../api'
import { useRouter } from 'vue-router'
import BookingConfirmForm from './BookingConfirmForm.vue'

const router = useRouter()
const loading = ref(false)
const activities = ref([])
const routes = ref([])
const recommendation = ref(null)
const showBookingDialog = ref(false)
const bookingDrafts = ref([])
const selectedDraftIndex = ref(null)
const bookingLoading = ref(false)
const functionCallSteps = ref([])

const recommendForm = ref({
  days: 3,
  startDate: 0,
  budget: 'moderate',
  customPrompt: ''
})

// 渲染markdown为HTML
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    return marked(text)
  } catch (e) {
    console.error('Markdown渲染失败:', e)
    return text
  }
}

const getRecommendation = async () => {
  if (!recommendForm.value.days) {
    ElMessage.warning('请输入旅行天数')
    return
  }

  loading.value = true
  try {
    const response = await getAiRecommendation({
      days: recommendForm.value.days,
      startDate: recommendForm.value.startDate,
      customPrompt: recommendForm.value.customPrompt,
      userId: localStorage.getItem('userId')
    })
    recommendation.value = response.data
    ElMessage.success('✨ 生成成功！')
  } catch (err) {
    ElMessage.error('生成失败: ' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  recommendForm.value = {
    days: 3,
    startDate: 0,
    budget: 'moderate',
    customPrompt: ''
  }
  recommendation.value = null
}

const copyRecommendation = () => {
  if (recommendation.value?.recommendation) {
    navigator.clipboard.writeText(recommendation.value.recommendation)
    ElMessage.success('已复制到剪贴板')
  }
}

const goToBooking = async () => {
  ElMessage.info('正在为您跳转到预订页面...')
  // 延迟跳转，让用户看到提示
  setTimeout(() => {
    router.push({ 
      path: '/customer',
      query: { tab: 'homestay' }
    })
  }, 500)
}

const generateBookingDraft = async () => {
  if (!recommendForm.value.days) {
    ElMessage.warning('请先输入旅行天数')
    return
  }

  bookingLoading.value = true
  
  // 初始化 Function Calling 步骤
  functionCallSteps.value = [
    { text: '正在查询所有民宿信息...', active: true, completed: false },
    { text: '分析民宿详细信息...', active: false, completed: false },
    { text: '检查房间可用性...', active: false, completed: false },
    { text: '生成预订方案...', active: false, completed: false }
  ]
  
  // 模拟步骤进度（因为实际 API 不返回中间状态）
  const stepInterval = setInterval(() => {
    const currentIndex = functionCallSteps.value.findIndex(s => s.active && !s.completed)
    if (currentIndex >= 0 && currentIndex < functionCallSteps.value.length) {
      functionCallSteps.value[currentIndex].completed = true
      functionCallSteps.value[currentIndex].active = false
      if (currentIndex + 1 < functionCallSteps.value.length) {
        functionCallSteps.value[currentIndex + 1].active = true
      }
    }
  }, 1500)
  
  // 显示加载提示
  const loadingMsg = ElMessage({
    message: '🤖 AI正在为您生成订单草稿，请稍候...',
    type: 'info',
    duration: 0, // 不自动关闭
    showClose: false
  })
  
  try {
    const response = await generateAiBookingDraft({
      userId: localStorage.getItem('userId'),
      days: recommendForm.value.days,
      startDate: recommendForm.value.startDate,
      budget: recommendForm.value.budget,
      customPrompt: recommendForm.value.customPrompt
    })
    
    // 清除步骤间隔
    clearInterval(stepInterval)
    
    // 标记所有步骤完成
    functionCallSteps.value.forEach(s => {
      s.completed = true
      s.active = false
    })
    
    // 关闭加载提示
    loadingMsg.close()
    
    if (response.data.success) {
      bookingDrafts.value = response.data.options || []
      selectedDraftIndex.value = null
      
      if (bookingDrafts.value.length > 0) {
        showBookingDialog.value = true
        ElMessage.success({
          message: `✨ 智能订单生成成功！为您推荐了 ${bookingDrafts.value.length} 个方案`,
          duration: 3000
        })
      } else {
        ElMessage.warning({
          message: 'AI未能生成订单草稿，可能是参数不合适或暂无可用民宿',
          duration: 5000
        })
      }
    } else {
      ElMessage.error('生成失败: ' + response.data.message)
    }
  } catch (err) {
    // 清除步骤间隔
    clearInterval(stepInterval)
    
    // 关闭加载提示
    loadingMsg.close()
    console.error('生成订单草稿失败:', err)
    ElMessage.error('生成失败: ' + (err.response?.data?.message || err.message))
  } finally {
    bookingLoading.value = false
    functionCallSteps.value = []
  }
}

const selectDraft = (index) => {
  selectedDraftIndex.value = selectedDraftIndex.value === index ? null : index
}

const confirmBooking = async (bookingData) => {
  try {
    const response = await confirmAiBookingDraft(bookingData)
    
    if (response.data.success) {
      ElMessage.success('🎉 订单创建成功！等待管理员确认')
      closeBookingDialog()
      // 可选：跳转到订单页面
      setTimeout(() => {
        router.push({ 
          path: '/customer',
          query: { tab: 'orders' }
        })
      }, 1000)
    } else {
      ElMessage.error('创建失败: ' + response.data.message)
    }
  } catch (err) {
    console.error('确认订单失败:', err)
    ElMessage.error('创建失败: ' + (err.response?.data?.message || err.message))
  }
}

const closeBookingDialog = () => {
  showBookingDialog.value = false
  bookingDrafts.value = []
  selectedDraftIndex.value = null
}

const loadActivities = async () => {
  try {
    const res = await fetchActivities()
    activities.value = res.data || []
  } catch (err) {
    console.error('加载游玩项目失败:', err)
  }
}

const loadRoutes = async () => {
  try {
    const res = await fetchRoutes()
    routes.value = res.data || []
  } catch (err) {
    console.error('加载旅游路线失败:', err)
  }
}

onMounted(() => {
  loadActivities()
  loadRoutes()
})
</script>

<style scoped>
.ai-recommendation {
  padding: 20px 0;
}

.mb-20 {
  margin-bottom: 20px;
}

.mr-5 {
  margin-right: 5px;
}

/* 订单草稿对话框样式 */
.single-booking h3,
.multiple-bookings h3 {
  margin-bottom: 20px;
  color: #303133;
  font-size: 18px;
}

.booking-options {
  margin-bottom: 24px;
}

.booking-option-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.booking-option-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.booking-option-card.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.option-content h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.option-content p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.option-content .location {
  color: #909399;
}

.option-content .dates {
  color: #67c23a;
  font-weight: 500;
}

.option-content .price {
  color: #e6a23c;
  font-weight: 600;
  font-size: 16px;
}

.option-content .reason {
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-style: italic;
  color: #606266;
}

.option-content .warning {
  margin-top: 12px;
}

.selected-booking {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #ebeef5;
}

.no-drafts {
  text-align: center;
  padding: 40px 0;
}

.button-group {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  align-items: center;
}

.loading-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
}

/* AI 思考图标缩放动画 */
.ai-thinking-icon {
  display: inline-block;
  animation: aiScaling 1.5s ease-in-out infinite;
}

@keyframes aiScaling {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.5);
  }
}

.recommendation-content {
  padding: 20px;
  line-height: 1.8;
  color: #333;
}

/* Markdown样式 */
.recommendation-content :deep(h1),
.recommendation-content :deep(h2),
.recommendation-content :deep(h3),
.recommendation-content :deep(h4),
.recommendation-content :deep(h5) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  color: #222;
}

.recommendation-content :deep(h1) {
  font-size: 24px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.recommendation-content :deep(h2) {
  font-size: 20px;
  color: #409eff;
}

.recommendation-content :deep(h3) {
  font-size: 18px;
}

.recommendation-content :deep(p) {
  margin: 12px 0;
}

.recommendation-content :deep(ul),
.recommendation-content :deep(ol) {
  margin: 12px 0 12px 24px;
}

.recommendation-content :deep(li) {
  margin: 8px 0;
}

.recommendation-content :deep(code) {
  background-color: #f5f7fa;
  color: #e6a23c;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Monaco', 'Courier New', monospace;
}

.recommendation-content :deep(pre) {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 12px 0;
}

.recommendation-content :deep(pre code) {
  background-color: transparent;
  color: #333;
  padding: 0;
}

.recommendation-content :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding-left: 12px;
  margin: 12px 0;
  color: #666;
}

.recommendation-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.recommendation-content :deep(th),
.recommendation-content :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 12px;
  text-align: left;
}

.recommendation-content :deep(th) {
  background-color: #f5f7fa;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
}

/* 智能生成订单按钮顶部样式 */
.booking-action-top {
  text-align: center;
}

.booking-button-prominent {
  font-size: 16px;
  padding: 12px 32px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  transition: all 0.3s ease;
}

.booking-button-prominent:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4);
}

.booking-button-prominent:active {
  transform: translateY(0);
}

.activity-item,
.route-item {
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child,
.route-item:last-child {
  border-bottom: none;
}

.activity-name,
.route-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: #333;
}

.activity-location {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.activity-desc,
.route-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}

.route-tags {
  margin: 8px 0;
}

.list-box {
  display: flex;
  flex-direction: column;
}

/* Function Calling 过程显示 */
.function-calling-process {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4f8 100%);
  border-radius: 12px;
  border: 2px solid #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.process-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 16px;
}

.process-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: white;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #606266;
}

.step-item.active {
  background: #e8f4f8;
  border: 1px solid #409eff;
  color: #409eff;
  font-weight: 500;
}

.step-item.completed {
  background: #f0f9ff;
  color: #67c23a;
}

.step-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.step-text {
  flex: 1;
}
</style>
