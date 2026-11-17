<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/api/trade'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { Order } from '@/types'
import {
  afterSaleLabels,
  conditionLabels,
  deliveryMethodLabels
} from '@/utils/status'
import { formatMoney, formatTime } from '@/utils/format'
import { canApplyAftersale } from '@/utils/order'
import PageHeader from '@/components/PageHeader.vue'
import CoverImage from '@/components/CoverImage.vue'
import StatusTag from '@/components/StatusTag.vue'
import CountdownText from '@/components/CountdownText.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()

const role = ref<'buyer' | 'seller'>('buyer')
const status = ref('all')
const orders = ref<Order[]>([])
const loading = ref(false)

const tabs = [
  { label: '全部', value: 'all' },
  { label: '待支付', value: 'pending_payment' },
  { label: '待交付', value: 'pending_delivery' },
  { label: '待确认', value: 'pending_confirm' },
  { label: '已完成', value: 'completed' }
]

const rechargeVisible = ref(false)
const rechargeAmount = ref(0)
const recharging = ref(false)
const pendingPayOrder = ref<Order | null>(null)

const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewOrder = ref<Order | null>(null)
const reviewForm = reactive({
  rating: 5,
  tags: [] as string[],
  content: ''
})
const reviewTags = ['描述相符', '成色好', '沟通顺畅', '交付准时', '包装用心']

async function load() {
  loading.value = true
  try {
    orders.value = await tradeApi.orders({
      role: role.value,
      status: status.value === 'all' ? undefined : status.value
    })
  } finally {
    loading.value = false
  }
}

async function switchRole(value: string | number | boolean) {
  role.value = value === 'seller' ? 'seller' : 'buyer'
  await load()
}

async function pay(order: Order) {
  try {
    await tradeApi.pay(order.id)
    await auth.fetchMe()
    ElMessage.success('支付成功')
    await load()
  } catch (error) {
    const message = (error as Error).message
    if (message.includes('余额不足')) {
      pendingPayOrder.value = order
      rechargeAmount.value = Math.ceil(order.amount - (auth.user?.balance ?? 0))
      rechargeVisible.value = true
    } else {
      ElMessage.error(message)
    }
  }
}

async function rechargeAndPay() {
  if (!pendingPayOrder.value) return
  recharging.value = true
  try {
    await userApi.recharge(rechargeAmount.value)
    await auth.fetchMe()
    await tradeApi.pay(pendingPayOrder.value.id)
    await auth.fetchMe()
    ElMessage.success('充值并支付成功')
    rechargeVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    recharging.value = false
  }
}

async function cancel(order: Order) {
  await ElMessageBox.confirm('取消后物品会重新上架，确定取消订单吗？', '取消订单', {
    confirmButtonText: '取消订单',
    cancelButtonText: '再想想',
    type: 'warning'
  })
  await tradeApi.cancel(order.id)
  ElMessage.success('订单已取消')
  await load()
}

async function confirmReceipt(order: Order) {
  await ElMessageBox.confirm(
    `确认已收到「${order.itemTitle}」吗？确认后款项将放款给卖家。`,
    '确认收货',
    { confirmButtonText: '确认收货', cancelButtonText: '取消', type: 'warning' }
  )
  await tradeApi.confirm(order.id)
  await auth.fetchMe()
  ElMessage.success('已确认收货，交易完成')
  await load()
}

function openReview(order: Order) {
  reviewOrder.value = order
  reviewForm.rating = 5
  reviewForm.tags = []
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewOrder.value) return
  reviewLoading.value = true
  try {
    await tradeApi.review(reviewOrder.value.id, {
      rating: reviewForm.rating,
      tags: reviewForm.tags,
      content: reviewForm.content
    })
    ElMessage.success('评价已提交')
    reviewVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    reviewLoading.value = false
  }
}

function goAftersale(order: Order) {
  router.push(`/aftersale/apply/${order.id}`)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="MY ORDERS"
      title="我的订单"
      subtitle="交易状态与售后状态分开记录；资金在确认收货后放款，未发货可直接退款。"
    >
      <template #actions>
        <el-button round @click="router.push('/my-items')">我的物品</el-button>
      </template>
    </PageHeader>

    <div class="toolbar">
      <el-radio-group :model-value="role" @change="switchRole">
        <el-radio-button value="buyer">我买到的</el-radio-button>
        <el-radio-button value="seller">我卖出的</el-radio-button>
      </el-radio-group>
      <el-tabs v-model="status" class="status-tabs" @tab-change="load">
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card surface">
        <header class="order-head">
          <span class="order-no">订单号 {{ order.id }}</span>
          <span class="order-time">{{ formatTime(order.createdAt) }}</span>
          <div class="order-tags">
            <el-tag v-if="order.afterSaleStatus !== 'none'" type="danger" effect="plain" size="small" round>
              {{ afterSaleLabels[order.afterSaleStatus] }}
            </el-tag>
            <StatusTag kind="trade" :status="order.tradeStatus" />
          </div>
        </header>

        <div class="order-body">
          <div class="thumb">
            <CoverImage :seed="order.itemCoverSeed" :title="order.itemTitle" />
          </div>
          <div class="order-info">
            <router-link class="item-title" :to="`/items/${order.itemId}`">{{ order.itemTitle }}</router-link>
            <p class="meta">
              成色 {{ conditionLabels[order.itemCondition] }}
              <span>·</span>
              {{ deliveryMethodLabels[order.deliveryMethod] }}
              <span v-if="order.pickupPointName">· {{ order.pickupPointName }}</span>
            </p>
            <p class="meta">
              {{ role === 'buyer' ? `卖家 ${order.sellerName}` : `买家 ${order.buyerName}` }}
              <span>·</span>
              订单金额 <strong>{{ formatMoney(order.amount) }}</strong>
            </p>
            <p v-if="order.tradeStatus === 'pending_payment'" class="countdown">
              支付倒计时 <CountdownText :deadline="order.payDeadline" />
            </p>
            <p v-else-if="order.tradeStatus === 'pending_confirm'" class="countdown">
              自动确认倒计时 <CountdownText :deadline="order.confirmDeadline" />
            </p>
          </div>
          <div class="order-actions">
            <el-button size="small" @click="router.push(`/orders/${order.id}`)">订单详情</el-button>

            <template v-if="role === 'buyer'">
              <el-button
                v-if="order.tradeStatus === 'pending_payment'"
                size="small"
                type="primary"
                @click="pay(order)"
              >
                去支付
              </el-button>
              <el-button
                v-if="order.tradeStatus === 'pending_payment'"
                size="small"
                plain
                @click="cancel(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.tradeStatus === 'pending_confirm'"
                size="small"
                type="primary"
                @click="confirmReceipt(order)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="order.tradeStatus === 'completed' && !order.reviewed"
                size="small"
                type="warning"
                plain
                @click="openReview(order)"
              >
                评价
              </el-button>
              <el-button
                v-if="canApplyAftersale(order)"
                size="small"
                plain
                @click="goAftersale(order)"
              >
                申请售后
              </el-button>
            </template>

            <el-button
              v-if="role === 'seller' && order.tradeStatus === 'completed' && !order.reviewed"
              size="small"
              type="warning"
              plain
              @click="openReview(order)"
            >
              评价买家
            </el-button>
          </div>
        </div>
      </article>

      <EmptyState v-if="!loading && !orders.length" description="当前分类下还没有订单">
        <el-button type="primary" round @click="router.push('/search')">去逛逛</el-button>
      </EmptyState>
    </div>

    <el-dialog v-model="rechargeVisible" title="余额不足" width="420px">
      <p class="dialog-tip">
        订单金额 {{ formatMoney(pendingPayOrder?.amount ?? 0) }}，当前余额 {{ formatMoney(auth.user?.balance ?? 0) }}。
      </p>
      <el-input v-model.number="rechargeAmount" type="number" size="large">
        <template #prefix>¥</template>
      </el-input>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="rechargeAndPay">充值并支付</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价这笔交易" width="480px">
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-score />
        </el-form-item>
        <el-form-item label="标签">
          <el-checkbox-group v-model="reviewForm.tags">
            <el-checkbox v-for="tag in reviewTags" :key="tag" :value="tag">{{ tag }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="说说这次交易体验" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.status-tabs {
  flex: 1;
  min-width: 320px;
}

.status-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 220px;
}

.order-card {
  padding: 0;
  overflow: hidden;
}

.order-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--sgj-line);
  background: #faf8f3;
  color: var(--sgj-muted);
  font-size: 12px;
}

.order-no {
  font-variant-numeric: tabular-nums;
}

.order-time {
  color: #a3aaa5;
}

.order-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.order-body {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
}

.thumb {
  width: 110px;
  height: 82px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px;
}

.order-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.item-title {
  color: var(--sgj-ink);
  font-size: 15px;
  font-weight: 600;
}

.item-title:hover {
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

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.order-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 108px;
}

.order-actions .el-button + .el-button {
  margin-left: 0;
}

.dialog-tip {
  margin: 0 0 14px;
  color: var(--sgj-muted);
  font-size: 13px;
}

@media (max-width: 760px) {
  .order-body {
    flex-wrap: wrap;
  }

  .thumb {
    width: 100%;
    height: 150px;
  }

  .order-actions {
    flex-direction: row;
    flex-wrap: wrap;
    width: 100%;
  }
}
</style>
