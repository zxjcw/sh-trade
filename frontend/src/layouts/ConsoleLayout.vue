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
    <aside class="sidebar">
      <router-link class="brand" to="/">
        <span class="brand-mark">拾</span>
        <span class="brand-text">
          <strong>{{ isAdminArea ? '校方管理台' : '运营工作台' }}</strong>
          <small>拾光集 · 控制台</small>
        </span>
      </router-link>

      <nav class="menu">
        <router-link
          v-for="item in menus"
          :key="item.to"
          :to="item.to"
          class="menu-item"
          :class="{ active: route.path === item.to }"
        >
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
          <el-tag type="warning" effect="light" round>
            {{ auth.role === 'admin' ? '管理员' : '运营' }}
          </el-tag>
          <span class="avatar">{{ avatarText }}</span>
          <span class="name">{{ auth.displayName }}</span>
          <el-button link type="danger" @click="logout">退出</el-button>
        </div>
      </header>

      <main class="workspace-body">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.console {
  display: flex;
  min-height: 100vh;
  background: var(--sgj-bg);
}

.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: 236px;
  flex-direction: column;
  padding: 22px 16px;
  background: #1f3429;
  color: #e9efe9;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 22px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 11px;
  background: #e8d9b8;
  color: #1f3429;
  font-size: 19px;
  font-weight: 700;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-size: 15px;
  letter-spacing: 1px;
}

.brand-text small {
  margin-top: 3px;
  color: rgba(233, 239, 233, 0.55);
  font-size: 10px;
  letter-spacing: 1px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  color: rgba(233, 239, 233, 0.72);
  font-size: 14px;
  transition: all 0.18s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: #fff;
}

.menu-item.active {
  background: rgba(232, 217, 184, 0.14);
  color: #f0e3c8;
  font-weight: 600;
}

.sidebar-foot {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.switch-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  color: rgba(233, 239, 233, 0.6);
  font-size: 13px;
}

.switch-link:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.workspace {
  width: calc(100% - 236px);
  margin-left: 236px;
  padding: 26px 32px 64px;
}

.workspace-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.crumb {
  margin: 0 0 5px;
  color: var(--sgj-muted);
  font-size: 12px;
  letter-spacing: 1px;
}

h1 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 1px;
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #d9a860, #b57a25);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.name {
  font-size: 13px;
}

.workspace-body {
  width: 100%;
  max-width: 1320px;
}

@media (max-width: 900px) {
  .sidebar {
    position: static;
    width: 100%;
    flex-direction: row;
    align-items: center;
    padding: 12px 16px;
  }

  .brand {
    padding: 0;
  }

  .brand-text small,
  .sidebar-foot,
  .account .name {
    display: none;
  }

  .menu {
    flex-direction: row;
    margin-left: 16px;
    overflow-x: auto;
  }

  .menu-item {
    white-space: nowrap;
  }

  .console {
    flex-direction: column;
  }

  .workspace {
    width: 100%;
    margin-left: 0;
    padding: 20px 18px 48px;
  }
}
</style>
