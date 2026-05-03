<template>
  <el-card class="chat-window">
    <template #header>
      <div class="chat-header">
        <span>{{ title }}</span>
        <el-button size="small" @click="refreshMessages">刷新</el-button>
      </div>
    </template>
    
    <div class="messages-container" ref="messageContainer">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.type]">
        <div class="message-meta">
          <span class="sender">{{ msg.type === 'CUSTOMER_INQUIRY' ? '我' : '客服' }}</span>
          <span class="time">{{ formatTime(msg.createdAt) }}</span>
        </div>
        <div class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
      </div>
      <div v-if="messages.length === 0" class="empty-messages">
        暂无消息
      </div>
    </div>
    
    <div class="message-input">
      <el-input
        v-model="newMessage"
        type="textarea"
        :rows="3"
        placeholder="输入消息..."
        @keyup.ctrl.enter="sendMessage"
      />
      <el-button type="primary" @click="sendMessage" :loading="sending">
        发送 (Ctrl+Enter)
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchMessagesByUser, createMessage, markUserAdminMessagesAsRead } from '../api'
import { ElMessage } from 'element-plus'
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

const props = defineProps({
  userId: {
    type: Number,
    required: true
  },
  title: {
    type: String,
    default: '客服消息'
  }
})

const emit = defineEmits(['messagesUpdated'])

const messages = ref([])
let pollTimer = null
const newMessage = ref('')
const sending = ref(false)
const messageContainer = ref(null)
const customerName = ref(localStorage.getItem('username') || `客户${props.userId}`)

const loadMessages = async () => {
  try {
    const res = await fetchMessagesByUser(props.userId)
    const sortedMessages = res.data.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    
    // 计算未读消息数（管理员回复未读的）
    const unreadCount = sortedMessages.filter(m => m.type === 'ADMIN_REPLY' && !m.isRead).length
    
    // 检查是否有新消息
    const hasNewMessages = sortedMessages.length !== messages.value.length ||
      (sortedMessages.length > 0 && messages.value.length > 0 &&
       sortedMessages[sortedMessages.length - 1].id !== messages.value[messages.value.length - 1]?.id)
    
    messages.value = sortedMessages
    
    // 通知父组件消息更新
    emit('messagesUpdated', { unreadCount, hasNewMessages })
    
    await markUserAdminMessagesAsRead(props.userId)
    await nextTick()
    scrollToBottom()
  } catch (err) {
    console.error('加载消息失败:', err)
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  sending.value = true
  try {
    await createMessage({
      userId: props.userId,
      customerName: customerName.value,
      content: newMessage.value,
      type: 'CUSTOMER_INQUIRY',
      isRead: false
    })
    newMessage.value = ''
    await loadMessages()
    ElMessage.success('发送成功')
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

onMounted(() => {
  loadMessages()
  // Auto refresh every 3 seconds for better real-time experience
  pollTimer = setInterval(loadMessages, 3000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.chat-window {
  height: 600px;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 16px;
  max-height: 400px;
}

.message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  max-width: 70%;
}

.message.CUSTOMER_INQUIRY {
  background-color: #409eff;
  color: white;
  margin-left: auto;
  text-align: left;
}

.message.ADMIN_REPLY {
  background-color: white;
  border: 1px solid #dcdfe6;
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

.message.ADMIN_REPLY .message-content.markdown-body :deep(code) {
  background-color: #f0f0f0;
}

.message.ADMIN_REPLY .message-content.markdown-body :deep(pre) {
  background-color: #f5f5f5;
}

.message.ADMIN_REPLY .message-content.markdown-body :deep(blockquote) {
  border-left-color: #409eff;
}

.empty-messages {
  text-align: center;
  color: #909399;
  padding: 40px;
}

.message-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.message-input .el-input {
  flex: 1;
}
</style>
