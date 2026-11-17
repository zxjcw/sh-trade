<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/catalog'
import { itemApi } from '@/api/item'
import { useAuthStore } from '@/stores/auth'
import type { Category, Item, PublicStats } from '@/types'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()

const categories = ref<Category[]>([])
const items = ref<Item[]>([])
const stats = ref<PublicStats>({
  verifiedUsers: 0,
  onSaleItems: 0,
  completedOrders: 0,
  totalAmount: 0
})
const keyword = ref('')
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [categoryList, itemList, statData] = await Promise.all([
      catalogApi.categories(),
      itemApi.list({ status: 'on_sale', sort: 'latest', limit: 8 }),
      catalogApi.publicStats()
    ])
    categories.value = categoryList
      .filter((category) => category.parentId === null)
      .map((category) => ({
        ...category,
        children: categoryList.filter((child) => child.parentId === category.id)
      }))
    items.value = itemList
    stats.value = statData
  } finally {
    loading.value = false
  }
}

function search() {
  router.push({ name: 'search', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

function goCategory(id: number) {
  router.push({ name: 'search', query: { categoryId: String(id) } })
}

function goPublish() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/publish' } })
  } else if (!auth.isVerified) {
    router.push({ name: 'verify', query: { redirect: '/publish' } })
  } else {
    router.push('/publish')
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">CAMPUS SECOND-HAND · 可信流转</p>
        <h1>让旧物<br />遇见下一位主人</h1>
        <p class="lead">
          教材、数码、宿舍好物，在校园里继续被使用。每件物品都有可查履历，每笔交易都有资金托管。
        </p>
        <form class="hero-search" @submit.prevent="search">
          <el-input
            v-model="keyword"
            size="large"
            placeholder="搜教材、数码、宿舍好物…"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="search">搜索</el-button>
            </template>
          </el-input>
        </form>
        <div class="hero-actions">
          <el-button type="primary" round size="large" @click="goPublish">发布物品</el-button>
          <el-button round size="large" plain @click="router.push('/bounty')">发布求购</el-button>
        </div>
      </div>

      <div class="hero-side">
        <div class="stat-card">
          <span class="stat-label">认证学生</span>
          <strong>{{ stats.verifiedUsers }}</strong>
          <span class="stat-hint">仅本校学生可交易</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">在售物品</span>
          <strong>{{ stats.onSaleItems }}</strong>
          <span class="stat-hint">正在等待新主人</span>
        </div>
        <div class="stat-card wide">
          <span class="stat-label">已完成交易</span>
          <strong>{{ stats.completedOrders }}</strong>
          <span class="stat-hint">累计成交 ¥{{ stats.totalAmount.toFixed(2) }}</span>
        </div>
      </div>
    </section>

    <section class="category-strip">
      <button class="category-chip" type="button" @click="router.push('/search')">
        <span class="chip-icon">全</span>
        全部物品
      </button>
      <button
        v-for="category in categories"
        :key="category.id"
        class="category-chip"
        type="button"
        @click="goCategory(category.id)"
      >
        <span class="chip-icon">{{ category.name.slice(0, 1) }}</span>
        {{ category.name }}
        <span class="chip-count">{{ category.children?.length ?? 0 }}</span>
      </button>
    </section>

    <section class="feature-band">
      <button class="feature-card bounty" type="button" @click="router.push('/bounty')">
        <span class="feature-kicker">找不到想要的？</span>
        <strong>发布求购悬赏</strong>
        <span class="feature-desc">留下预算和关键词，物品上架时自动通知你</span>
        <span class="feature-arrow">→</span>
      </button>
      <button class="feature-card exchange" type="button" @click="router.push('/exchange')">
        <span class="feature-kicker">不想花钱买？</span>
        <strong>发起物品置换</strong>
        <span class="feature-desc">系统匹配二人互换或三人环，以物换物</span>
        <span class="feature-arrow">→</span>
      </button>
    </section>

    <div class="section-head">
      <div>
        <h2>最新上架</h2>
        <p>刚刚通过审核、正在等待新主人的物品</p>
      </div>
      <router-link class="more" to="/search">查看全部 →</router-link>
    </div>

    <div v-loading="loading" class="grid-wrap">
      <div v-if="items.length" class="grid-cards">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>
      <EmptyState v-else-if="!loading" description="暂时没有在售物品">
        <el-button type="primary" round @click="goPublish">成为第一个发布的人</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 22px;
  padding: 42px 44px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 82% 18%, rgba(232, 217, 184, 0.22), transparent 34%),
    radial-gradient(circle at 8% 92%, rgba(201, 138, 46, 0.2), transparent 38%),
    linear-gradient(145deg, #1f3b2c 0%, #2f6b4f 58%, #447e5f 100%);
  box-shadow: 0 24px 48px rgba(31, 59, 44, 0.18);
  color: #f6f1e4;
}

.eyebrow {
  margin: 0 0 14px;
  color: rgba(246, 241, 228, 0.6);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(36px, 4vw, 54px);
  font-weight: 600;
  letter-spacing: 2px;
  line-height: 1.22;
}

.lead {
  max-width: 480px;
  margin: 16px 0 24px;
  color: rgba(246, 241, 228, 0.72);
  font-size: 14px;
  line-height: 1.9;
}

.hero-search {
  max-width: 520px;
}

.hero-search :deep(.el-input__wrapper) {
  border-radius: 12px 0 0 12px;
  box-shadow: none;
}

.hero-search :deep(.el-input-group__append) {
  border-radius: 0 12px 12px 0;
  background: #f0e3c8;
  box-shadow: none;
}

.hero-search :deep(.el-input-group__append .el-button) {
  border: 0;
  background: transparent;
  color: #234f3b;
  font-weight: 600;
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.hero-actions :deep(.el-button--primary) {
  --el-button-bg-color: #e8d9b8;
  --el-button-border-color: #e8d9b8;
  --el-button-text-color: #234f3b;
  --el-button-hover-bg-color: #f0e3c8;
  --el-button-hover-border-color: #f0e3c8;
  --el-button-hover-text-color: #1d3a2b;
}

.hero-actions :deep(.el-button.is-plain) {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(246, 241, 228, 0.4);
  --el-button-text-color: #f6f1e4;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.1);
  --el-button-hover-border-color: rgba(246, 241, 228, 0.7);
  --el-button-hover-text-color: #fff;
}

.hero-side {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-content: center;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  border: 1px solid rgba(246, 241, 228, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(6px);
}

.stat-card.wide {
  grid-column: span 2;
}

.stat-label {
  color: rgba(246, 241, 228, 0.6);
  font-size: 12px;
}

.stat-card strong {
  font-size: 30px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.stat-hint {
  color: rgba(246, 241, 228, 0.5);
  font-size: 11px;
}

.category-strip {
  display: flex;
  gap: 10px;
  margin: 26px 0 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.category-chip {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 8px 14px 8px 8px;
  border: 1px solid var(--sgj-line);
  border-radius: 999px;
  background: var(--sgj-surface);
  color: var(--sgj-ink);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.16s ease;
}

.category-chip:hover {
  border-color: var(--sgj-primary);
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
}

.chip-icon {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
  font-size: 12px;
  font-weight: 600;
}

.chip-count {
  color: #a3aaa5;
  font-size: 11px;
}

.feature-band {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin: 26px 0 4px;
}

.feature-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 22px 24px;
  overflow: hidden;
  border: 0;
  border-radius: 18px;
  color: #f6f1e4;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
}

.feature-card.bounty {
  background: linear-gradient(125deg, #5b4425, #9a6f2e);
}

.feature-card.exchange {
  background: linear-gradient(125deg, #274a55, #3f6f78);
}

.feature-kicker {
  color: rgba(246, 241, 228, 0.62);
  font-size: 11px;
  letter-spacing: 1px;
}

.feature-card strong {
  font-size: 20px;
  letter-spacing: 1px;
}

.feature-desc {
  max-width: 420px;
  color: rgba(246, 241, 228, 0.66);
  font-size: 12px;
}

.feature-arrow {
  position: absolute;
  right: 22px;
  bottom: 18px;
  font-size: 22px;
  opacity: 0.7;
}

.grid-wrap {
  min-height: 220px;
}

@media (max-width: 980px) {
  .hero {
    grid-template-columns: 1fr;
    padding: 32px 28px;
  }

  .hero-side {
    grid-template-columns: repeat(3, 1fr);
  }

  .stat-card.wide {
    grid-column: auto;
  }
}

@media (max-width: 760px) {
  .hero {
    padding: 28px 22px;
  }

  .hero-side {
    display: none;
  }

  .feature-band {
    grid-template-columns: 1fr;
  }
}
</style>
