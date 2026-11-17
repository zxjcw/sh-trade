<script setup lang="ts">
import { conditionLabels } from '@/utils/status'
import type { Item } from '@/types'
import CoverImage from './CoverImage.vue'
import StatusTag from './StatusTag.vue'

defineProps<{ item: Item }>()
</script>

<template>
  <router-link class="item-card" :to="`/items/${item.id}`">
    <div class="plate">
      <CoverImage
        :seed="item.coverSeed"
        :title="item.title"
        :badge="conditionLabels[item.condition]"
      />
    </div>
    <div class="body">
      <div class="meta-top">
        <span class="category">{{ item.categoryName }}</span>
        <StatusTag v-if="item.status !== 'on_sale'" kind="item" :status="item.status" />
      </div>
      <h3 class="title">{{ item.title }}</h3>
      <div class="price-row">
        <span class="price">{{ item.price.toFixed(2) }}</span>
        <span class="original">原价 ¥{{ item.originalPrice.toFixed(0) }}</span>
      </div>
      <div class="meta">
        <span>{{ item.sellerName }}</span>
        <span class="dot">/</span>
        <span>信用 {{ item.sellerCredit }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.item-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plate {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  background: var(--sgj-paper-deep);
  transition: border-color 0.25s ease;
}

.item-card:hover .plate {
  border-color: var(--sgj-ink-soft);
}

.body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 7px;
}

.meta-top {
  display: flex;
  min-height: 20px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.category {
  color: var(--sgj-muted);
  font-size: 10px;
  letter-spacing: 0.14em;
}

.title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--sgj-ink);
  font-family: var(--sgj-font-body);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.item-card:hover .title {
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}

.price-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 10px;
  margin-top: auto;
}

.price {
  font-size: 21px;
}

.original {
  color: var(--sgj-faint);
  font-size: 11px;
  text-decoration: line-through;
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid var(--sgj-line);
  color: var(--sgj-faint);
  font-size: 11px;
  letter-spacing: 0.02em;
}

.dot {
  color: var(--sgj-line-strong);
}
</style>
