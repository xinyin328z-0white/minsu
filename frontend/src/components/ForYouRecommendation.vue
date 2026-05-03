<template>
  <div class="for-you-page">
    <div class="recommend-toolbar">
      <div>
        <div class="toolbar-title">为你推荐</div>
        <div class="toolbar-subtitle">{{ recommendations?.summary || '根据你的偏好和相似用户行为推荐民宿与旅游线路' }}</div>
      </div>
      <div class="toolbar-actions">
        <el-button @click="openPreferenceDialog">
          <el-icon><Edit /></el-icon>
          偏好设置
        </el-button>
        <el-button type="primary" :loading="loading" @click="loadRecommendations">
          <el-icon><Refresh /></el-icon>
          刷新推荐
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="preferenceMissing"
      type="warning"
      :closable="false"
      show-icon
      class="mb-16"
      title="请先完善偏好，系统会结合相似用户历史订单生成更准确的协同过滤推荐。"
    />

    <div v-if="preference" class="preference-strip">
      <el-tag v-if="budgetLabel" type="success" effect="plain">{{ budgetLabel }}</el-tag>
      <el-tag v-for="style in splitCsv(preference.travelStyles)" :key="`style-${style}`" effect="plain">
        {{ style }}
      </el-tag>
      <el-tag v-for="location in splitCsv(preference.preferredLocations)" :key="`location-${location}`" type="info" effect="plain">
        {{ location }}
      </el-tag>
      <span v-if="preference.minPrice || preference.maxPrice" class="price-range">
        ¥{{ preference.minPrice || 0 }} - ¥{{ preference.maxPrice || '不限' }}/晚
      </span>
    </div>

    <el-skeleton v-if="loading && !recommendations" :rows="8" animated />

    <template v-else>
      <section class="recommend-section">
        <div class="section-heading">
          <div>
            <h3>推荐民宿</h3>
            <p>按协同过滤得分、预算和偏好匹配排序</p>
          </div>
        </div>

        <el-empty v-if="homestayItems.length === 0" description="暂无民宿推荐" />
        <el-row v-else :gutter="16">
          <el-col
            v-for="item in homestayItems"
            :key="item.id"
            :xs="24"
            :sm="12"
            :lg="6"
            class="recommend-col"
          >
            <el-card class="recommend-card homestay-card" shadow="hover" :body-style="{ padding: '0' }">
              <div class="homestay-cover" :style="getCoverStyle(item)">
                <el-icon v-if="!item.imageUrl" class="cover-icon"><House /></el-icon>
                <el-tag class="score-tag" type="success" effect="dark">
                  {{ displayScore(item.score) }} 分
                </el-tag>
              </div>
              <div class="card-body">
                <div class="card-title">{{ item.name }}</div>
                <div class="meta-line">
                  <el-icon><Location /></el-icon>
                  <span>{{ item.location || '位置待补充' }}</span>
                </div>
                <div class="price-line">
                  <el-icon><Money /></el-icon>
                  <span>¥{{ item.pricePerNight || '-' }}/晚</span>
                  <span class="room-count">{{ item.availableRooms || 0 }} 间房</span>
                </div>
                <p class="desc">{{ item.description || '暂无描述' }}</p>
                <div class="reason-box">{{ item.reason }}</div>
                <el-button type="primary" class="book-btn" @click="emit('book-homestay', item)">
                  <el-icon><Calendar /></el-icon>
                  预订民宿
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>

      <section class="recommend-section">
        <div class="section-heading">
          <div>
            <h3>推荐旅游线路</h3>
            <p>与民宿推荐一起结合偏好和相似用户特征生成</p>
          </div>
        </div>

        <el-empty v-if="routeItems.length === 0" description="暂无旅游线路推荐" />
        <el-row v-else :gutter="16">
          <el-col
            v-for="item in routeItems"
            :key="item.id"
            :xs="24"
            :md="12"
            class="recommend-col"
          >
            <el-card class="recommend-card route-card" shadow="hover">
              <div class="route-header">
                <div>
                  <div class="card-title">{{ item.name }}</div>
                  <div class="route-tags">
                    <el-tag v-for="tag in splitCsv(item.tags)" :key="tag" size="small" effect="plain">
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
                <el-tag type="success" effect="dark">{{ displayScore(item.score) }} 分</el-tag>
              </div>
              <p class="route-desc">{{ item.description || '暂无线路描述' }}</p>
              <div class="reason-box">{{ item.reason }}</div>
            </el-card>
          </el-col>
        </el-row>
      </section>
    </template>

    <el-dialog
      v-model="preferenceDialog"
      title="收集用户偏好"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form :model="preferenceForm" label-width="96px">
        <el-form-item label="旅行偏好">
          <el-checkbox-group v-model="preferenceForm.travelStyles">
            <el-checkbox-button v-for="style in styleOptions" :key="style" :label="style" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="目的位置">
          <el-select
            v-model="preferenceForm.preferredLocations"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入偏好位置"
            style="width: 100%;"
          >
            <el-option v-for="location in locationOptions" :key="location" :label="location" :value="location" />
          </el-select>
        </el-form-item>

        <el-form-item label="预算类型">
          <el-radio-group v-model="preferenceForm.budgetLevel">
            <el-radio-button label="ECONOMY">经济型</el-radio-button>
            <el-radio-button label="MODERATE">舒适型</el-radio-button>
            <el-radio-button label="LUXURY">高档型</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="价格区间">
          <el-slider
            v-model="preferenceForm.priceRange"
            range
            :min="100"
            :max="800"
            :step="10"
            show-stops
          />
          <div class="slider-value">¥{{ preferenceForm.priceRange[0] }} - ¥{{ preferenceForm.priceRange[1] }}/晚</div>
        </el-form-item>

        <el-form-item label="出行人群">
          <el-select v-model="preferenceForm.companionType" placeholder="选择出行人群" style="width: 100%;">
            <el-option label="独自出行" value="SOLO" />
            <el-option label="情侣出行" value="COUPLE" />
            <el-option label="家庭亲子" value="FAMILY" />
            <el-option label="朋友结伴" value="FRIENDS" />
            <el-option label="商务差旅" value="BUSINESS" />
          </el-select>
        </el-form-item>

        <el-form-item label="补充说明">
          <el-input
            v-model="preferenceForm.notes"
            type="textarea"
            :rows="3"
            placeholder="如：喜欢安静、想住河景房、带老人小孩、希望路线轻松等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="preferenceDialog = false">稍后再说</el-button>
        <el-button type="primary" :loading="saving" @click="savePreference">保存并推荐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Edit, House, Location, Money, Refresh } from '@element-plus/icons-vue'
import {
  fetchForYouRecommendations,
  fetchHomestays,
  getImageUrl,
  getUserPreference,
  saveUserPreference
} from '../api'

const emit = defineEmits(['book-homestay'])

const userId = Number(localStorage.getItem('userId'))
const loading = ref(false)
const saving = ref(false)
const preferenceDialog = ref(false)
const preferenceMissing = ref(false)
const preference = ref(null)
const recommendations = ref(null)
const locationOptions = ref([])

const styleOptions = ['温泉', '休闲', '养生', '户外', '骑行', '漂流', '冒险', '文化', '古镇', '亲子', '山水', '美食', '安静']

const preferenceForm = ref({
  travelStyles: [],
  preferredLocations: [],
  budgetLevel: 'MODERATE',
  priceRange: [200, 500],
  companionType: 'COUPLE',
  notes: ''
})

const homestayItems = computed(() => recommendations.value?.homestays || [])
const routeItems = computed(() => recommendations.value?.routes || [])

const budgetLabel = computed(() => {
  const map = {
    ECONOMY: '经济型',
    MODERATE: '舒适型',
    LUXURY: '高档型'
  }
  return preference.value?.budgetLevel ? map[preference.value.budgetLevel] : ''
})

const splitCsv = (value) => {
  if (!value) return []
  return String(value).split(/[,，\s]+/).map(item => item.trim()).filter(Boolean)
}

const displayScore = (score) => Number(score || 0).toFixed(2)

const getCoverStyle = (item) => {
  if (item.imageUrl) {
    return {
      backgroundImage: `url(${getImageUrl(item.imageUrl)})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  const gradients = [
    'linear-gradient(135deg, #2563eb 0%, #16a34a 100%)',
    'linear-gradient(135deg, #f97316 0%, #0ea5e9 100%)',
    'linear-gradient(135deg, #0f766e 0%, #f59e0b 100%)',
    'linear-gradient(135deg, #be123c 0%, #475569 100%)'
  ]
  return { background: gradients[(item.id || 0) % gradients.length] }
}

const loadLocations = async () => {
  try {
    const { data } = await fetchHomestays()
    const locations = (data || []).map(item => item.location).filter(Boolean)
    locationOptions.value = [...new Set(locations)]
  } catch (err) {
    console.error('加载位置选项失败:', err)
  }
}

const loadPreference = async () => {
  try {
    const { data } = await getUserPreference(userId)
    preferenceMissing.value = !data.exists
    preference.value = data.preference || null
    if (preference.value) {
      fillPreferenceForm(preference.value)
    }
    if (!data.exists) {
      preferenceDialog.value = true
    }
  } catch (err) {
    console.error('加载偏好失败:', err)
  }
}

const loadRecommendations = async () => {
  if (!userId) return
  loading.value = true
  try {
    const { data } = await fetchForYouRecommendations(userId)
    recommendations.value = data
    preferenceMissing.value = Boolean(data.preferenceMissing)
    preference.value = data.preference || preference.value
  } catch (err) {
    console.error('加载推荐失败:', err)
    ElMessage.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

const fillPreferenceForm = (item) => {
  preferenceForm.value = {
    travelStyles: splitCsv(item.travelStyles),
    preferredLocations: splitCsv(item.preferredLocations),
    budgetLevel: item.budgetLevel || 'MODERATE',
    priceRange: [item.minPrice || 200, item.maxPrice || 500],
    companionType: item.companionType || 'COUPLE',
    notes: item.notes || ''
  }
}

const openPreferenceDialog = () => {
  if (preference.value) {
    fillPreferenceForm(preference.value)
  }
  preferenceDialog.value = true
}

const savePreference = async () => {
  if (preferenceForm.value.travelStyles.length === 0) {
    ElMessage.warning('请至少选择一个旅行偏好')
    return
  }

  saving.value = true
  try {
    const payload = {
      travelStyles: preferenceForm.value.travelStyles,
      preferredLocations: preferenceForm.value.preferredLocations,
      budgetLevel: preferenceForm.value.budgetLevel,
      minPrice: preferenceForm.value.priceRange[0],
      maxPrice: preferenceForm.value.priceRange[1],
      companionType: preferenceForm.value.companionType,
      notes: preferenceForm.value.notes
    }
    const { data } = await saveUserPreference(userId, payload)
    preference.value = data
    preferenceMissing.value = false
    preferenceDialog.value = false
    ElMessage.success('偏好已保存')
    await loadRecommendations()
  } catch (err) {
    console.error('保存偏好失败:', err)
    ElMessage.error('保存偏好失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadLocations()
  await loadPreference()
  await loadRecommendations()
})
</script>

<style scoped>
.for-you-page {
  padding: 20px 0;
}

.mb-16 {
  margin-bottom: 16px;
}

.recommend-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.toolbar-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.preference-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.price-range {
  color: #475569;
  font-size: 13px;
}

.recommend-section {
  margin-bottom: 22px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
}

.section-heading p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.recommend-col {
  margin-bottom: 16px;
}

.recommend-card {
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.homestay-cover {
  height: 150px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-icon {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.9);
}

.score-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.card-body {
  padding: 14px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.meta-line,
.price-line {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.price-line {
  color: #b45309;
  font-weight: 600;
}

.room-count {
  margin-left: auto;
  color: #64748b;
  font-weight: 400;
}

.desc,
.route-desc {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.reason-box {
  min-height: 44px;
  padding: 9px 10px;
  margin: 12px 0;
  background: #f8fafc;
  border-left: 3px solid #16a34a;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
  border-radius: 6px;
}

.book-btn {
  width: 100%;
}

.route-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.route-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.slider-value {
  width: 100%;
  color: #64748b;
  font-size: 12px;
  margin-top: -4px;
}

@media (max-width: 768px) {
  .recommend-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
