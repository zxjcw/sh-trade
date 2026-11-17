<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCloseFilled } from '@element-plus/icons-vue'
import { tradeApi } from '@/api/trade'
import { useAuthStore } from '@/stores/auth'
import type { Order } from '@/types'
import {
  afterSaleLabels,
  conditionLabels,
  deliveryMethodLabels
} from '@/utils/status'
import { formatMoney, formatTime } from '@/utils/format'
import { canApplyAftersale } from '@/utils/order'
import CoverImage from '@/components/CoverImage.vue'
import StatusTag from '@/components/StatusTag.vue'
import CountdownText from '@/components/CountdownText.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref<Order | null>(null)
const loading = ref(true)

const isBuyer = computed(() => order.value?.buyerId === auth.user?.id)
const isSeller = computed(() => order.value?.sellerId === auth.user?.id)

const activeStep = computed(() => {
  switch (order.value?.tradeStatus) {
    case 'pending_payment':
      return 1
    case 'pending_delivery':
      return 2
    case 'pending_confirm':
      return 3
    case 'completed':
      return 4
    default:
      return 0
  }
})

const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({ rating: 5, tags: [] as string[], content: '' })

async function load() {
  loading.value = true
  try {
    order.value = await tradeApi.order(String(route.params.id))
    await auth.fetchMe()
  } catch (error) {
    ElMessage.error((error as Error).message)
    router.replace('/orders')
  } finally {
    loading.value = false
  }
}

async function pay() {
  if (!order.value) return
  try {
    await tradeApi.pay(order.value.id)
    await auth.fetchMe()
    ElMessage.success('支付成功')
    await load()
  } catch (error) {
    const message = (error as Error).message
    ElMessage.error(message)
    if (message.includes('余额不足')) router.push('/wallet')
  }
}

async function cancel() {
  if (!order.value) return
  await ElMessageBox.confirm('取消后物品会重新上架，确定取消吗？', '取消订单', {
    confirmButtonText: '取消订单',
    cancelButtonText: '再想想',
    type: 'warning'
  })
  await tradeApi.cancel(order.value.id)
  ElMessage.success('订单已取消')
  await load()
}

async function confirmReceipt() {
  if (!order.value) return
  await ElMessageBox.confirm('确认后款项将放款给卖家，且不可撤销。', '确认收货', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await tradeApi.confirm(order.value.id)
  await auth.fetchMe()
  ElMessage.success('交易完成')
  await load()
}

async function submitReview() {
  if (!order.value) return
  reviewLoading.value = true
  try {
    await tradeApi.review(order.value.id, reviewForm)
    ElMessage.success('评价已提交')
    reviewVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    reviewLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page page-narrow">
    <div v-loading="loading">
      <template v-if="order">
        <div class="head">
          <el-button link @click="router.push('/orders')">
            <el-icon><ArrowLeft /></el-icon>
            返回订单
          </el-button>
          <div class="head-right">
            <span class="order-no">{{ order.id }}</span>
            <StatusTag kind="trade" :status="order.tradeStatus" />
            <el-tag v-if="order.afterSaleStatus !== 'none'" type="danger" effect="plain" round size="small">
              {{ afterSaleLabels[order.afterSaleStatus] }}
            </el-tag>
          </div>
        </div>

        <section class="surface status-panel">
          <div v-if="order.tradeStatus === 'cancelled'" class="cancelled">
            <el-icon><CircleCloseFilled /></el-icon>
            <div>
              <strong>订单已取消</strong>
              <p>超时未支付或买家主动取消，物品已重新上架。</p>
            </div>
          </div>
          <el-steps v-else :active="activeStep" align-center finish-status="success">
            <el-step title="待支付" :description="order.tradeStatus === 'pending_payment' ? '请完成支付' : ''" />
            <el-step title="待交付" description="卖家交付 / 送至自提点" />
            <el-step title="待确认" description="买家确认收货" />
            <el-step title="已完成" :description="order.tradeStatus === 'completed' ? '交易完成' : ''" />
          </el-steps>
          <p v-if="order.tradeStatus === 'pending_payment'" class="deadline">
            支付剩余时间 <CountdownText :deadline="order.payDeadline" />
            <span>超时未支付将自动取消并释放物品</span>
          </p>
          <p v-if="order.tradeStatus === 'pending_confirm'" class="deadline">
            自动确认剩余时间 <CountdownText :deadline="order.confirmDeadline" />
          </p>
        </section>

        <div class="grid">
          <section class="surface panel">
            <h2>订单信息</h2>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="物品">
                <router-link class="item-link" :to="`/items/${order.itemId}`">{{ order.itemTitle }}</router-link>
              </el-descriptions-item>
              <el-descriptions-item label="成色">{{ conditionLabels[order.itemCondition] }}</el-descriptions-item>
              <el-descriptions-item label="订单金额">
                <strong class="amount">{{ formatMoney(order.amount) }}</strong>
              </el-descriptions-item>
              <el-descriptions-item label="交付方式">
                {{ deliveryMethodLabels[order.deliveryMethod] }}
                <span v-if="order.pickupPointName"> · {{ order.pickupPointName }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="买家">{{ order.buyerName }}</el-descriptions-item>
              <el-descriptions-item label="卖家">{{ order.sellerName }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(order.createdAt) }}</el-descriptions-item>
              <el-descriptions-item v-if="order.paidAt" label="支付时间">{{ formatTime(order.paidAt) }}</el-descriptions-item>
              <el-descriptions-item v-if="order.completedAt" label="完成时间">
                {{ formatTime(order.completedAt) }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="actions">
              <template v-if="isBuyer">
                <el-button v-if="order.tradeStatus === 'pending_payment'" type="primary" @click="pay">去支付</el-button>
                <el-button v-if="order.tradeStatus === 'pending_payment'" @click="cancel">取消订单</el-button>
                <el-button v-if="order.tradeStatus === 'pending_confirm'" type="primary" @click="confirmReceipt">
                  确认收货
                </el-button>
                <el-button
                  v-if="order.tradeStatus === 'completed' && !order.reviewed"
                  type="warning"
                  plain
                  @click="reviewVisible = true"
                >
                  评价
                </el-button>
                <el-button
                  v-if="canApplyAftersale(order)"
                  @click="router.push(`/aftersale/apply/${order.id}`)"
                >
                  申请售后
                </el-button>
              </template>
              <el-button
                v-if="isSeller && order.tradeStatus === 'completed' && !order.reviewed"
                type="warning"
                plain
                @click="reviewVisible = true"
              >
                评价买家
              </el-button>
            </div>
          </section>

          <aside class="aside">
            <section class="surface item-card">
              <div class="thumb">
                <CoverImage :seed="order.itemCoverSeed" :title="order.itemTitle" />
              </div>
              <div class="item-info">
                <strong>{{ order.itemTitle }}</strong>
                <span>{{ formatMoney(order.amount) }}</span>
                <router-link :to="`/items/${order.itemId}`">查看物品 →</router-link>
              </div>
            </section>

            <section v-if="order.delivery" class="surface panel">
              <h2>交付记录</h2>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item v-if="order.delivery.agreeTime" label="约定时间">
                  {{ formatTime(order.delivery.agreeTime) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.delivery.meetupPlace" label="面交地点">
                  {{ order.delivery.meetupPlace }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.delivery.verifyTime" label="核验时间">
                  {{ formatTime(order.delivery.verifyTime) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.delivery.inboundTime" label="入库时间">
                  {{ formatTime(order.delivery.inboundTime) }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.delivery.pickupCode" label="取件码">
                  {{ order.delivery.pickupCode }}
                </el-descriptions-item>
                <el-descriptions-item v-if="order.delivery.outboundTime" label="出库时间">
                  {{ formatTime(order.delivery.outboundTime) }}
                </el-descriptions-item>
              </el-descriptions>
              <p v-if="!order.delivery" class="muted">暂无交付记录</p>
            </section>

            <el-alert
              v-if="order.afterSaleStatus !== 'none'"
              type="warning"
              :closable="false"
              show-icon
              :title="`售后状态：${afterSaleLabels[order.afterSaleStatus]}`"
            >
              <template #default>
                <p class="alert-text">
                  {{ order.afterSaleStatus === 'arbitrating' ? '运营正在处理本次售后争议。' : '请关注订单状态变化。' }}
                </p>
              </template>
            </el-alert>
          </aside>
        </div>
      </template>
    </div>

    <el-dialog v-model="reviewVisible" title="评价这笔交易" width="480px">
      <el-form label-position="top">
        <el-form-item label="评分"><el-rate v-model="reviewForm.rating" show-score /></el-form-item>
        <el-form-item label="标签">
          <el-checkbox-group v-model="reviewForm.tags">
            <el-checkbox v-for="tag in ['描述相符', '成色好', '沟通顺畅', '交付准时']" :key="tag" :value="tag">
              {{ tag }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" />
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
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-no {
  color: var(--sgj-muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.status-panel {
  padding: 26px 24px;
  margin-bottom: 20px;
}

.cancelled {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #8d9490;
}

.cancelled .el-icon {
  font-size: 38px;
}

.cancelled strong {
  font-size: 16px;
}

.cancelled p {
  margin: 4px 0 0;
  font-size: 12px;
}

.deadline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 20px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
  align-items: start;
}

.panel {
  padding: 24px 26px;
}

.panel h2 {
  margin: 0 0 16px;
  font-size: 17px;
}

.item-link {
  color: var(--sgj-primary);
}

.amount {
  color: var(--sgj-accent-deep);
  font-size: 18px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.item-card {
  display: flex;
  gap: 12px;
  padding: 14px;
}

.thumb {
  width: 88px;
  height: 66px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}

.item-info strong {
  font-size: 14px;
}

.item-info span {
  color: var(--sgj-accent-deep);
  font-weight: 600;
}

.item-info a {
  color: var(--sgj-primary);
}

.alert-text {
  margin: 4px 0 0;
  font-size: 12px;
}

@media (max-width: 820px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 20px;
  }
}
</style>
