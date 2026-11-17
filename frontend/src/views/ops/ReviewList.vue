<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { opsApi } from '@/api/ops'
import type { Item, ItemCondition } from '@/types'
import { conditionLabels, notesLabels, sourceLabels } from '@/utils/status'
import { formatMoney, formatTime } from '@/utils/format'
import CoverImage from '@/components/CoverImage.vue'
import EmptyState from '@/components/EmptyState.vue'

const items = ref<Item[]>([])
const loading = ref(true)
const drawerVisible = ref(false)
const current = ref<Item | null>(null)
const rejectVisible = ref(false)
const rejectReason = ref('')
const operating = ref(false)

const historyLabels: Record<string, string> = {
  created: '建档',
  deal: '成交',
  exchange: '置换',
  return: '退货',
  repair: '维修'
}

function conditionLabel(condition: ItemCondition) {
  return conditionLabels[condition]
}

async function load() {
  loading.value = true
  try {
    items.value = await opsApi.pendingItems()
  } finally {
    loading.value = false
  }
}

function openDrawer(item: Item) {
  current.value = item
  drawerVisible.value = true
}

async function approve(item: Item | null) {
  if (!item) return
  await ElMessageBox.confirm(`确认通过「${item.title}」的审核并上架吗？`, '审核通过', {
    confirmButtonText: '通过并上架',
    cancelButtonText: '取消',
    type: 'success'
  })
  operating.value = true
  try {
    await opsApi.auditItem(item.id, 'approved')
    ElMessage.success('已通过审核并上架')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    operating.value = false
  }
}

function openReject() {
  rejectReason.value = ''
  rejectVisible.value = true
}

async function reject() {
  if (!current.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  operating.value = true
  try {
    await opsApi.auditItem(current.value.id, 'rejected', rejectReason.value)
    ElMessage.success('已驳回并通知卖家')
    rejectVisible.value = false
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    operating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="surface toolbar">
      <div>
        <h2>待审核物品</h2>
        <p>核对标题、分类、成色、图片与描述；审核记录由运营写入，上架状态由发布业务落库。</p>
      </div>
      <el-button round :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="items" stripe @row-click="openDrawer">
        <el-table-column label="物品" min-width="300">
          <template #default="{ row }">
            <div class="item-cell">
              <div class="thumb"><CoverImage :seed="row.coverSeed" :title="row.title" /></div>
              <div class="item-text">
                <strong>{{ row.title }}</strong>
                <span>{{ row.categoryName }} · {{ conditionLabel(row.condition) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="卖家" width="110" />
        <el-table-column label="售价" width="110">
          <template #default="{ row }">
            <span class="price">{{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="openDrawer(row)">查看</el-button>
            <el-button size="small" type="success" @click.stop="approve(row)">通过</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !items.length" description="没有待审核的物品" />
    </div>

    <el-drawer v-model="drawerVisible" title="物品审核" size="520px">
      <template v-if="current">
        <div class="drawer-cover">
          <CoverImage :seed="current.coverSeed" :title="current.title" />
        </div>
        <h2 class="drawer-title">{{ current.title }}</h2>
        <div class="price-row">
          <span class="price">{{ current.price.toFixed(2) }}</span>
          <span class="muted">原价 ¥{{ current.originalPrice.toFixed(0) }}</span>
        </div>

        <el-descriptions :column="2" border size="small" class="desc">
          <el-descriptions-item label="分类">{{ current.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ conditionLabels[current.condition] }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceLabels[current.source] }}</el-descriptions-item>
          <el-descriptions-item label="笔记">{{ notesLabels[current.notesTag] }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ current.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="信用分">{{ current.sellerCredit }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="sub-title">物品描述</h3>
        <p class="description">{{ current.description || '未填写描述' }}</p>

        <h3 class="sub-title">履历</h3>
        <el-timeline>
          <el-timeline-item
            v-for="event in current.history"
            :key="event.id"
            :timestamp="formatTime(event.time)"
          >
            {{ historyLabels[event.type] ?? event.type }} — {{ event.description }}
          </el-timeline-item>
        </el-timeline>

        <div class="drawer-actions">
          <el-button type="success" :loading="operating" @click="approve(current)">通过并上架</el-button>
          <el-button type="danger" plain @click="openReject">驳回</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectVisible" title="驳回物品" width="460px">
      <p class="dialog-tip">驳回理由会通过站内信发送给卖家，请写清楚需要修改的内容。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="如：图片与描述不一致 / 分类选择错误" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="operating" @click="reject">确认驳回</el-button>
      </template>
    </el-dialog>
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
  line-height: 1.7;
}

.table-wrap {
  padding: 8px 16px 16px;
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.thumb {
  width: 66px;
  height: 50px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.item-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.item-text strong {
  font-size: 13px;
}

.item-text span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.price {
  color: var(--sgj-accent-deep);
  font-weight: 600;
}

.drawer-cover {
  height: 200px;
  overflow: hidden;
  border: 1px solid var(--sgj-line-strong);
  border-radius: 1px;
}

.drawer-title {
  margin: 16px 0 8px;
  font-size: 20px;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price-row .price {
  font-size: 26px;
}

.desc {
  margin: 16px 0;
}

.sub-title {
  margin: 18px 0 8px;
  font-size: 14px;
}

.description {
  margin: 0;
  color: #4b564f;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-line;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

.drawer-actions .el-button {
  flex: 1;
}

.dialog-tip {
  margin: 0 0 12px;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.7;
}
</style>
