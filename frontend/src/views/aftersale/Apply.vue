<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { tradeApi } from '@/api/trade'
import type { Order } from '@/types'
import { deliveryMethodLabels, tradeStatusLabels } from '@/utils/status'
import { formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const order = ref<Order | null>(null)
const loading = ref(true)
const submitting = ref(false)

const form = reactive({
  reason: '描述不符',
  evidence: ''
})

const reasons = ['描述不符', '商品损坏', '未收到物品', '卖家爽约', '其他']

const rules: FormRules = {
  reason: [{ required: true, message: '请选择退款原因', trigger: 'change' }],
  evidence: [{ required: true, message: '请填写举证说明', trigger: 'blur' }]
}

const routeHint = computed(() => {
  if (!order.value) return ''
  if (order.value.tradeStatus === 'pending_delivery') {
    return '当前订单还未交付，提交后可直接退款，托管资金原路退回买家钱包。'
  }
  if (order.value.afterSaleStatus !== 'none') {
    return '该订单已有售后处理中，不能重复提交。'
  }
  return '当前订单已交付，提交后进入售后流程。演示环境会直接转运营仲裁，由运营裁定退款或驳回。'
})

const canSubmit = computed(
  () =>
    Boolean(order.value) &&
    order.value?.tradeStatus !== 'cancelled' &&
    order.value?.afterSaleStatus === 'none'
)

async function load() {
  loading.value = true
  try {
    order.value = await tradeApi.order(String(route.params.orderId))
  } catch (error) {
    ElMessage.error((error as Error).message)
    router.replace('/orders')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!order.value) return
  await formRef.value?.validate()
  submitting.value = true
  try {
    await tradeApi.applyAftersale(order.value.id, {
      reason: form.reason,
      evidence: form.evidence
    })
    ElMessage.success(order.value.tradeStatus === 'pending_delivery' ? '退款成功，物品已重新上架' : '售后申请已提交')
    router.replace(`/orders/${order.value.id}`)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page page-narrow">
    <PageHeader
      eyebrow="AFTER-SALE"
      title="申请售后"
      subtitle="如实填写退款原因并提交举证。平台按交易状态区分处理：未交付直接退款，已交付转仲裁。"
    />

    <div v-loading="loading">
      <template v-if="order">
        <section class="surface order-summary">
          <div class="summary-head">
            <span>订单 {{ order.id }}</span>
            <StatusTag kind="trade" :status="order.tradeStatus" />
          </div>
          <div class="summary-body">
            <div>
              <strong>{{ order.itemTitle }}</strong>
              <p>
                {{ tradeStatusLabels[order.tradeStatus] }} ·
                {{ deliveryMethodLabels[order.deliveryMethod] }} ·
                金额 {{ formatMoney(order.amount) }}
              </p>
            </div>
            <span class="seller">卖家 {{ order.sellerName }}</span>
          </div>
        </section>

        <el-alert
          class="hint"
          :type="order.tradeStatus === 'pending_delivery' ? 'success' : 'warning'"
          :closable="false"
          show-icon
          :title="routeHint"
        />

        <section class="surface form-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="退款原因" prop="reason">
              <el-select v-model="form.reason" size="large" class="full" :disabled="!canSubmit">
                <el-option v-for="reason in reasons" :key="reason" :label="reason" :value="reason" />
              </el-select>
            </el-form-item>
            <el-form-item label="举证说明" prop="evidence">
              <el-input
                v-model="form.evidence"
                type="textarea"
                :rows="5"
                maxlength="500"
                show-word-limit
                :disabled="!canSubmit"
                placeholder="描述问题、沟通情况，以及物品与描述不符的具体表现"
              />
            </el-form-item>
            <div class="rules">
              <p><strong>处理规则</strong></p>
              <ul>
                <li>待交付（未发货）：直接退款，平台托管资金退回买家。</li>
                <li>待确认 / 已完成：卖家 2 分钟内响应；拒绝或超时转运营仲裁。</li>
                <li>已完成订单售后窗口为收货后 5 分钟，超时不再受理。</li>
                <li>售后成立会回滚履历、评价与信用记录。</li>
              </ul>
            </div>
            <div class="actions">
              <el-button size="large" @click="router.back()">取消</el-button>
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                :disabled="!canSubmit"
                @click="submit"
              >
                提交售后申请
              </el-button>
            </div>
          </el-form>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.order-summary {
  padding: 20px 24px;
  margin-bottom: 16px;
}

.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.summary-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.summary-body strong {
  font-size: 16px;
}

.summary-body p {
  margin: 6px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.seller {
  color: var(--sgj-muted);
  font-size: 12px;
}

.hint {
  margin-bottom: 16px;
}

.form-card {
  padding: 26px 28px;
}

.full {
  width: 100%;
}

.rules {
  padding: 14px 18px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-paper-deep);
  color: var(--sgj-muted);
  font-size: 11px;
  line-height: 1.9;
}

.rules strong {
  color: var(--sgj-ink);
}

.rules ul {
  margin: 6px 0 0;
  padding-left: 18px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 640px) {
  .form-card {
    padding: 20px;
  }

  .summary-body {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
