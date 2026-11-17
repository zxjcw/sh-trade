<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Bell, Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const notifications = useNotificationStore()
const keyword = ref('')

const nav = [
  { label: '首页', to: '/' },
  { label: '悬赏大厅', to: '/bounty' },
  { label: '物品置换', to: '/exchange' }
]

const consoleEntry = computed(() =>
  auth.role === 'admin' ? '/admin/categories' : '/ops/reviews'
)

const avatarText = computed(() => auth.displayName.slice(0, 1))

function search() {
  const value = keyword.value.trim()
  router.push({ name: 'search', query: value ? { keyword: value } : {} })
}

function goPublish() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/publish' } })
    return
  }
  if (!auth.isVerified) {
    router.push({ name: 'verify', query: { redirect: '/publish' } })
    return
  }
  router.push('/publish')
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await auth.logout()
    notifications.clear()
    router.push('/')
    return
  }
  router.push(command)
}

onMounted(() => {
  if (auth.isLoggedIn) notifications.load().catch(() => undefined)
})

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) notifications.load().catch(() => undefined)
    else notifications.clear()
  }
)
</script>

<template>
  <div class="shell">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link class="brand" to="/">
          <span class="brand-mark">拾</span>
          <span class="brand-text">
            <strong>拾光集</strong>
            <small>校园二手交易平台</small>
          </span>
        </router-link>

        <nav class="nav">
          <router-link
            v-for="item in nav"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: route.path === item.to }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <form class="search" @submit.prevent="search">
          <el-input
            v-model="keyword"
            placeholder="搜索教材、数码、宿舍好物…"
            clearable
            size="default"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </form>

        <div class="actions">
          <el-button class="publish-btn" type="primary" round @click="goPublish">
            <el-icon><Plus /></el-icon>
            发布物品
          </el-button>

          <router-link v-if="auth.isLoggedIn" to="/notifications" class="icon-btn" aria-label="消息">
            <el-badge :value="notifications.unreadCount" :hidden="notifications.unreadCount === 0">
              <el-icon :size="20"><Bell /></el-icon>
            </el-badge>
          </router-link>

          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="user-chip" type="button">
                <span class="avatar">{{ avatarText }}</span>
                <span class="user-name">{{ auth.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="/my-items">我的物品</el-dropdown-item>
                  <el-dropdown-item command="/orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="/wallet">我的钱包</el-dropdown-item>
                  <el-dropdown-item v-if="!auth.isVerified" command="/verify">学籍认证</el-dropdown-item>
                  <el-dropdown-item v-if="auth.role !== 'student'" :command="consoleEntry" divided>
                    {{ auth.role === 'admin' ? '管理台' : '运营台' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="login-link">登录</router-link>
            <router-link to="/register" class="register-link">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="footer-inner">
        <span>拾光集 · 校园二手交易平台</span>
        <span class="footer-dot">·</span>
        <span>物品有履历，交易有托管，信用有记录</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  border-bottom: 1px solid rgba(230, 225, 214, 0.9);
  background: rgba(255, 254, 250, 0.9);
  backdrop-filter: blur(14px);
}

.topbar-inner {
  display: flex;
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
  align-items: center;
  gap: 22px;
  padding: 12px 0;
}

.brand {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(145deg, #2f6b4f, #1f4a37);
  box-shadow: 0 6px 16px rgba(47, 107, 79, 0.25);
  color: #f6f1e4;
  font-size: 20px;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 17px;
  letter-spacing: 3px;
}

.brand-text small {
  color: var(--sgj-muted);
  font-size: 10px;
  letter-spacing: 1px;
}

.nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  position: relative;
  padding: 7px 12px;
  border-radius: 8px;
  color: #5c675f;
  font-size: 14px;
  transition: all 0.18s ease;
}

.nav-link:hover {
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
}

.nav-link.active {
  color: var(--sgj-primary);
  font-weight: 600;
}

.nav-link.active::after {
  position: absolute;
  right: 12px;
  bottom: 1px;
  left: 12px;
  height: 2px;
  border-radius: 2px;
  background: var(--sgj-primary);
  content: '';
}

.search {
  flex: 1;
  max-width: 360px;
  margin-left: auto;
}

.actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
}

.publish-btn {
  padding-inline: 16px;
}

.icon-btn {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 10px;
  color: #5c675f;
  transition: background 0.18s ease;
}

.icon-btn:hover {
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border: 1px solid var(--sgj-line);
  border-radius: 999px;
  background: #fff;
  color: var(--sgj-ink);
  cursor: pointer;
  font-size: 13px;
}

.avatar {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #d9a860, #b57a25);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.login-link {
  color: var(--sgj-ink);
  font-size: 14px;
}

.register-link {
  padding: 7px 16px;
  border-radius: 999px;
  background: var(--sgj-primary);
  color: #fff;
  font-size: 14px;
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--sgj-line);
  background: rgba(255, 254, 250, 0.7);
}

.footer-inner {
  display: flex;
  flex-wrap: wrap;
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  gap: 8px;
  padding: 22px 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.footer-dot {
  color: #c9cec9;
}

@media (max-width: 1080px) {
  .nav {
    display: none;
  }

  .search {
    max-width: none;
  }
}

@media (max-width: 760px) {
  .topbar-inner {
    width: calc(100% - 24px);
    flex-wrap: wrap;
    gap: 10px;
  }

  .brand-text small {
    display: none;
  }

  .search {
    order: 3;
    flex-basis: 100%;
  }

  .publish-btn {
    display: none;
  }

  .user-name {
    display: none;
  }

  .footer-inner {
    width: calc(100% - 32px);
  }
}
</style>
