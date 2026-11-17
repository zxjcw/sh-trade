<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bountyApi } from '@/api/bounty'
import type { Bounty } from '@/types'
import { conditionLabels } from '@/utils/status'
import { formatDate, formatMoney } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const bounties = ref<Bounty[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    bounties.value = await bountyApi.list('mine')
  } finally {
    loading.value = false
  }
}

async function close(bounty: Bounty) {
  await ElMessageBox.confirm(`关闭悬赏「${bounty.keyword}」后将不再匹配物品。`, '关闭悬赏', {
    confirmButtonText: '关闭',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await bountyApi.close(bounty.id)
  ElMessage.success('悬赏已关闭')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page page-narrow">
    <PageHeader
      eyebrow="MY WANTED"
      title="我的悬赏"
      subtitle="悬赏在有效期内持续匹配新上架物品，也可以随时关闭。"
    >
      <template #actions>
        <el-button type="primary" round @click="router.push('/bounty/publish')">发布悬赏</el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="list">
      <article v-for="bounty in bounties" :key="bounty.id" class="item surface">
        <div class="main">
          <div class="head">
            <StatusTag kind="bounty" :status="bounty.status" />
            <span class="category">{{ bounty.categoryName }}</span>
            <span class="date">发布于 {{ formatDate(bounty.createdAt) }}</span>
          </div>
          <h3>{{ bounty.keyword }}</h3>
          <p class="meta">
            预算 <strong>{{ formatMoney(bounty.budget) }}</strong>
            <span>·</span>
            期望 {{ conditionLabels[bounty.condition] }}
            <span>·</span>
            有效期至 {{ formatDate(bounty.expiresAt) }}
            <span>·</span>
            {{ bounty.matchCount }} 次匹配
          </p>
        </div>
        <div class="actions">
          <el-button size="small" @click="router.push({ name: 'search', query: { keyword: bounty.keyword } })">
            查看匹配
          </el-button>
          <el-button v-if="bounty.status === 'open'" size="small" plain @click="close(bounty)">
            关闭
          </el-button>
        </div>
      </article>

      <EmptyState v-if="!loading && !bounties.length" description="还没有发布过悬赏">
        <el-button type="primary" round @click="router.push('/bounty/publish')">发布求购</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<style scoped>
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 180px;
}

.item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 22px;
}

.main {
  flex: 1;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.date {
  margin-left: auto;
  color: #a3aaa5;
}

h3 {
  margin: 10px 0 6px;
  font-size: 18px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.meta strong {
  color: var(--sgj-accent-deep);
}

.actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 640px) {
  .item {
    flex-wrap: wrap;
  }

  .actions {
    width: 100%;
  }

  .actions .el-button {
    flex: 1;
  }
}
</style>
