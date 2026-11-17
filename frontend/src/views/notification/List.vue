<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import type { Notification, NotificationType } from '@/types'
import { notificationTypeColors, notificationTypeLabels } from '@/utils/status'
import { fromNow, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const store = useNotificationStore()

function tagType(type: NotificationType) {
  return notificationTypeColors[type] as 'success' | 'warning' | 'primary' | 'info' | 'danger'
}

async function open(item: Notification) {
  if (!item.read) await store.markRead(item.id)
  if (item.link) router.push(item.link)
}

async function markAll() {
  await store.markAllRead()
  ElMessage.success('已全部标记为已读')
}

onMounted(() => {
  store.load().catch((error) => ElMessage.error((error as Error).message))
})
</script>

<template>
  <div class="page page-narrow">
    <PageHeader
      eyebrow="NOTIFICATIONS"
      title="消息通知"
      subtitle="平台只发送认证、审核、悬赏、置换、售后、仲裁和订单完成类通知。"
    >
      <template #actions>
        <el-button round :disabled="store.unreadCount === 0" @click="markAll">全部已读</el-button>
      </template>
    </PageHeader>

    <div v-loading="store.loading" class="notice-list">
      <article
        v-for="item in store.items"
        :key="item.id"
        class="notice surface"
        :class="{ unread: !item.read }"
        @click="open(item)"
      >
        <span class="dot" :class="{ visible: !item.read }"></span>
        <div class="notice-main">
          <div class="notice-head">
            <el-tag
              :type="tagType(item.type)"
              size="small"
              effect="light"
              round
            >
              {{ notificationTypeLabels[item.type] }}
            </el-tag>
            <strong>{{ item.title }}</strong>
            <span class="time" :title="formatTime(item.createdAt)">{{ fromNow(item.createdAt) }}</span>
          </div>
          <p class="content">{{ item.content }}</p>
        </div>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </article>

      <EmptyState v-if="!store.loading && !store.items.length" description="暂无通知" />
    </div>
  </div>
</template>

<style scoped>
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.notice {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.notice:hover {
  border-color: rgba(47, 107, 79, 0.35);
  box-shadow: 0 8px 22px rgba(36, 49, 42, 0.06);
  transform: translateY(-1px);
}

.notice.unread {
  background: #fffdf7;
}

.dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
  background: transparent;
}

.dot.visible {
  background: var(--sgj-accent);
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.notice-head strong {
  font-size: 14px;
}

.time {
  margin-left: auto;
  color: #a3aaa5;
  font-size: 11px;
}

.content {
  margin: 7px 0 0;
  color: var(--sgj-muted);
  font-size: 13px;
  line-height: 1.7;
}

.arrow {
  color: #c2c8c3;
}
</style>
