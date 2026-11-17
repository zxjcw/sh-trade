<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { itemApi } from '@/api/item'
import { useAuthStore } from '@/stores/auth'
import type { CreditRecord, Item, Review } from '@/types'
import { formatDate, formatMoney, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('credit')
const loading = ref(true)
const credits = ref<CreditRecord[]>([])
const reviews = ref<Review[]>([])
const favorites = ref<Item[]>([])

const roleLabel = {
  student: '学生',
  operator: '平台运营',
  admin: '校方管理员'
}

async function load() {
  loading.value = true
  try {
    const [creditList, reviewList, favoriteList] = await Promise.all([
      userApi.creditRecords(),
      userApi.reviews(),
      itemApi.favorites()
    ])
    credits.value = creditList
    reviews.value = reviewList
    favorites.value = favoriteList
    await auth.fetchMe()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader eyebrow="PROFILE" title="个人中心" subtitle="账号、信用、评价与收藏都在这里。" />

    <section class="profile-card surface">
      <div class="profile-main">
        <span class="avatar">{{ auth.displayName.slice(0, 1) }}</span>
        <div class="identity">
          <div class="name-row">
            <h1>{{ auth.displayName }}</h1>
            <el-tag v-if="auth.isVerified" type="success" effect="light" round size="small">已认证</el-tag>
            <el-tag v-else type="warning" effect="light" round size="small">未认证</el-tag>
            <el-tag effect="plain" round size="small">{{ roleLabel[auth.role] }}</el-tag>
          </div>
          <p class="username">@{{ auth.user?.username }}</p>
          <p v-if="auth.user?.studentNo" class="school">
            {{ auth.user?.college }} · {{ auth.user?.major }} · {{ auth.user?.grade }} 级 · 学号
            {{ auth.user?.studentNo }}
          </p>
        </div>
      </div>

      <div class="metrics">
        <div class="metric">
          <span>信用分</span>
          <strong>{{ auth.user?.creditScore ?? 0 }}</strong>
        </div>
        <div class="metric">
          <span>钱包余额</span>
          <strong>{{ formatMoney(auth.user?.balance ?? 0) }}</strong>
        </div>
        <div class="metric">
          <span>收藏</span>
          <strong>{{ favorites.length }}</strong>
        </div>
      </div>

      <div class="profile-actions">
        <el-button type="primary" round @click="router.push('/wallet')">我的钱包</el-button>
        <el-button round @click="router.push('/orders')">我的订单</el-button>
        <el-button round @click="router.push('/my-items')">我的物品</el-button>
        <el-button v-if="!auth.isVerified" type="warning" plain round @click="router.push('/verify')">
          去认证
        </el-button>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="信用记录" name="credit">
        <div v-loading="loading" class="surface panel">
          <el-timeline v-if="credits.length">
            <el-timeline-item
              v-for="record in credits"
              :key="record.id"
              :timestamp="formatTime(record.createdAt)"
              placement="top"
            >
              <div class="credit-row">
                <span class="reason">{{ record.reason }}</span>
                <el-tag :type="record.change >= 0 ? 'success' : 'danger'" effect="light" round size="small">
                  {{ record.change >= 0 ? '+' : '' }}{{ record.change }}
                </el-tag>
                <span v-if="record.orderId" class="order">订单 {{ record.orderId }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <EmptyState v-else-if="!loading" description="还没有信用变动记录" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的评价" name="reviews">
        <div v-loading="loading" class="review-list">
          <article v-for="review in reviews" :key="review.id" class="review-card surface">
            <div class="review-head">
              <div>
                <strong>{{ review.reviewerId === auth.user?.id ? '我评价的' : '收到的评价' }}</strong>
                <span class="muted"> · {{ review.itemTitle }}</span>
              </div>
              <el-rate :model-value="review.rating" disabled />
            </div>
            <div class="review-tags">
              <el-tag v-for="tag in review.tags" :key="tag" size="small" effect="plain" round>{{ tag }}</el-tag>
              <el-tag v-if="review.status !== 'valid'" type="info" size="small" round>
                {{ review.status === 'void' ? '已作废' : '系统默认' }}
              </el-tag>
            </div>
            <p class="review-content">{{ review.content }}</p>
            <p class="review-foot">
              {{ review.reviewerName }} → {{ review.targetName }} · {{ formatDate(review.createdAt) }}
            </p>
          </article>
          <EmptyState v-if="!loading && !reviews.length" description="还没有评价记录" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的收藏" name="favorites">
        <div v-loading="loading">
          <div v-if="favorites.length" class="grid-cards">
            <ItemCard v-for="item in favorites" :key="item.id" :item="item" />
          </div>
          <EmptyState v-else-if="!loading" description="还没有收藏物品">
            <el-button type="primary" round @click="router.push('/search')">去逛逛</el-button>
          </EmptyState>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
  padding: 28px 30px;
}

.profile-main {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 18px;
  min-width: 280px;
}

.avatar {
  display: grid;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #d9a860, #b57a25);
  box-shadow: 0 10px 24px rgba(181, 122, 37, 0.24);
  color: #fff;
  font-size: 30px;
  font-weight: 600;
}

.name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.name-row h1 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 1px;
}

.username {
  margin: 5px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.school {
  margin: 4px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.metrics {
  display: flex;
  gap: 26px;
  padding: 0 12px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.metric strong {
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tabs {
  margin-top: 22px;
}

.panel {
  padding: 26px 28px;
}

.credit-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.reason {
  font-size: 14px;
}

.order {
  color: var(--sgj-muted);
  font-size: 12px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  padding: 20px 22px;
}

.review-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.review-head strong {
  font-size: 14px;
}

.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.review-content {
  margin: 12px 0 0;
  color: #4b564f;
  font-size: 13px;
  line-height: 1.8;
}

.review-foot {
  margin: 10px 0 0;
  color: var(--sgj-muted);
  font-size: 11px;
}

@media (max-width: 860px) {
  .profile-card {
    padding: 22px;
  }

  .metrics {
    width: 100%;
    justify-content: space-between;
    padding: 0;
  }
}
</style>
