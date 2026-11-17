<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { opsApi } from '@/api/ops'
import type { Arbitration } from '@/types'
import { formatTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const arbitrations = ref<Arbitration[]>([])
const loading = ref(true)
const drawerVisible = ref(false)
const current = ref<Arbitration | null>(null)
const resolveVisible = ref(false)
const resolving = ref(false)
const result = ref('buyer')
const note = ref('')

const pendingCount = computed(
  () => arbitrations.value.filter((entry) => entry.status === 'pending').length
)

async function load() {
  loading.value = true
  try {
    arbitrations.value = await opsApi.arbitrations()
  } finally {
    loading.value = false
  }
}

function openDrawer(item: Arbitration) {
  current.value = item
  drawerVisible.value = true
}

function openResolve(item: Arbitration | null) {
  if (!item) return
  current.value = item
  result.value = 'buyer'
  note.value = ''
  resolveVisible.value = true
}

async function resolve() {
  if (!current.value) return
  resolving.value = true
  try {
    const decision = result.value === 'buyer' ? 'buyer' : 'seller'
    await opsApi.resolveArbitration(current.value.id, decision, note.value)
    ElMessage.success(decision === 'buyer' ? '已支持买家并执行退款' : '已支持卖家，售后关闭')
    resolveVisible.value = false
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    resolving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="surface toolbar">
      <div>
        <h2>仲裁申请</h2>
        <p>共 {{ arbitrations.length }} 条，待处理 {{ pendingCount }} 条。裁定结果会通知买卖双方并留档。</p>
      </div>
      <el-button round :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="arbitrations" stripe>
        <el-table-column prop="id" label="仲裁号" width="90" />
        <el-table-column prop="orderId" label="订单号" width="170" />
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="targetName" label="被申请人" width="110" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column label="举证" min-width="240">
          <template #default="{ row }">
            <span class="evidence">{{ row.evidence }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending' ? 'warning' : 'success'" size="small" effect="light" round>
              {{ row.status === 'pending' ? '待处理' : '已裁定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDrawer(row)">详情</el-button>
            <el-button
              v-if="row.status === 'pending'"
              size="small"
              type="primary"
              @click="openResolve(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !arbitrations.length" description="没有仲裁申请" />
    </div>

    <el-drawer v-model="drawerVisible" title="仲裁详情" size="480px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="仲裁号">{{ current.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ current.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="被申请人">{{ current.targetName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ current.type }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(current.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            {{ current.status === 'pending' ? '待处理' : '已裁定' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="current.result" label="裁定结果">
            {{ current.result === 'buyer' ? '支持买家' : '支持卖家' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="current.handler" label="处理人">{{ current.handler }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="sub-title">举证说明</h3>
        <p class="evidence-text">{{ current.evidence || '未填写' }}</p>

        <div v-if="current.status === 'pending'" class="drawer-actions">
          <el-button type="primary" @click="openResolve(current)">处理该仲裁</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="resolveVisible" title="裁定仲裁" width="480px">
      <p class="dialog-tip">
        支持买家会执行退款/扣回并恢复物品；支持卖家会关闭售后。裁定后不可撤回。
      </p>
      <el-radio-group v-model="result" class="result-group">
        <el-radio value="buyer" border>支持买家</el-radio>
        <el-radio value="seller" border>支持卖家</el-radio>
      </el-radio-group>
      <el-input v-model="note" type="textarea" :rows="3" placeholder="裁定说明（选填）" class="note" />
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolving" @click="resolve">确认裁定</el-button>
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
}

.table-wrap {
  padding: 8px 16px 16px;
}

.evidence {
  display: -webkit-box;
  overflow: hidden;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.sub-title {
  margin: 18px 0 8px;
  font-size: 14px;
}

.evidence-text {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-paper-deep);
  color: var(--sgj-ink-soft);
  font-size: 12px;
  line-height: 1.8;
}

.drawer-actions {
  margin-top: 24px;
}

.dialog-tip {
  margin: 0 0 16px;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.8;
}

.result-group {
  display: flex;
  gap: 10px;
}

.note {
  margin-top: 14px;
}
</style>
