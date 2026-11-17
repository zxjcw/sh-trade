<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { catalogApi } from '@/api/catalog'
import { exchangeApi } from '@/api/exchange'
import { itemApi } from '@/api/item'
import { useAuthStore } from '@/stores/auth'
import type { Category, ExchangeIntent, ExchangePlan, Item } from '@/types'
import { formatDate, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import CoverImage from '@/components/CoverImage.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import CountdownText from '@/components/CountdownText.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const myItems = ref<Item[]>([])
const targetItems = ref<Item[]>([])
const categories = ref<Category[]>([])
const intents = ref<ExchangeIntent[]>([])
const plans = ref<ExchangePlan[]>([])
const loading = ref(true)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  myItemId: '',
  matchType: 'specific' as 'specific' | 'condition',
  targetItemId: '',
  targetCategoryId: '',
  keyword: ''
})

const rules: FormRules = {
  myItemId: [{ required: true, message: '请选择你的物品', trigger: 'change' }]
}

const categoryOptions = computed(() => {
  const roots = categories.value.filter((category) => category.parentId === null)
  return roots.flatMap((root) =>
    categories.value
      .filter((child) => child.parentId === root.id)
      .map((child) => ({ value: String(child.id), label: `${root.name} / ${child.name}` }))
  )
})

const availableTargets = computed(() =>
  targetItems.value.filter((item) => String(item.id) !== form.myItemId)
)

const pendingPlans = computed(() => plans.value.filter((plan) => plan.status === 'pending'))

async function load() {
  loading.value = true
  try {
    const [mine, all, categoryList, intentList, planList] = await Promise.all([
      itemApi.list({ scope: 'mine' }),
      itemApi.list({ status: 'on_sale' }),
      catalogApi.categories(),
      exchangeApi.intents(),
      exchangeApi.plans()
    ])
    myItems.value = mine.filter((item) => item.status === 'on_sale')
    targetItems.value = all
    categories.value = categoryList
    intents.value = intentList
    plans.value = planList
  } finally {
    loading.value = false
  }
}

function openDialog(targetItemId?: number) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!myItems.value.length) {
    ElMessage.warning('你还没有可用于置换的在售物品，先去发布一件吧')
    router.push('/publish')
    return
  }
  form.myItemId = String(myItems.value[0].id)
  form.matchType = targetItemId ? 'specific' : 'specific'
  form.targetItemId = targetItemId ? String(targetItemId) : ''
  form.targetCategoryId = ''
  form.keyword = ''
  dialogVisible.value = true
}

async function submitIntent() {
  await formRef.value?.validate()
  if (form.matchType === 'specific' && !form.targetItemId) {
    ElMessage.warning('请选择期望置换的物品')
    return
  }
  if (form.matchType === 'condition' && !form.targetCategoryId && !form.keyword) {
    ElMessage.warning('请填写期望分类或关键词')
    return
  }
  submitting.value = true
  try {
    await exchangeApi.createIntent({
      myItemId: Number(form.myItemId),
      matchType: form.matchType,
      targetItemId: form.matchType === 'specific' ? Number(form.targetItemId) : undefined,
      targetCategoryId: form.matchType === 'condition' ? Number(form.targetCategoryId) : undefined,
      keyword: form.matchType === 'condition' ? form.keyword : undefined
    })
    ElMessage.success('置换意向已进入匹配池')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

async function confirmPlan(plan: ExchangePlan) {
  await exchangeApi.confirmPlan(plan.id)
  ElMessage.success('已确认，等待其他参与人')
  await load()
}

async function rejectPlan(plan: ExchangePlan) {
  await ElMessageBox.confirm('拒绝后方案作废，所有物品解锁回在售状态。', '拒绝置换', {
    confirmButtonText: '拒绝',
    cancelButtonText: '再想想',
    type: 'warning'
  })
  await exchangeApi.rejectPlan(plan.id)
  ElMessage.success('方案已作废')
  await load()
}

function myConfirmed(plan: ExchangePlan) {
  return plan.participants.find((entry) => entry.userId === auth.user?.id)?.confirmed ?? false
}

onMounted(async () => {
  await load()
  const target = Number(route.query.targetItemId)
  if (target) openDialog(target)
})
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="EXCHANGE"
      title="物品置换"
      subtitle="用你的物品换想要的物品。系统寻找二人互换或三人环，全部确认后线下面交。"
    >
      <template #actions>
        <el-button type="primary" round @click="openDialog()">发起置换</el-button>
      </template>
    </PageHeader>

    <section class="explain">
      <div class="step">
        <span class="num">01</span>
        <strong>提交意向</strong>
        <p>选择自己的物品，指定期望物品或分类 / 关键词条件。</p>
      </div>
      <div class="step">
        <span class="num">02</span>
        <strong>系统匹配</strong>
        <p>校验物品状态、持有人与信用，寻找 2–3 人闭环。</p>
      </div>
      <div class="step">
        <span class="num">03</span>
        <strong>全部确认</strong>
        <p>10 分钟内三方确认，物品锁定；线下面交后更新归属与履历。</p>
      </div>
    </section>

    <div v-loading="loading">
      <section v-if="plans.length" class="section">
        <div class="section-head">
          <div>
            <h2>置换方案</h2>
            <p>有方案待你确认时，物品会暂时锁定</p>
          </div>
        </div>

        <div class="plan-list">
          <article v-for="plan in plans" :key="plan.id" class="plan surface">
            <header class="plan-head">
              <div class="plan-title">
                <StatusTag kind="plan" :status="plan.status" />
                <strong>{{ plan.ringType }} 人置换环</strong>
                <span class="plan-id">方案 #{{ plan.id }}</span>
              </div>
              <div v-if="plan.status === 'pending'" class="deadline">
                确认截止 <CountdownText :deadline="plan.deadline" />
              </div>
            </header>

            <div class="participants">
              <template v-for="(participant, index) in plan.participants" :key="participant.userId">
                <div class="participant">
                  <div class="mini-cover">
                    <CoverImage :seed="participant.itemSeed" :title="participant.itemTitle" />
                  </div>
                  <div class="participant-info">
                    <strong>{{ participant.userName }}</strong>
                    <span>{{ participant.itemTitle }}</span>
                  </div>
                  <el-tag :type="participant.confirmed ? 'success' : 'info'" size="small" effect="light" round>
                    {{ participant.confirmed ? '已确认' : '待确认' }}
                  </el-tag>
                </div>
                <span v-if="index < plan.participants.length - 1" class="arrow">⇄</span>
              </template>
            </div>

            <footer class="plan-foot">
              <span class="muted">生成于 {{ formatTime(plan.createdAt) }}</span>
              <template v-if="plan.status === 'pending'">
                <el-button
                  size="small"
                  type="primary"
                  :disabled="myConfirmed(plan)"
                  @click="confirmPlan(plan)"
                >
                  {{ myConfirmed(plan) ? '已确认，等待其他人' : '确认置换' }}
                </el-button>
                <el-button size="small" plain @click="rejectPlan(plan)">拒绝</el-button>
              </template>
            </footer>
          </article>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div>
            <h2>我的置换意向</h2>
            <p>意向有效期间持续参与匹配</p>
          </div>
        </div>

        <div v-if="intents.length" class="intent-list">
          <article v-for="intent in intents" :key="intent.id" class="intent surface">
            <div class="side">
              <div class="mini-cover">
                <CoverImage :seed="intent.myItemSeed" :title="intent.myItemTitle" />
              </div>
              <div class="intent-info">
                <span class="label">我的物品</span>
                <strong>{{ intent.myItemTitle }}</strong>
              </div>
            </div>
            <span class="exchange-icon">⇄</span>
            <div class="side target">
              <div class="want-icon">想要</div>
              <div class="intent-info">
                <span class="label">
                  {{ intent.matchType === 'specific' ? '指定物品' : '条件匹配' }}
                </span>
                <strong>
                  {{ intent.matchType === 'specific' ? intent.targetItemTitle : intent.targetCategoryName || intent.keyword }}
                </strong>
              </div>
            </div>
            <div class="intent-foot">
              <StatusTag kind="intent" :status="intent.status" />
              <span class="date">{{ formatDate(intent.createdAt) }}</span>
            </div>
          </article>
        </div>
        <EmptyState v-else-if="!loading" description="还没有发起过置换">
          <el-button type="primary" round @click="openDialog()">发起置换</el-button>
        </EmptyState>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" title="发起置换" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="我的物品" prop="myItemId">
          <el-select v-model="form.myItemId" class="full" size="large" placeholder="选择在售物品">
            <el-option
              v-for="item in myItems"
              :key="item.id"
              :label="item.title"
              :value="String(item.id)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="匹配方式">
          <el-radio-group v-model="form.matchType">
            <el-radio-button value="specific">指定物品</el-radio-button>
            <el-radio-button value="condition">条件匹配</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.matchType === 'specific'">
          <el-form-item label="期望置换的物品" required>
            <el-select v-model="form.targetItemId" class="full" size="large" filterable placeholder="选择在售物品">
              <el-option
                v-for="item in availableTargets"
                :key="item.id"
                :label="`${item.title} · ¥${item.price}`"
                :value="String(item.id)"
              />
            </el-select>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="期望分类">
            <el-select v-model="form.targetCategoryId" class="full" size="large" clearable placeholder="不限分类">
              <el-option v-for="option in categoryOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="期望关键词">
            <el-input v-model="form.keyword" size="large" placeholder="如：吉他、显示器、考研书" />
          </el-form-item>
        </template>

        <p class="dialog-tip">
          发布后进入匹配池。匹配成功会生成方案并通知所有参与人，10 分钟内全部确认才生效。
        </p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitIntent">提交意向</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.explain {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 10px;
}

.step {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 18px 20px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-surface);
}

.num {
  color: var(--sgj-brass);
  font-family: var(--sgj-font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.step strong {
  font-size: 15px;
}

.step p {
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.7;
}

.section {
  margin-top: 12px;
}

.plan-list,
.intent-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.plan {
  padding: 20px 24px;
}

.plan-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.plan-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plan-title strong {
  font-family: var(--sgj-font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.plan-id {
  color: #a3aaa5;
  font-size: 11px;
}

.deadline {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.participants {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-paper-deep);
}

.participant {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-surface);
}

.mini-cover {
  width: 52px;
  height: 40px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.participant-info,
.intent-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.participant-info strong,
.intent-info strong {
  font-size: 13px;
}

.participant-info span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.arrow {
  color: var(--sgj-accent);
  font-size: 18px;
}

.plan-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.plan-foot .muted {
  margin-right: auto;
  font-size: 11px;
}

.intent {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
}

.side {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 12px;
  min-width: 220px;
}

.target {
  justify-content: flex-end;
  text-align: right;
}

.target .intent-info {
  align-items: flex-end;
}

.label {
  color: var(--sgj-muted);
  font-size: 10px;
  letter-spacing: 1px;
}

.exchange-icon {
  color: var(--sgj-accent);
  font-size: 20px;
}

.want-icon {
  display: grid;
  width: 52px;
  height: 40px;
  place-items: center;
  border: 1px dashed var(--sgj-line-strong);
  border-radius: 1px;
  color: var(--sgj-muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.intent-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.date {
  color: #a3aaa5;
  font-size: 11px;
}

.dialog-tip {
  margin: 4px 0 0;
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

@media (max-width: 820px) {
  .explain {
    grid-template-columns: 1fr;
  }

  .intent {
    flex-direction: column;
    align-items: flex-start;
  }

  .target {
    justify-content: flex-start;
    text-align: left;
  }

  .target .intent-info {
    align-items: flex-start;
  }

  .exchange-icon {
    transform: rotate(90deg);
  }
}
</style>
