<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { PaymentRecord } from '@/types'
import { formatMoney, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const auth = useAuthStore()
const loading = ref(true)
const recharging = ref(false)
const amount = ref(100)
const records = ref<PaymentRecord[]>([])

const balance = computed(() => auth.user?.balance ?? 0)

const typeLabels: Record<PaymentRecord['type'], string> = {
  recharge: '充值',
  payment: '支付',
  payout: '放款',
  refund: '退款',
  chargeback: '扣回'
}

const typeColors: Record<PaymentRecord['type'], string> = {
  recharge: 'success',
  payment: 'info',
  payout: 'success',
  refund: 'success',
  chargeback: 'danger'
}

function typeTag(row: PaymentRecord) {
  return typeColors[row.type] as 'success' | 'info' | 'danger'
}

function typeLabel(row: PaymentRecord) {
  return typeLabels[row.type]
}

function isOut(row: PaymentRecord) {
  return row.type === 'payment' || row.type === 'chargeback'
}

async function load() {
  loading.value = true
  try {
    await auth.fetchMe()
    records.value = await userApi.payments()
  } finally {
    loading.value = false
  }
}

async function recharge() {
  if (!(amount.value > 0)) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (amount.value > 5000) {
    ElMessage.warning('单次充值不能超过 5000 元')
    return
  }
  recharging.value = true
  try {
    await userApi.recharge(amount.value)
    await load()
    ElMessage.success('充值成功（模拟支付）')
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
    <PageHeader
      eyebrow="WALLET"
      title="我的钱包"
      subtitle="充值用于平台内交易；资金支付后进入平台托管，确认收货后放款给卖家。"
    />

    <div class="wallet-grid">
      <section class="balance-card">
        <p class="label">可用余额</p>
        <p class="balance">{{ formatMoney(balance) }}</p>
        <p class="formula">余额 = 充值 + 退款 + 放款 − 支付 − 扣回</p>
        <div class="balance-foot">
          <span>充值仅用于模拟交易，不可提现</span>
          <router-link to="/orders">查看订单 →</router-link>
        </div>
      </section>

      <section class="surface recharge-card">
        <h2>充值</h2>
        <p class="hint">单次上限 ¥5000，充值记录只增不改，外部单号唯一。</p>
        <div class="quick">
          <button
            v-for="value in [50, 100, 200, 500, 1000]"
            :key="value"
            type="button"
            class="quick-btn"
            :class="{ active: amount === value }"
            @click="amount = value"
          >
            ¥{{ value }}
          </button>
        </div>
        <el-input v-model.number="amount" size="large" type="number" class="amount-input">
          <template #prefix>¥</template>
          <template #append>
            <el-button type="primary" :loading="recharging" @click="recharge">充值</el-button>
          </template>
        </el-input>
        <p class="note">演示环境为模拟支付，点击充值后余额立即到账。</p>
      </section>
    </div>

    <section class="surface panel">
      <div class="panel-head">
        <h2>资金流水</h2>
        <span class="muted">当前用户账户流水</span>
      </div>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row)" size="small" effect="light" round>
              {{ typeLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span :class="['amount', isOut(row) ? 'out' : 'in']">
              {{ isOut(row) ? '-' : '+' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额快照" width="130">
          <template #default="{ row }">{{ formatMoney(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column prop="externalNo" label="外部单号" min-width="170" />
        <el-table-column prop="remark" label="备注" min-width="110" />
      </el-table>
      <EmptyState v-if="!loading && !records.length" description="还没有资金流水" />
    </section>
  </div>
</template>

<style scoped>
.wallet-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
  margin-bottom: 20px;
}

.balance-card {
  display: flex;
  flex-direction: column;
  padding: 30px 32px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 82% 12%, rgba(232, 217, 184, 0.22), transparent 36%),
    linear-gradient(145deg, #1f3b2c, #2f6b4f 68%, #447e5f);
  box-shadow: 0 20px 40px rgba(31, 59, 44, 0.16);
  color: #f6f1e4;
}

.label {
  margin: 0;
  color: rgba(246, 241, 228, 0.62);
  font-size: 12px;
  letter-spacing: 2px;
}

.balance {
  margin: 10px 0 6px;
  font-size: 46px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.formula {
  margin: 0;
  color: rgba(246, 241, 228, 0.55);
  font-size: 11px;
}

.balance-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 26px;
  color: rgba(246, 241, 228, 0.6);
  font-size: 11px;
}

.balance-foot a {
  color: #e8d9b8;
}

.recharge-card {
  padding: 26px 28px;
}

.recharge-card h2 {
  margin: 0 0 6px;
  font-size: 18px;
}

.hint {
  margin: 0 0 16px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.quick-btn {
  padding: 7px 14px;
  border: 1px solid var(--sgj-line);
  border-radius: 9px;
  background: #fff;
  color: var(--sgj-ink);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;
}

.quick-btn:hover,
.quick-btn.active {
  border-color: var(--sgj-primary);
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
}

.amount-input {
  width: 100%;
}

.note {
  margin: 12px 0 0;
  color: #a0a6a1;
  font-size: 11px;
}

.panel {
  padding: 24px 26px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
}

.panel-head .muted {
  font-size: 12px;
}

.amount {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.amount.in {
  color: #3f8f5f;
}

.amount.out {
  color: #bf5b4b;
}

@media (max-width: 820px) {
  .wallet-grid {
    grid-template-columns: 1fr;
  }

  .balance {
    font-size: 38px;
  }
}
</style>
