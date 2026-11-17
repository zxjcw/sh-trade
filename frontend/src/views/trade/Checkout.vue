<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Lock } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/catalog'
import { itemApi } from '@/api/item'
import { tradeApi } from '@/api/trade'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { DeliveryMethod, Item, PickupPoint } from '@/types'
import { conditionLabels } from '@/utils/status'
import { formatMoney } from '@/utils/format'
import CoverImage from '@/components/CoverImage.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const item = ref<Item | null>(null)
const pickupPoints = ref<PickupPoint[]>([])
const loading = ref(true)
const submitting = ref(false)
const deliveryMethod = ref<DeliveryMethod>('meetup')
const pickupPointId = ref<number>()

const deliveryOptions: { value: DeliveryMethod; label: string; hint: string }[] = [
  { value: 'meetup', label: '当面交易', hint: '双方约定时间地点，见面扫码核验' },
  { value: 'pickup', label: '自提点自提', hint: '卖家送件到校内自提点，买家凭取件码取件' }
]

const rechargeVisible = ref(false)
const rechargeAmount = ref(0)
const recharging = ref(false)

const balance = computed(() => auth.user?.balance ?? 0)
const insufficient = computed(() => Boolean(item.value && balance.value < item.value.price))
const shortfall = computed(() => (item.value ? Math.max(0, item.value.price - balance.value) : 0))

async function load() {
  loading.value = true
  try {
    const [detail, points] = await Promise.all([
      itemApi.detail(Number(route.params.itemId)),
      catalogApi.pickupPoints()
    ])
    item.value = detail
    pickupPoints.value = points
    await auth.fetchMe()
  } catch (error) {
    ElMessage.error((error as Error).message)
    router.replace('/')
  } finally {
    loading.value = false
  }
}

async function placeOrder() {
  if (!item.value) return
  if (deliveryMethod.value === 'pickup' && !pickupPointId.value) {
    ElMessage.warning('请选择自提点')
    return
  }
  submitting.value = true
  try {
    const order = await tradeApi.create({
      itemId: item.value.id,
      deliveryMethod: deliveryMethod.value,
      pickupPointId: deliveryMethod.value === 'pickup' ? pickupPointId.value : undefined
    })
    const paid = await tradeApi.pay(order.id)
    await auth.fetchMe()
    ElMessage.success('支付成功，资金已进入平台托管')
    router.replace(`/orders/${paid.id}`)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

function submit() {
  if (insufficient.value) {
    rechargeAmount.value = Math.ceil(shortfall.value)
    rechargeVisible.value = true
    return
  }
  placeOrder()
}

async function recharge() {
  if (!(rechargeAmount.value > 0)) {
    ElMessage.warning('请输入有效金额')
    return
  }
  recharging.value = true
  try {
    await userApi.recharge(rechargeAmount.value)
    await auth.fetchMe()
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    await placeOrder()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    recharging.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page page-narrow">
    <div v-loading="loading">
      <template v-if="item">
        <div class="head">
          <el-button link @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h1>确认订单</h1>
        </div>

        <div class="checkout-grid">
          <section class="surface panel">
            <h2>交付方式</h2>
            <div class="delivery-options">
              <button
                v-for="option in deliveryOptions"
                :key="option.value"
                type="button"
                class="delivery-card"
                :class="{ active: deliveryMethod === option.value }"
                @click="deliveryMethod = option.value"
              >
                <strong>{{ option.label }}</strong>
                <span>{{ option.hint }}</span>
              </button>
            </div>

            <el-form v-if="deliveryMethod === 'pickup'" label-position="top" class="pickup-form">
              <el-form-item label="选择自提点" required>
                <el-select v-model="pickupPointId" size="large" placeholder="请选择自提点" class="full">
                  <el-option
                    v-for="point in pickupPoints"
                    :key="point.id"
                    :label="`${point.name}（${point.address}）`"
                    :value="point.id"
                  />
                </el-select>
              </el-form-item>
            </el-form>

            <div class="escrow">
              <el-icon><Lock /></el-icon>
              <div>
                <strong>资金托管</strong>
                <p>支付后款项进入平台托管账户，确认收货后才放款给卖家；未发货可申请直接退款。</p>
              </div>
            </div>
          </section>

          <aside class="surface summary">
            <h2>订单摘要</h2>
            <div class="item-row">
              <div class="thumb">
                <CoverImage :seed="item.coverSeed" :title="item.title" />
              </div>
              <div class="item-info">
                <strong>{{ item.title }}</strong>
                <span>成色 {{ conditionLabels[item.condition] }}</span>
                <span>卖家 {{ item.sellerName }}</span>
              </div>
            </div>

            <div class="line">
              <span>物品金额</span>
              <strong>{{ formatMoney(item.price) }}</strong>
            </div>
            <div class="line">
              <span>平台服务费</span>
              <strong>¥0.00</strong>
            </div>
            <div class="line total">
              <span>应付金额</span>
              <strong>{{ formatMoney(item.price) }}</strong>
            </div>

            <div class="balance" :class="{ low: insufficient }">
              <span>钱包余额</span>
              <strong>{{ formatMoney(balance) }}</strong>
            </div>
            <p v-if="insufficient" class="warn">
              余额不足，还需充值 {{ formatMoney(shortfall) }}
            </p>

            <el-button
              class="pay-btn"
              type="primary"
              size="large"
              :loading="submitting"
              :disabled="item.status !== 'on_sale'"
              @click="submit"
            >
              {{ insufficient ? '充值并支付' : '立即支付' }}
            </el-button>
            <router-link class="wallet-link" to="/wallet">去钱包看看 →</router-link>
          </aside>
        </div>
      </template>
    </div>

    <el-dialog v-model="rechargeVisible" title="余额不足，先充值" width="420px">
      <p class="recharge-tip">
        需支付 {{ formatMoney(item?.price ?? 0) }}，当前余额 {{ formatMoney(balance) }}，至少充值
        {{ formatMoney(shortfall) }}。
      </p>
      <el-form label-position="top">
        <el-form-item label="充值金额（元）">
          <el-input v-model.number="rechargeAmount" type="number" size="large">
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>
      </el-form>
      <div class="quick-amounts">
        <el-button v-for="amount in [50, 100, 200, 500]" :key="amount" size="small" @click="rechargeAmount = amount">
          ¥{{ amount }}
        </el-button>
      </div>
      <template #footer>
        <el-button @click="rechargeVisible = false">稍后再说</el-button>
        <el-button type="primary" :loading="recharging" @click="recharge">确认充值并支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.head h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
}

.checkout-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 20px;
  align-items: start;
}

.panel {
  padding: 26px 28px;
}

.panel h2,
.summary h2 {
  margin: 0 0 18px;
  font-size: 18px;
}

.delivery-options {
  display: grid;
  gap: 12px;
}

.delivery-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 16px 18px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-surface);
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.delivery-card:hover {
  border-color: var(--sgj-primary);
}

.delivery-card.active {
  border-color: var(--sgj-green);
  background: var(--sgj-green-soft);
  box-shadow: 0 0 0 1px var(--sgj-green) inset;
}

.delivery-card strong {
  font-size: 15px;
}

.delivery-card span {
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.6;
}

.pickup-form {
  margin-top: 16px;
}

.full {
  width: 100%;
}

.escrow {
  display: flex;
  gap: 12px;
  margin-top: 22px;
  padding: 14px 16px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-paper-deep);
  color: var(--sgj-green);
}

.escrow strong {
  font-size: 13px;
}

.escrow p {
  margin: 4px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.7;
}

.summary {
  position: sticky;
  top: 92px;
  padding: 26px 26px 22px;
}

.item-row {
  display: flex;
  gap: 12px;
  padding-bottom: 18px;
  border-bottom: 1px dashed var(--sgj-line);
}

.thumb {
  width: 84px;
  height: 64px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-info strong {
  font-size: 14px;
  line-height: 1.4;
}

.item-info span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  color: var(--sgj-muted);
  font-size: 13px;
}

.line strong {
  color: var(--sgj-ink);
}

.line.total {
  padding-top: 14px;
  border-top: 1px dashed var(--sgj-line);
  color: var(--sgj-ink);
  font-size: 14px;
}

.line.total strong {
  color: var(--sgj-brass);
  font-family: var(--sgj-font-display);
  font-size: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.balance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: 1px;
  background: var(--sgj-paper-deep);
  color: var(--sgj-muted);
  font-size: 11px;
}

.balance strong {
  color: var(--sgj-ink);
}

.balance.low strong {
  color: #bf5b4b;
}

.warn {
  margin: 8px 0 0;
  color: #bf5b4b;
  font-size: 12px;
}

.pay-btn {
  width: 100%;
  margin-top: 16px;
}

.wallet-link {
  display: block;
  margin-top: 12px;
  color: var(--sgj-primary);
  font-size: 12px;
  text-align: center;
}

.recharge-tip {
  margin: 0 0 16px;
  color: var(--sgj-muted);
  font-size: 13px;
  line-height: 1.7;
}

.quick-amounts {
  display: flex;
  gap: 8px;
}

.quick-amounts .el-button {
  flex: 1;
}

@media (max-width: 860px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
  }

  .panel {
    padding: 20px;
  }
}
</style>
