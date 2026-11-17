<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api/catalog'
import { itemApi } from '@/api/item'
import type { Category, Item } from '@/types'
import { conditionOptions } from '@/utils/status'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const categories = ref<Category[]>([])
const items = ref<Item[]>([])
const loading = ref(false)

const filters = reactive({
  keyword: '',
  categoryId: '',
  condition: '',
  sort: 'latest'
})

const flatCategories = computed(() => {
  const roots = categories.value.filter((category) => category.parentId === null)
  return roots.flatMap((root) => [
    { value: String(root.id), label: root.name },
    ...categories.value
      .filter((child) => child.parentId === root.id)
      .map((child) => ({ value: String(child.id), label: `${root.name} / ${child.name}` }))
  ])
})

const resultText = computed(() =>
  filters.keyword ? `“${filters.keyword}” 的搜索结果` : '全部在售物品'
)

function readQuery() {
  filters.keyword = String(route.query.keyword ?? '')
  filters.categoryId = String(route.query.categoryId ?? '')
  filters.condition = String(route.query.condition ?? '')
  filters.sort = String(route.query.sort ?? 'latest')
}

async function fetchItems() {
  loading.value = true
  try {
    items.value = await itemApi.list({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
      condition: filters.condition || undefined,
      sort: filters.sort as 'latest' | 'price_asc' | 'price_desc',
      status: 'on_sale'
    })
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  router.push({
    name: 'search',
    query: {
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
      ...(filters.condition ? { condition: filters.condition } : {}),
      ...(filters.sort !== 'latest' ? { sort: filters.sort } : {})
    }
  })
}

function reset() {
  filters.keyword = ''
  filters.categoryId = ''
  filters.condition = ''
  filters.sort = 'latest'
  applyFilters()
}

watch(
  () => route.query,
  async () => {
    readQuery()
    await fetchItems()
  }
)

onMounted(async () => {
  categories.value = await catalogApi.categories()
  readQuery()
  await fetchItems()
})
</script>

<template>
  <div class="page">
    <PageHeader
      eyebrow="MARKETPLACE"
      title="搜索物品"
      :subtitle="resultText"
    >
      <template #actions>
        <el-button round @click="router.push('/bounty')">发布求购</el-button>
        <el-button type="primary" round @click="router.push('/publish')">发布物品</el-button>
      </template>
    </PageHeader>

    <section class="filter surface">
      <el-input
        v-model="filters.keyword"
        class="keyword"
        size="large"
        placeholder="输入物品名称或关键词"
        clearable
        @keyup.enter="applyFilters"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="filters.categoryId" size="large" placeholder="全部分类" clearable>
        <el-option v-for="option in flatCategories" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>

      <el-select v-model="filters.condition" size="large" placeholder="全部成色" clearable>
        <el-option v-for="option in conditionOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>

      <el-select v-model="filters.sort" size="large">
        <el-option label="最新上架" value="latest" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
      </el-select>

      <div class="filter-actions">
        <el-button size="large" @click="reset">重置</el-button>
        <el-button size="large" type="primary" @click="applyFilters">筛选</el-button>
      </div>
    </section>

    <div v-loading="loading" class="result">
      <p class="count">共找到 <strong>{{ items.length }}</strong> 件物品</p>
      <div v-if="items.length" class="grid-cards">
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </div>
      <EmptyState v-else-if="!loading" description="没有找到符合条件的物品">
        <el-button type="primary" round @click="router.push('/bounty')">发布求购，让别人来找你</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<style scoped>
.filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 16px;
  margin-bottom: 24px;
}

.keyword {
  flex: 1;
  min-width: 220px;
}

.filter :deep(.el-select) {
  width: 150px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.count {
  margin: 0 0 14px;
  color: var(--sgj-muted);
  font-size: 13px;
}

.count strong {
  color: var(--sgj-primary);
}

.result {
  min-height: 300px;
}

@media (max-width: 760px) {
  .filter :deep(.el-select) {
    width: calc(50% - 5px);
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .filter-actions .el-button {
    flex: 1;
  }
}
</style>
