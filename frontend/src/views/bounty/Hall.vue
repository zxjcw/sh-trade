<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { bountyApi } from '@/api/bounty'
import { useAuthStore } from '@/stores/auth'
import type { Bounty } from '@/types'
import { conditionLabels } from '@/utils/status'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const bounties = ref<Bounty[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    bounties.value = await bountyApi.list('hall')
  } finally {
    loading.value = false
  }
}

function goPublish(keyword?: string) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/bounty/publish' } })
    return
  }
  if (!auth.isVerified) {
    router.push({ name: 'verify', query: { redirect: '/bounty/publish' } })
    return
  }
  router.push({ name: 'bounty-publish', query: keyword ? { keyword } : {} })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="WANTED"
      title="悬赏大厅"
      subtitle="没搜到想要的？留下预算和关键词。有物品上架命中时，系统会第一时间通知你。"
    >
      <template #actions>
        <el-button round @click="router.push('/bounty/mine')">我的悬赏</el-button>
        <el-button type="primary" round @click="goPublish()">发布求购</el-button>
      </template>
    </PageHeader>

    <section class="banner">
      <div>
        <span class="banner-kicker">不想一直刷列表？</span>
        <h2>让物品来找你</h2>
        <p>悬赏按分类、关键词和预算匹配，价格容差默认 ±20%。同一买家最多同时保留 3 条悬赏。</p>
      </div>
      <el-button type="primary" size="large" @click="goPublish()">发布一条悬赏</el-button>
    </section>

    <div v-loading="loading" class="bounty-grid">
      <article v-for="bounty in bounties" :key="bounty.id" class="bounty-card surface">
        <div class="card-head">
          <el-tag effect="plain" round size="small">{{ bounty.categoryName }}</el-tag>
          <span class="expire">有效至 {{ formatDate(bounty.expiresAt) }}</span>
        </div>
        <h3>{{ bounty.keyword }}</h3>
        <div class="budget">
          <span>预算</span>
          <strong>¥{{ bounty.budget.toFixed(0) }}</strong>
          <span class="condition">期望 {{ conditionLabels[bounty.condition] }}</span>
        </div>
        <div class="meta">
          <span>{{ bounty.buyerName }}</span>
          <span>·</span>
          <span>{{ bounty.matchCount }} 件匹配</span>
        </div>
        <div class="actions">
          <el-button size="small" @click="router.push({ name: 'search', query: { keyword: bounty.keyword } })">
            看看匹配物品
          </el-button>
          <el-button size="small" type="primary" plain @click="goPublish(bounty.keyword)">
            我有类似物品
          </el-button>
        </div>
      </article>

      <EmptyState v-if="!loading && !bounties.length" description="暂时没有悬赏">
        <el-button type="primary" round @click="goPublish()">发布第一条求购</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<style scoped>
.banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 32px;
  margin-bottom: 26px;
  border: 1px solid var(--sgj-line);
  border-left: 2px solid var(--sgj-brass);
  border-radius: 0;
  background: var(--sgj-paper-deep);
  color: var(--sgj-ink);
}

.banner-kicker {
  color: var(--sgj-brass);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.banner h2 {
  margin: 8px 0 10px;
  color: var(--sgj-ink);
  font-family: var(--sgj-font-display);
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.banner p {
  max-width: 620px;
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.85;
}

.bounty-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.bounty-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 22px;
  transition: border-color 0.2s ease;
}

.bounty-card:hover {
  border-color: var(--sgj-line-strong);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.expire {
  color: #a3aaa5;
  font-size: 11px;
}

.bounty-card h3 {
  margin: 0;
  font-size: 17px;
  line-height: 1.45;
}

.budget {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.budget span {
  color: var(--sgj-muted);
  font-size: 12px;
}

.budget strong {
  color: var(--sgj-accent-deep);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.budget .condition {
  margin-left: auto;
  color: var(--sgj-muted);
  font-size: 11px;
}

.meta {
  display: flex;
  gap: 5px;
  color: var(--sgj-muted);
  font-size: 11px;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 4px;
}

.actions .el-button {
  flex: 1;
}

@media (max-width: 900px) {
  .bounty-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .bounty-grid {
    grid-template-columns: 1fr;
  }

  .banner {
    padding: 22px;
  }
}
</style>
