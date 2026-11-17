<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Star, StarFilled } from '@element-plus/icons-vue'
import { itemApi } from '@/api/item'
import { useAuthStore } from '@/stores/auth'
import type { Item } from '@/types'
import { conditionLabels, notesLabels, sourceLabels } from '@/utils/status'
import { formatDate } from '@/utils/format'
import CoverImage from '@/components/CoverImage.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const item = ref<Item | null>(null)
const loading = ref(true)
const notFound = ref(false)
const activeImage = ref(0)

const historyLabels: Record<string, string> = {
  created: '建档',
  deal: '成交',
  exchange: '置换',
  return: '退货',
  repair: '维修'
}

const isOwner = computed(() => item.value?.sellerId === auth.user?.id)
const canBuy = computed(() => item.value?.status === 'on_sale' && !isOwner.value)

const gallery = computed(() => {
  if (!item.value) return []
  return item.value.images.filter((image) => image.status === 'active').slice(0, 3)
})

async function load() {
  loading.value = true
  try {
    item.value = await itemApi.detail(Number(route.params.id))
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

function ensureLogin(action: string) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return false
  }
  if (!auth.isVerified) {
    ElMessage.warning('请先完成学籍认证')
    router.push({ name: 'verify', query: { redirect: route.fullPath } })
    return false
  }
  if (item.value && item.value.sellerId === auth.user?.id) {
    ElMessage.warning('这是你自己发布的物品')
    return false
  }
  return true
}

function buy() {
  if (!item.value) return
  if (!ensureLogin('buy')) return
  router.push(`/checkout/${item.value.id}`)
}

async function toggleFavorite() {
  if (!item.value) return
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const result = await itemApi.toggleFavorite(item.value.id)
  item.value.favorited = result.favorited
  ElMessage.success(result.favorited ? '已加入收藏' : '已取消收藏')
}

function goBounty() {
  if (!item.value) return
  router.push({ name: 'bounty-publish', query: { keyword: item.value.title } })
}

function goExchange() {
  if (!item.value) return
  router.push({ path: '/exchange', query: { targetItemId: String(item.value.id) } })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div v-if="notFound" class="not-found surface">
      <el-result icon="warning" title="物品不存在或已下架" sub-title="它可能已经被别人买走，去看看其他物品吧">
        <template #extra>
          <el-button type="primary" round @click="router.push('/search')">继续逛逛</el-button>
        </template>
      </el-result>
    </div>

    <div v-else v-loading="loading" class="detail">
      <template v-if="item">
        <nav class="breadcrumb">
          <router-link to="/">首页</router-link>
          <span>/</span>
          <router-link :to="{ name: 'search', query: { categoryId: String(item.categoryId) } }">
            {{ item.categoryName }}
          </router-link>
          <span>/</span>
          <span class="current">{{ item.title }}</span>
        </nav>

        <section class="main-card surface">
          <div class="gallery">
            <div class="main-image">
              <CoverImage
                :seed="gallery[activeImage]?.seed ?? item.coverSeed"
                :title="item.title"
              />
            </div>
            <div class="thumbs">
              <button
                v-for="(image, index) in gallery"
                :key="image.id"
                type="button"
                class="thumb"
                :class="{ active: activeImage === index }"
                @click="activeImage = index"
              >
                <CoverImage :seed="image.seed" :title="item.title" />
              </button>
            </div>
          </div>

          <div class="info">
            <div class="info-top">
              <StatusTag kind="item" :status="item.status" />
              <span class="category">{{ item.categoryName }}</span>
              <span class="time">上架于 {{ formatDate(item.createdAt) }}</span>
            </div>
            <h1>{{ item.title }}</h1>

            <div class="price-box">
              <span class="current-price">¥{{ item.price.toFixed(2) }}</span>
              <span class="origin-price">原价 ¥{{ item.originalPrice.toFixed(0) }}</span>
              <span v-if="item.purchasePrice" class="origin-price">
                入手价 ¥{{ item.purchasePrice.toFixed(0) }}
              </span>
            </div>

            <div class="attrs">
              <div class="attr">
                <span>成色</span>
                <strong>{{ conditionLabels[item.condition] }}</strong>
              </div>
              <div class="attr">
                <span>来源</span>
                <strong>{{ sourceLabels[item.source] }}</strong>
              </div>
              <div class="attr">
                <span>笔记资料</span>
                <strong>{{ notesLabels[item.notesTag] }}</strong>
              </div>
              <div class="attr">
                <span>物品编号</span>
                <strong>#{{ String(item.id).padStart(6, '0') }}</strong>
              </div>
            </div>

            <div class="seller">
              <span class="avatar">{{ item.sellerName.slice(0, 1) }}</span>
              <div class="seller-info">
                <strong>{{ item.sellerName }}</strong>
                <span>信用分 {{ item.sellerCredit }} · 已通过学籍认证</span>
              </div>
              <el-tag effect="plain" round size="small">卖家</el-tag>
            </div>

            <div class="actions">
              <el-button
                class="buy"
                type="primary"
                size="large"
                :disabled="!canBuy"
                @click="buy"
              >
                {{ item.status === 'on_sale' ? (isOwner ? '这是你的物品' : '立即购买') : '当前不可购买' }}
              </el-button>
              <el-button size="large" :type="item.favorited ? 'warning' : 'default'" plain @click="toggleFavorite">
                <el-icon><StarFilled v-if="item.favorited" /><Star v-else /></el-icon>
                {{ item.favorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button size="large" plain @click="goBounty">我要找同款</el-button>
              <el-button size="large" plain @click="goExchange">发起置换</el-button>
            </div>

            <p class="assurance">
              <el-icon><CircleCheck /></el-icon>
              平台托管交易：确认收货后才会放款给卖家，物品履历全程可查。
            </p>
          </div>
        </section>

        <div class="lower-grid">
          <section class="surface panel">
            <h2>物品描述</h2>
            <p class="description">{{ item.description || '卖家暂未填写详细描述。' }}</p>
            <div class="facts">
              <div><span>分类</span><strong>{{ item.categoryName }}</strong></div>
              <div><span>成色</span><strong>{{ conditionLabels[item.condition] }}</strong></div>
              <div><span>上架时间</span><strong>{{ formatDate(item.createdAt) }}</strong></div>
            </div>
          </section>

          <section class="surface panel">
            <h2>物品履历</h2>
            <p class="panel-hint">履历由系统写入，用户不可修改。物品编号终身不变。</p>
            <el-timeline class="timeline">
              <el-timeline-item
                v-for="event in [...item.history].reverse()"
                :key="event.id"
                :timestamp="formatDate(event.time)"
                placement="top"
              >
                <div class="event">
                  <el-tag size="small" effect="plain">{{ historyLabels[event.type] ?? event.type }}</el-tag>
                  <span>{{ event.description }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.breadcrumb a:hover {
  color: var(--sgj-primary);
}

.current {
  overflow: hidden;
  max-width: 320px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-card {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 34px;
  padding: 26px;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-image {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 14px;
}

.thumbs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.thumb {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 10px;
  background: none;
  cursor: pointer;
  opacity: 0.72;
  transition: all 0.16s ease;
}

.thumb.active {
  border-color: var(--sgj-primary);
  opacity: 1;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.category {
  color: var(--sgj-primary);
}

.time {
  margin-left: auto;
}

.info h1 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 0.5px;
  line-height: 1.4;
}

.price-box {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(120deg, #fbf5e8, #f7f2e6);
}

.current-price {
  color: var(--sgj-accent-deep);
  font-size: 32px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.origin-price {
  color: #a08f72;
  font-size: 12px;
  text-decoration: line-through;
}

.attrs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.attr {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid var(--sgj-line);
  border-radius: 10px;
  font-size: 12px;
}

.attr span {
  color: var(--sgj-muted);
}

.seller {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f8f6f0;
}

.avatar {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #d9a860, #b57a25);
  color: #fff;
  font-weight: 600;
}

.seller-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.seller-info strong {
  font-size: 14px;
}

.seller-info span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.buy {
  flex: 1;
  min-width: 150px;
}

.assurance {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.lower-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.panel {
  padding: 24px 26px;
}

.panel h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.panel-hint {
  margin: 0 0 16px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.description {
  margin: 0;
  color: #4b564f;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-line;
}

.facts {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--sgj-line);
}

.facts div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.facts span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.facts strong {
  font-size: 13px;
}

.timeline {
  padding-left: 4px;
}

.event {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4b564f;
  font-size: 13px;
}

.not-found {
  max-width: 640px;
  margin: 60px auto;
  padding: 20px;
}

@media (max-width: 900px) {
  .main-card {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .lower-grid {
    grid-template-columns: 1fr;
  }

  .info h1 {
    font-size: 22px;
  }
}
</style>
