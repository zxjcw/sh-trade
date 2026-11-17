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
    <a class="skip-link" href="#main">跳到主要内容</a>

    <header class="topbar">
      <div class="topbar-inner">
        <router-link class="brand" to="/">
          <span class="brand-mark">拾</span>
          <span class="brand-text">
            <strong>拾光集</strong>
            <small>CAMPUS EXCHANGE</small>
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
          <el-input v-model="keyword" placeholder="搜索教材、数码、宿舍好物" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </form>

        <div class="actions">
          <el-button class="publish-btn" type="primary" @click="goPublish">
            <el-icon><Plus /></el-icon>
            发布物品
          </el-button>

          <router-link v-if="auth.isLoggedIn" to="/notifications" class="icon-btn" aria-label="消息">
            <el-badge :value="notifications.unreadCount" :hidden="notifications.unreadCount === 0">
              <el-icon :size="19"><Bell /></el-icon>
            </el-badge>
          </router-link>

          <template v-if="auth.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="user-chip" type="button">
                <span class="avatar">{{ avatarText }}</span>
                <span class="user-name">{{ auth.displayName }}</span>
                <el-icon class="chevron"><ArrowDown /></el-icon>
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

    <main id="main" class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-mark">拾</span>
          <span>拾光集 · 校园二手交易平台</span>
        </div>
        <span class="footer-meta">物品履历 / 资金托管 / 信用记录</span>
        <span class="footer-copy">© 2026</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100dvh;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  border-bottom: 1px solid var(--sgj-line);
  background: rgba(241, 237, 228, 0.9);
  backdrop-filter: blur(16px);
}

.topbar-inner {
  display: flex;
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  align-items: center;
  gap: 28px;
  padding: 14px 0;
}

.brand {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 11px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid var(--sgj-green-deep);
  border-radius: 1px;
  background: var(--sgj-green);
  color: #f6f1e4;
  font-family: var(--sgj-font-display);
  font-size: 19px;
  font-weight: 600;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-family: var(--sgj-font-display);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.brand-text small {
  margin-top: 3px;
  color: var(--sgj-faint);
  font-size: 8px;
  letter-spacing: 0.22em;
}

.nav {
  display: flex;
  gap: 22px;
}

.nav-link {
  position: relative;
  padding: 8px 0;
  color: var(--sgj-muted);
  font-size: 12px;
  letter-spacing: 0.1em;
  transition: color 0.2s ease;
}

.nav-link:hover,
.nav-link.active {
  color: var(--sgj-ink);
}

.nav-link.active::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  background: var(--sgj-brass);
  content: '';
}

.search {
  flex: 1;
  max-width: 300px;
  margin-left: auto;
}

.search :deep(.el-input__wrapper) {
  padding: 2px 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  border-bottom: 1px solid var(--sgj-line-strong);
}

.search :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--sgj-ink);
  box-shadow: none;
}

.search :deep(.el-input__inner) {
  font-size: 12px;
  letter-spacing: 0.03em;
}

.search :deep(.el-input__prefix) {
  color: var(--sgj-faint);
}

.actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 16px;
}

.publish-btn {
  padding-inline: 15px;
  font-size: 12px;
  letter-spacing: 0.06em;
}

.icon-btn {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: var(--sgj-muted);
  transition: color 0.2s ease;
}

.icon-btn:hover {
  color: var(--sgj-ink);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--sgj-ink);
  cursor: pointer;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.avatar {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 1px;
  background: var(--sgj-brass);
  color: #fbf8f1;
  font-family: var(--sgj-font-display);
  font-size: 14px;
  font-weight: 600;
}

.chevron {
  color: var(--sgj-faint);
  font-size: 11px;
}

.login-link {
  color: var(--sgj-ink-soft);
  font-size: 12px;
  letter-spacing: 0.06em;
}

.login-link:hover {
  color: var(--sgj-green);
}

.register-link {
  padding: 7px 16px;
  border: 1px solid var(--sgj-green);
  background: var(--sgj-green);
  color: #f7f3e9;
  font-size: 12px;
  letter-spacing: 0.08em;
  transition: background-color 0.2s ease;
}

.register-link:hover {
  background: var(--sgj-green-deep);
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--sgj-line);
}

.footer-inner {
  display: flex;
  flex-wrap: wrap;
  width: min(1240px, calc(100% - 64px));
  margin: 0 auto;
  align-items: center;
  gap: 14px;
  padding: 24px 0 28px;
  color: var(--sgj-faint);
  font-size: 10px;
  letter-spacing: 0.1em;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--sgj-muted);
}

.footer-mark {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border: 1px solid var(--sgj-line-strong);
  color: var(--sgj-ink-soft);
  font-family: var(--sgj-font-display);
  font-size: 11px;
}

.footer-meta {
  margin-left: auto;
}

.footer-copy {
  font-variant-numeric: tabular-nums;
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
    width: calc(100% - 32px);
    flex-wrap: wrap;
    gap: 10px 14px;
    padding: 12px 0;
  }

  .brand-text small {
    display: none;
  }

  .search {
    order: 3;
    flex-basis: 100%;
    max-width: none;
  }

  .publish-btn {
    display: none;
  }

  .user-name {
    display: none;
  }

  .actions {
    gap: 10px;
    margin-left: auto;
  }

  .footer-inner {
    width: calc(100% - 32px);
  }

  .footer-meta {
    display: none;
  }

  .footer-copy {
    margin-left: auto;
  }
}
</style>
