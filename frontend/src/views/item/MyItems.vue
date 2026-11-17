<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/item'
import { tradeApi } from '@/api/trade'
import type { Item, ItemCondition, Order } from '@/types'
import { conditionOptions, conditionLabels } from '@/utils/status'
import { formatDate, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import ItemCard from '@/components/ItemCard.vue'
import CoverImage from '@/components/CoverImage.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const activeTab = ref('published')
const loading = ref(true)
const published = ref<Item[]>([])
const bought = ref<{ order: Order; item: Item | null }[]>([])

const relistVisible = ref(false)
const relistLoading = ref(false)
const relistTarget = ref<{ order: Order; item: Item } | null>(null)
const relistForm = reactive({
  price: 0,
  condition: 'good' as ItemCondition,
  description: ''
})

async function load() {
  loading.value = true
  try {
    const [items, orders] = await Promise.all([
      itemApi.list({ scope: 'mine' }),
      tradeApi.orders({ role: 'buyer', status: 'completed' })
    ])
    const boughtIds = new Set(orders.map((order) => order.itemId))
    published.value = items.filter((item) => !boughtIds.has(item.id))
    const pairs = await Promise.all(
      orders.map(async (order) => {
        try {
          const detail = await itemApi.detail(order.itemId)
          return { order, item: detail }
        } catch {
          return { order, item: null }
        }
      })
    )
    bought.value = pairs
  } finally {
    loading.value = false
  }
}

function openRelist(order: Order, item: Item | null) {
  if (!item) return
  relistTarget.value = { order, item }
  relistForm.price = item.price
  relistForm.condition = item.condition
  relistForm.description = item.description
  relistVisible.value = true
}

async function submitRelist() {
  if (!relistTarget.value) return
  relistLoading.value = true
  try {
    await itemApi.relist(relistTarget.value.item.id, {
      price: Number(relistForm.price),
      condition: relistForm.condition,
      description: relistForm.description
    })
    ElMessage.success('已提交再次上架，等待审核')
    relistVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    relistLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="MY ITEMS"
      title="我的物品"
      subtitle="我发布的物品、我买到且可以再次上架的物品。物品编号终身不变，履历随编号延续。"
    >
      <template #actions>
        <el-button type="primary" round @click="router.push('/publish')">发布物品</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="我发布的" name="published">
        <div v-loading="loading">
          <div v-if="published.length" class="grid-cards">
            <ItemCard v-for="item in published" :key="item.id" :item="item" />
          </div>
          <EmptyState v-else-if="!loading" description="还没有发布过物品">
            <el-button type="primary" round @click="router.push('/publish')">去发布</el-button>
          </EmptyState>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我买到的" name="bought">
        <div v-loading="loading" class="bought-list">
          <div v-for="pair in bought" :key="pair.order.id" class="bought-card surface">
            <div class="thumb">
              <CoverImage
                :seed="pair.item?.coverSeed ?? pair.order.itemCoverSeed"
                :title="pair.order.itemTitle"
              />
            </div>
            <div class="bought-info">
              <router-link class="title" :to="`/items/${pair.order.itemId}`">
                {{ pair.order.itemTitle }}
              </router-link>
              <p class="meta">
                成交价 <strong>{{ formatMoney(pair.order.amount) }}</strong>
                <span>·</span>
                卖家 {{ pair.order.sellerName }}
                <span>·</span>
                {{ formatDate(pair.order.completedAt) }} 完成
              </p>
              <p class="meta">
                成色 {{ conditionLabels[pair.order.itemCondition] }} · 订单号 {{ pair.order.id }}
              </p>
            </div>
            <div class="bought-actions">
              <el-button size="small" @click="router.push(`/orders/${pair.order.id}`)">查看订单</el-button>
              <el-button
                v-if="pair.item?.status === 'completed'"
                size="small"
                type="primary"
                plain
                @click="openRelist(pair.order, pair.item)"
              >
                再次上架
              </el-button>
              <el-tag v-else-if="pair.item" size="small" type="info">{{ pair.item?.status === 'pending' ? '审核中' : '已流转' }}</el-tag>
            </div>
          </div>
          <EmptyState v-if="!loading && !bought.length" description="还没有买到过物品" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="relistVisible" title="再次上架" width="520px">
      <p class="dialog-tip">
        复用原物品编号 #{{ relistTarget?.item.id }}，旧图片会转为历史记录。
      </p>
      <el-form label-position="top">
        <el-form-item label="新售价（元）">
          <el-input v-model.number="relistForm.price" type="number" />
        </el-form-item>
        <el-form-item label="当前成色">
          <el-select v-model="relistForm.condition" class="full">
            <el-option v-for="option in conditionOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="relistForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relistVisible = false">取消</el-button>
        <el-button type="primary" :loading="relistLoading" @click="submitRelist">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tabs {
  --el-tabs-header-height: 46px;
}

.bought-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 140px;
}

.bought-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px;
}

.thumb {
  width: 120px;
  height: 90px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.bought-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.title {
  color: var(--sgj-ink);
  font-size: 15px;
  font-weight: 600;
}

.title:hover {
  color: var(--sgj-primary);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.meta strong {
  color: var(--sgj-accent-deep);
}

.bought-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-tip {
  margin: 0 0 16px;
  padding: 10px 12px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-brass-soft);
  color: #7b633f;
  font-size: 11px;
  line-height: 1.8;
}

.full {
  width: 100%;
}

@media (max-width: 640px) {
  .bought-card {
    flex-wrap: wrap;
  }

  .thumb {
    width: 100%;
    height: 140px;
  }

  .bought-actions {
    flex-direction: row;
    width: 100%;
  }
}
</style>
