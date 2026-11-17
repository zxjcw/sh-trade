<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Checked,
  CollectionTag,
  DataAnalysis,
  HomeFilled,
  Location,
  Reading,
  Switch as SwitchIcon,
  UserFilled,
  Warning
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const isAdminArea = computed(() => route.path.startsWith('/admin'))

const menus = computed(() =>
  isAdminArea.value
    ? [
        { label: '学籍管理', to: '/admin/roster', icon: Reading },
        { label: '分类管理', to: '/admin/categories', icon: CollectionTag },
        { label: '自提点管理', to: '/admin/pickup-points', icon: Location },
        { label: '运营账号', to: '/admin/operators', icon: UserFilled }
      ]
    : [
        { label: '物品审核', to: '/ops/reviews', icon: Checked },
        { label: '仲裁处理', to: '/ops/arbitrations', icon: Warning },
        { label: '运营报表', to: '/ops/report', icon: DataAnalysis }
      ]
)

const title = computed(() => String(route.meta.title ?? '控制台'))
const avatarText = computed(() => auth.displayName.slice(0, 1))

async function logout() {
  await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await auth.logout()
  notifications.clear()
  router.push('/login')
}
</script>

<template>
  <div class="console">
    <a class="skip-link" href="#workspace">跳到主要内容</a>

    <aside class="sidebar">
      <router-link class="brand" to="/">
        <span class="brand-mark">拾</span>
        <span class="brand-text">
          <strong>{{ isAdminArea ? '校方管理台' : '运营工作台' }}</strong>
          <small>PLATFORM CONSOLE</small>
        </span>
      </router-link>

      <p class="menu-label">{{ isAdminArea ? 'ADMINISTRATION' : 'OPERATIONS' }}</p>

      <nav class="menu">
        <router-link
          v-for="(item, index) in menus"
          :key="item.to"
          :to="item.to"
          class="menu-item"
          :class="{ active: route.path === item.to }"
        >
          <span class="menu-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-foot">
        <router-link v-if="isAdminArea" to="/ops/reviews" class="switch-link">
          <el-icon><SwitchIcon /></el-icon>
          运营工作台
        </router-link>
        <router-link v-else-if="auth.role === 'admin'" to="/admin/categories" class="switch-link">
          <el-icon><SwitchIcon /></el-icon>
          校方管理台
        </router-link>
        <router-link to="/" class="switch-link">
          <el-icon><HomeFilled /></el-icon>
          返回学生端
        </router-link>
      </div>
    </aside>

    <section class="workspace">
      <header class="workspace-head">
        <div>
          <p class="crumb">{{ isAdminArea ? '校方管理' : '平台运营' }} / {{ title }}</p>
          <h1>{{ title }}</h1>
        </div>
        <div class="account">
          <el-tag type="warning" effect="plain">
            {{ auth.role === 'admin' ? '管理员' : '运营' }}
          </el-tag>
          <span class="avatar">{{ avatarText }}</span>
          <span class="name">{{ auth.displayName }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </header>

      <main id="workspace" class="workspace-body">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.console {
  display: flex;
  min-height: 100dvh;
  background: var(--sgj-paper);
}

.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: 244px;
  flex-direction: column;
  padding: 26px 18px 22px;
  border-right: 1px solid rgba(244, 239, 227, 0.1);
  background:
    radial-gradient(90% 80% at 20% 0%, rgba(216, 180, 119, 0.08), transparent 50%),
    var(--sgj-green-deep);
  color: #f4efe3;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 6px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid rgba(244, 239, 227, 0.34);
  background: transparent;
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: 19px;
  font-weight: 600;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-family: var(--sgj-font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.brand-text small {
  margin-top: 3px;
  color: rgba(244, 239, 227, 0.38);
  font-size: 8px;
  letter-spacing: 0.18em;
}

.menu-label {
  margin: 40px 6px 10px;
  color: rgba(244, 239, 227, 0.3);
  font-size: 8px;
  letter-spacing: 0.22em;
}

.menu {
  display: flex;
  flex-direction: column;
}

.menu-item {
  position: relative;
  display: grid;
  grid-template-columns: 24px 18px 1fr;
  align-items: center;
  gap: 9px;
  padding: 12px 10px;
  color: rgba(244, 239, 227, 0.62);
  font-size: 13px;
  letter-spacing: 0.04em;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.menu-item:hover {
  background: rgba(244, 239, 227, 0.05);
  color: #f4efe3;
}

.menu-item.active {
  background: rgba(244, 239, 227, 0.07);
  color: #f4efe3;
}

.menu-item.active::before {
  position: absolute;
  top: 8px;
  bottom: 8px;
  left: 0;
  width: 2px;
  background: var(--sgj-brass);
  content: '';
}

.menu-index {
  color: rgba(244, 239, 227, 0.24);
  font-size: 9px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1em;
}

.menu-item.active .menu-index {
  color: rgba(216, 180, 119, 0.8);
}

.sidebar-foot {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(244, 239, 227, 0.1);
}

.switch-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  color: rgba(244, 239, 227, 0.45);
  font-size: 11px;
  letter-spacing: 0.05em;
  transition: color 0.2s ease;
}

.switch-link:hover {
  color: #f4efe3;
}

.workspace {
  width: calc(100% - 244px);
  margin-left: 244px;
  padding: 30px 40px 80px;
}

.workspace-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  max-width: 1360px;
  margin: 0 auto 30px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--sgj-line);
}

.crumb {
  margin: 0 0 7px;
  color: var(--sgj-faint);
  font-size: 9px;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0;
  font-family: var(--sgj-font-display);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 0.03em;
  line-height: 1.1;
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--sgj-muted);
  font-size: 12px;
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

.name {
  letter-spacing: 0.04em;
}

.workspace-body {
  width: 100%;
  max-width: 1360px;
  margin: 0 auto;
}

@media (max-width: 900px) {
  .console {
    flex-direction: column;
  }

  .sidebar {
    position: static;
    width: 100%;
    flex-direction: row;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    border-right: 0;
    border-bottom: 1px solid rgba(244, 239, 227, 0.12);
    overflow-x: auto;
  }

  .brand {
    flex-shrink: 0;
    padding: 0;
  }

  .brand-text small,
  .menu-label,
  .sidebar-foot,
  .account .name {
    display: none;
  }

  .menu {
    flex-direction: row;
    flex-shrink: 0;
  }

  .menu-item {
    grid-template-columns: 18px 1fr;
    padding: 8px 10px;
    white-space: nowrap;
  }

  .menu-item.active::before {
    top: auto;
    right: 8px;
    bottom: 0;
    left: 8px;
    width: auto;
    height: 1px;
  }

  .menu-index {
    display: none;
  }

  .workspace {
    width: 100%;
    margin-left: 0;
    padding: 24px 18px 60px;
  }

  .workspace-head {
    align-items: flex-start;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
