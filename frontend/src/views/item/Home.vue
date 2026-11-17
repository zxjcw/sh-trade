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
  <div class="page home">
    <section class="hero">
      <div class="hero-meta">
        <span>拾光集 · 校园二手目录</span>
        <span>VOL. 01 / 2026 AUTUMN</span>
      </div>

      <div class="hero-grid">
        <div class="hero-copy">
          <p class="hero-kicker">CURATED SECOND-HAND / CAMPUS ONLY</p>
          <h1>让旧物<br />遇见<span class="accent">下一位主人</span></h1>
          <p class="lead">
            教材、数码、宿舍好物，在校园里继续被使用。每件物品都有可查履历，每笔交易都有资金托管。
          </p>

          <form class="hero-search" @submit.prevent="search">
            <el-input v-model="keyword" size="large" placeholder="搜教材、数码、宿舍好物" clearable>
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button @click="search">搜索</el-button>
              </template>
            </el-input>
          </form>

          <div class="hero-actions">
            <el-button class="hero-primary" size="large" @click="goPublish">发布物品</el-button>
            <el-button class="hero-ghost" size="large" @click="router.push('/bounty')">发布求购</el-button>
          </div>
        </div>

        <aside class="ledger">
          <div class="ledger-row">
            <span>认证学生</span>
            <strong>{{ String(stats.verifiedUsers).padStart(2, '0') }}</strong>
            <em>VERIFIED</em>
          </div>
          <div class="ledger-row">
            <span>在售物品</span>
            <strong>{{ String(stats.onSaleItems).padStart(2, '0') }}</strong>
            <em>ON VIEW</em>
          </div>
          <div class="ledger-row">
            <span>已完成交易</span>
            <strong>{{ String(stats.completedOrders).padStart(2, '0') }}</strong>
            <em>CLOSED</em>
          </div>
          <div class="ledger-note">
            <span>累计成交</span>
            <strong>¥{{ stats.totalAmount.toFixed(2) }}</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="category-strip">
      <button class="category-item" type="button" @click="router.push('/search')">
        <span class="cat-index">00</span>
        <span class="cat-name">全部物品</span>
        <span class="cat-count">{{ stats.onSaleItems }}</span>
      </button>
      <button
        v-for="(category, index) in categories"
        :key="category.id"
        class="category-item"
        type="button"
        @click="goCategory(category.id)"
      >
        <span class="cat-index">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="cat-name">{{ category.name }}</span>
        <span class="cat-count">{{ category.children?.length ?? 0 }}</span>
      </button>
    </section>

    <section class="feature-band">
      <button class="feature-card" type="button" @click="router.push('/bounty')">
        <span class="feature-num">I</span>
        <span class="feature-copy">
          <span class="feature-kicker">SOURCING REQUEST</span>
          <strong>发布求购悬赏</strong>
          <span class="feature-desc">留下预算和关键词，物品上架时自动通知你</span>
        </span>
        <span class="feature-arrow">↗</span>
      </button>
      <button class="feature-card" type="button" @click="router.push('/exchange')">
        <span class="feature-num">II</span>
        <span class="feature-copy">
          <span class="feature-kicker">OBJECT EXCHANGE</span>
          <strong>发起物品置换</strong>
          <span class="feature-desc">系统匹配二人互换或三人环，以物换物</span>
        </span>
        <span class="feature-arrow">↗</span>
      </button>
    </section>

    <div class="section-head">
      <div>
        <p class="section-kicker">CATALOGUE / NEW ARRIVALS</p>
        <h2>最新上架</h2>
        <p>刚刚通过审核、正在等待新主人的物品</p>
      </div>
      <router-link class="more" to="/search">
        查看全部 / {{ String(items.length).padStart(2, '0') }}
      </router-link>
    </div>

    <div v-loading="loading" class="grid-wrap">
      <div v-if="items.length" class="grid-cards">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>
      <EmptyState v-else-if="!loading" description="暂时没有在售物品">
        <el-button type="primary" @click="goPublish">成为第一个发布的人</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  padding: 26px 40px 42px;
  overflow: hidden;
  border: 1px solid var(--sgj-green-deep);
  background:
    radial-gradient(90% 120% at 82% 8%, rgba(216, 180, 119, 0.1), transparent 46%),
    radial-gradient(80% 100% at 8% 100%, rgba(216, 180, 119, 0.07), transparent 50%),
    var(--sgj-green-deep);
  color: #f4efe3;
}

.hero::before {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.09'/%3E%3C/svg%3E");
  content: '';
  pointer-events: none;
}

.hero-meta {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(244, 239, 227, 0.16);
  color: rgba(244, 239, 227, 0.55);
  font-size: 9px;
  letter-spacing: 0.22em;
}

.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.5fr 0.72fr;
  gap: 56px;
  padding-top: 40px;
}

.hero-kicker {
  margin: 0 0 18px;
  color: rgba(216, 180, 119, 0.85);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.26em;
}

.hero-copy h1 {
  margin: 0;
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: clamp(42px, 4.6vw, 68px);
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.16;
}

.hero-copy .accent {
  color: #d8b477;
}

.lead {
  max-width: 32em;
  margin: 20px 0 26px;
  color: rgba(244, 239, 227, 0.66);
  font-size: 13px;
  line-height: 1.95;
}

.hero-search {
  max-width: 480px;
}

.hero-search :deep(.el-input__wrapper) {
  border-radius: 0;
  background: #f4efe3;
  box-shadow: none;
}

.hero-search :deep(.el-input-group__append) {
  border-radius: 0;
  background: #d8b477;
  box-shadow: none;
}

.hero-search :deep(.el-input-group__append .el-button) {
  border: 0;
  background: transparent;
  color: #223d2d;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.hero-primary {
  --el-button-bg-color: #f4efe3;
  --el-button-border-color: #f4efe3;
  --el-button-text-color: #223d2d;
  --el-button-hover-bg-color: #fffaf0;
  --el-button-hover-border-color: #fffaf0;
  --el-button-hover-text-color: #15281d;
}

.hero-ghost {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(244, 239, 227, 0.32);
  --el-button-text-color: #f4efe3;
  --el-button-hover-bg-color: rgba(244, 239, 227, 0.08);
  --el-button-hover-border-color: rgba(244, 239, 227, 0.7);
  --el-button-hover-text-color: #fff;
}

.ledger {
  display: flex;
  flex-direction: column;
  align-self: end;
}

.ledger-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: baseline;
  gap: 6px 14px;
  padding: 15px 0;
  border-top: 1px solid rgba(244, 239, 227, 0.16);
}

.ledger-row span {
  color: rgba(244, 239, 227, 0.56);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.ledger-row strong {
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: 30px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.ledger-row em {
  grid-column: 2;
  color: rgba(244, 239, 227, 0.38);
  font-size: 8px;
  font-style: normal;
  letter-spacing: 0.2em;
  text-align: right;
}

.ledger-note {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid rgba(244, 239, 227, 0.16);
  color: rgba(244, 239, 227, 0.48);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.ledger-note strong {
  color: #d8b477;
  font-family: var(--sgj-font-display);
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.category-strip {
  display: flex;
  margin-top: 22px;
  overflow-x: auto;
  border-top: 1px solid var(--sgj-line);
  border-bottom: 1px solid var(--sgj-line);
}

.category-item {
  display: flex;
  flex-shrink: 0;
  align-items: baseline;
  gap: 12px;
  padding: 15px 22px;
  border: 0;
  border-right: 1px solid var(--sgj-line);
  background: transparent;
  color: var(--sgj-ink);
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease;
}

.category-item:hover {
  background: var(--sgj-paper-deep);
}

.cat-index {
  color: var(--sgj-faint);
  font-size: 9px;
  letter-spacing: 0.12em;
}

.cat-name {
  font-size: 13px;
  letter-spacing: 0.04em;
}

.cat-count {
  color: var(--sgj-brass);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.feature-band {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 26px;
}

.feature-card {
  display: grid;
  grid-template-columns: 42px 1fr 26px;
  align-items: center;
  gap: 18px;
  padding: 24px 26px;
  border: 1px solid var(--sgj-line);
  background: var(--sgj-paper-deep);
  color: var(--sgj-ink);
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.25s ease,
    color 0.25s ease,
    border-color 0.25s ease;
}

.feature-card:hover {
  border-color: var(--sgj-green-deep);
  background: var(--sgj-green-deep);
  color: #f4efe3;
}

.feature-num {
  color: var(--sgj-brass);
  font-family: var(--sgj-font-display);
  font-size: 26px;
  line-height: 1;
}

.feature-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature-kicker {
  color: var(--sgj-faint);
  font-size: 8px;
  letter-spacing: 0.22em;
}

.feature-copy strong {
  font-family: var(--sgj-font-display);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.feature-desc {
  color: var(--sgj-muted);
  font-size: 11px;
  line-height: 1.7;
}

.feature-card:hover .feature-kicker,
.feature-card:hover .feature-desc {
  color: rgba(244, 239, 227, 0.56);
}

.feature-arrow {
  color: var(--sgj-brass);
  font-size: 20px;
}

.section-kicker {
  margin: 0 0 6px;
  color: var(--sgj-brass);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.22em;
}

.grid-wrap {
  min-height: 240px;
}

@media (max-width: 980px) {
  .hero {
    padding: 24px 28px 34px;
  }

  .hero-grid {
    grid-template-columns: 1fr;
    gap: 30px;
    padding-top: 32px;
  }

  .ledger {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 0 20px;
    align-self: auto;
  }

  .ledger-row {
    grid-template-columns: 1fr;
  }

  .ledger-row em {
    display: none;
  }

  .ledger-row strong {
    font-size: 26px;
  }

  .ledger-note {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .hero {
    padding: 20px 20px 28px;
  }

  .hero-meta span:last-child {
    display: none;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .lead {
    font-size: 12px;
  }

  .ledger {
    grid-template-columns: repeat(3, 1fr);
    gap: 0 14px;
  }

  .ledger-row strong {
    font-size: 22px;
  }

  .feature-band {
    grid-template-columns: 1fr;
  }

  .feature-card {
    grid-template-columns: 34px 1fr 20px;
    gap: 14px;
    padding: 20px;
  }

  .feature-copy strong {
    font-size: 19px;
  }

  .category-item {
    padding: 13px 18px;
  }
}
</style>
