<script setup lang="ts">
import { conditionLabels } from '@/utils/status'
import type { Item } from '@/types'
import CoverImage from './CoverImage.vue'
import StatusTag from './StatusTag.vue'

defineProps<{ item: Item }>()
</script>

<template>
  <router-link class="item-card surface" :to="`/items/${item.id}`">
    <div class="thumb">
      <CoverImage
        :seed="item.coverSeed"
        :title="item.title"
        :badge="conditionLabels[item.condition]"
      />
    </div>
    <div class="body">
      <h3 class="title">{{ item.title }}</h3>
      <div class="price-row">
        <span class="price">{{ item.price.toFixed(2) }}</span>
        <span class="original">原价 ¥{{ item.originalPrice.toFixed(0) }}</span>
      </div>
      <div class="meta">
        <span>{{ item.sellerName }}</span>
        <span class="dot">·</span>
        <span>信用 {{ item.sellerCredit }}</span>
        <StatusTag v-if="item.status !== 'on_sale'" class="status" kind="item" :status="item.status" />
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.item-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.item-card:hover {
  transform: translateY(-3px);
  border-color: rgba(47, 107, 79, 0.35);
  box-shadow: var(--sgj-shadow);
}

.thumb {
  aspect-ratio: 4 / 3;
  overflow: hidden;
}

.body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 14px 15px 16px;
}

.title {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--sgj-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: auto;
}

.price {
  font-size: 20px;
}

.original {
  color: #a3aaa5;
  font-size: 12px;
  text-decoration: line-through;
}

.meta {
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  color: var(--sgj-muted);
  font-size: 12px;
  white-space: nowrap;
}

.dot {
  color: #c7ccc8;
}

.status {
  margin-left: auto;
}
</style>
