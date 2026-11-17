<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { opsApi } from '@/api/ops'
import type { ReportData } from '@/types'
import { formatMoney } from '@/utils/format'

const report = ref<ReportData | null>(null)
const loading = ref(true)

const maxCategory = computed(() =>
  Math.max(1, ...(report.value?.categoryStats.map((entry) => entry.count) ?? [1]))
)

const maxMonth = computed(() =>
  Math.max(1, ...(report.value?.monthlyStats.map((entry) => entry.amount) ?? [1]))
)

async function load() {
  loading.value = true
  try {
    report.value = await opsApi.report()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="surface toolbar">
      <div>
        <h2>运营概览</h2>
        <p>报表实时查询，不落表。统计口径：已完成订单、用户账户流水与状态记录。</p>
      </div>
      <el-button round @click="load">刷新</el-button>
    </div>

    <template v-if="report">
      <div class="stats">
        <article class="stat surface">
          <span class="stat-label">已完成订单</span>
          <strong>{{ report.completedCount }}</strong>
          <span class="stat-hint">笔</span>
        </article>
        <article class="stat surface">
          <span class="stat-label">成交额</span>
          <strong>{{ formatMoney(report.completedAmount) }}</strong>
          <span class="stat-hint">按订单金额汇总</span>
        </article>
        <article class="stat surface">
          <span class="stat-label">退款率</span>
          <strong>{{ report.refundRate.toFixed(1) }}%</strong>
          <span class="stat-hint">已退款 / 已支付订单</span>
        </article>
        <article class="stat surface">
          <span class="stat-label">待审核</span>
          <strong>{{ report.pendingAudits }}</strong>
          <span class="stat-hint">件物品</span>
        </article>
        <article class="stat surface">
          <span class="stat-label">待仲裁</span>
          <strong>{{ report.pendingArbitrations }}</strong>
          <span class="stat-hint">条申请</span>
        </article>
      </div>

      <div class="charts">
        <section class="surface panel">
          <h2>品类成交分布</h2>
          <p class="panel-hint">按已完成订单关联的物品分类统计</p>
          <div v-if="report.categoryStats.length" class="bars">
            <div v-for="entry in report.categoryStats" :key="entry.name" class="bar-row">
              <span class="bar-label">{{ entry.name }}</span>
              <div class="bar-track">
                <div class="bar" :style="{ width: `${(entry.count / maxCategory) * 100}%` }"></div>
              </div>
              <span class="bar-value">{{ entry.count }} 笔 · {{ formatMoney(entry.amount) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无成交数据" :image-size="70" />
        </section>

        <section class="surface panel">
          <h2>近 6 个月成交</h2>
          <p class="panel-hint">按订单完成时间统计</p>
          <div class="month-bars">
            <div v-for="entry in report.monthlyStats" :key="entry.month" class="month">
              <span class="month-value">{{ entry.count }}</span>
              <div class="month-track">
                <div class="month-fill" :style="{ height: `${(entry.amount / maxMonth) * 100}%` }"></div>
              </div>
              <span class="month-label">{{ entry.month.slice(5) }} 月</span>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0 0 6px;
  font-size: 18px;
}

.toolbar p {
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 18px 20px;
  border-top: 2px solid var(--sgj-line-strong);
}

.stat-label {
  color: var(--sgj-muted);
  font-size: 12px;
}

.stat strong {
  font-family: var(--sgj-font-display);
  font-size: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
}

.stat-hint {
  color: #a3aaa5;
  font-size: 10px;
}

.charts {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 16px;
}

.panel {
  padding: 22px 24px;
}

.panel h2 {
  margin: 0 0 4px;
  font-size: 17px;
}

.panel-hint {
  margin: 0 0 18px;
  color: var(--sgj-muted);
  font-size: 11px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: 90px 1fr 150px;
  align-items: center;
  gap: 10px;
}

.bar-label {
  overflow: hidden;
  color: #4b564f;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  height: 8px;
  overflow: hidden;
  border-radius: 0;
  background: var(--sgj-paper-deep);
}

.bar {
  height: 100%;
  border-radius: 0;
  background: var(--sgj-green);
}

.bar-value {
  color: var(--sgj-muted);
  font-size: 11px;
  text-align: right;
}

.month-bars {
  display: flex;
  height: 220px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

.month {
  display: flex;
  flex: 1;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
}

.month-value {
  color: var(--sgj-muted);
  font-size: 10px;
}

.month-track {
  display: flex;
  width: 100%;
  height: 150px;
  align-items: flex-end;
  justify-content: center;
}

.month-fill {
  width: 60%;
  border-radius: 0;
  background: var(--sgj-brass);
  opacity: 0.82;
}

.month-label {
  color: #a3aaa5;
  font-size: 10px;
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bar-row {
    grid-template-columns: 70px 1fr;
  }

  .bar-value {
    display: none;
  }
}
</style>
