<template>
  <el-card>
    <template #header>AI服务配置</template>
    <el-form :model="aiConfig" label-width="150px" size="default">
      <el-form-item label="启用AI服务">
        <el-switch v-model="aiConfig.aiEnabled" />
      </el-form-item>
      <el-form-item label="API密钥">
        <el-input 
          v-model="aiConfig.aiApiKey"
          type="password"
          show-password
          placeholder="输入OpenAI API密钥"
        />
        <div style="font-size: 12px; color: #909399; margin-top: 8px;">
          提示：保存时只更新非空密钥。已保存的密钥显示为***
        </div>
      </el-form-item>
      <el-form-item label="模型">
        <el-input 
          v-model="aiConfig.aiModel"
          placeholder="例：gpt-3.5-turbo, gpt-4"
        />
      </el-form-item>
      <el-form-item label="API Base URL">
        <el-input 
          v-model="aiConfig.aiBaseUrl"
          placeholder="例：https://api.openai.com/v1"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveAiConfig" :loading="savingAiConfig">保存配置</el-button>
        <el-button @click="loadAiConfig">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, updateAiConfig } from '../api'

const aiConfig = ref({
  aiEnabled: false,
  aiApiKey: '',
  aiModel: '',
  aiBaseUrl: ''
})
const savingAiConfig = ref(false)

const loadAiConfig = async () => {
  try {
    const res = await getAiConfig()
    const settings = res.data || {}
    aiConfig.value = {
      aiEnabled: settings.aiEnabled === true,
      aiApiKey: settings.aiApiKey ? '***' : '',
      aiModel: settings.aiModel || '',
      aiBaseUrl: settings.aiBaseUrl || ''
    }
  } catch (err) {
    ElMessage.error('加载配置失败: ' + (err.response?.data?.message || err.message))
  }
}

const saveAiConfig = async () => {
  savingAiConfig.value = true
  try {
    const configToSave = {
      aiEnabled: aiConfig.value.aiEnabled,
      aiModel: aiConfig.value.aiModel,
      aiBaseUrl: aiConfig.value.aiBaseUrl
    }
    
    // 只在密钥非*** 时才更新
    if (aiConfig.value.aiApiKey && aiConfig.value.aiApiKey !== '***') {
      configToSave.aiApiKey = aiConfig.value.aiApiKey
    }
    
    await updateAiConfig(configToSave)
    ElMessage.success('AI配置已保存')
    await loadAiConfig()
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data?.message || err.message))
  } finally {
    savingAiConfig.value = false
  }
}

onMounted(() => {
  loadAiConfig()
})
</script>
