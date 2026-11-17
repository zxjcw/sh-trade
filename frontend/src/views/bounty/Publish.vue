<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { bountyApi } from '@/api/bounty'
import { catalogApi } from '@/api/catalog'
import type { Category, ItemCondition } from '@/types'
import { conditionOptions } from '@/utils/status'
import PageHeader from '@/components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const loading = ref(false)

const form = reactive({
  categoryId: '',
  keyword: String(route.query.keyword ?? ''),
  budget: '',
  condition: 'good' as ItemCondition,
  expiresDays: 7
})

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  keyword: [{ required: true, message: '请输入求购关键词', trigger: 'blur' }],
  budget: [{ required: true, message: '请输入预算', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择期望成色', trigger: 'change' }]
}

const categoryOptions = computed(() => {
  const roots = categories.value.filter((category) => category.parentId === null)
  return roots.flatMap((root) =>
    categories.value
      .filter((child) => child.parentId === root.id)
      .map((child) => ({ value: String(child.id), label: `${root.name} / ${child.name}` }))
  )
})

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await bountyApi.create({
      categoryId: Number(form.categoryId),
      keyword: form.keyword,
      budget: Number(form.budget),
      condition: form.condition,
      expiresDays: form.expiresDays
    })
    ElMessage.success('悬赏已发布，命中时会通知你')
    router.push('/bounty/mine')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await catalogApi.categories()
})
</script>

<template>
  <div class="page page-narrow">
    <PageHeader
      eyebrow="WANTED"
      title="发布求购"
      subtitle="填写你想要的物品，系统会按分类、关键词和价格容差匹配新上架物品。"
    >
      <template #actions>
        <el-button round @click="router.push('/bounty/mine')">我的悬赏</el-button>
      </template>
    </PageHeader>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="surface form-card">
      <div class="two-col">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" size="large" class="full" placeholder="选择求购分类">
            <el-option v-for="option in categoryOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望成色" prop="condition">
          <el-select v-model="form.condition" size="large" class="full">
            <el-option v-for="option in conditionOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="求购关键词" prop="keyword">
        <el-input v-model="form.keyword" size="large" maxlength="40" show-word-limit placeholder="如：高数教材、27 寸显示器、羽毛球拍" />
      </el-form-item>

      <div class="two-col">
        <el-form-item label="预算（元）" prop="budget">
          <el-input v-model="form.budget" size="large" type="number" placeholder="0.00" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="form.expiresDays" size="large" class="full">
            <el-option label="3 天" :value="3" />
            <el-option label="7 天" :value="7" />
            <el-option label="15 天" :value="15" />
          </el-select>
        </el-form-item>
      </div>

      <div class="tips">
        <p><strong>匹配规则</strong></p>
        <ul>
          <li>物品上架时按分类 + 关键词 + 价格容差（±20%）匹配。</li>
          <li>命中后给你发送「悬赏命中」通知，可直接下单。</li>
          <li>同一买家最多同时保留 3 条悬赏。</li>
        </ul>
      </div>

      <div class="actions">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button type="primary" size="large" :loading="loading" @click="submit">发布悬赏</el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.form-card {
  padding: 28px 30px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.full {
  width: 100%;
}

.tips {
  padding: 14px 18px;
  border-radius: 12px;
  background: #fbf5e8;
  color: #8a7b60;
  font-size: 12px;
  line-height: 1.9;
}

.tips strong {
  color: var(--sgj-accent-deep);
}

.tips ul {
  margin: 4px 0 0;
  padding-left: 18px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

@media (max-width: 640px) {
  .form-card {
    padding: 20px;
  }

  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
