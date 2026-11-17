<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'
import { catalogApi } from '@/api/catalog'
import { itemApi } from '@/api/item'
import type { Category, ItemCondition, NotesTag } from '@/types'
import { conditionOptions } from '@/utils/status'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const categories = ref<Category[]>([])
const files = ref<UploadUserFile[]>([])

const form = reactive({
  title: '',
  categoryId: '',
  price: '',
  originalPrice: '',
  purchasePrice: '',
  condition: 'like_new' as ItemCondition,
  source: 'offline' as 'platform' | 'offline',
  notesTag: 'no_notes' as NotesTag,
  description: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请填写物品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请填写售价', trigger: 'blur' }],
  originalPrice: [{ required: true, message: '请填写原价', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }],
  description: [{ required: true, message: '请描述物品情况', trigger: 'blur' }]
}

const categoryOptions = computed(() => {
  const roots = categories.value.filter((category) => category.parentId === null)
  return roots.flatMap((root) =>
    categories.value
      .filter((child) => child.parentId === root.id)
      .map((child) => ({
        value: String(child.id),
        label: `${root.name} / ${child.name}`
      }))
  )
})

function onFileChange(file: UploadFile) {
  if (file.raw) {
    file.url = URL.createObjectURL(file.raw)
  }
  files.value = files.value.slice(-3)
}

function onExceed() {
  ElMessage.warning('最多上传 3 张图片')
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const item = await itemApi.create({
      title: form.title,
      categoryId: Number(form.categoryId),
      price: Number(form.price),
      originalPrice: Number(form.originalPrice),
      purchasePrice: form.purchasePrice ? Number(form.purchasePrice) : undefined,
      condition: form.condition,
      description: form.description,
      source: form.source,
      notesTag: form.notesTag,
      coverSeed: Math.floor(Math.random() * 900) + 20
    })
    ElMessage.success('已提交审核，通过后将自动上架')
    router.push(`/items/${item.id}`)
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
      eyebrow="PUBLISH"
      title="发布物品"
      subtitle="如实填写成色与来源。提交后由运营审核，通过后自动上架并写入物品履历。"
    >
      <template #actions>
        <el-button round @click="router.push('/my-items')">我的物品</el-button>
      </template>
    </PageHeader>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-card surface">
      <section class="block">
        <h2>基本信息</h2>
        <el-form-item label="物品标题" prop="title">
          <el-input v-model="form.title" size="large" maxlength="40" show-word-limit placeholder="品牌 / 型号 / 关键特征" />
        </el-form-item>

        <div class="two-col">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" size="large" placeholder="选择二级分类" class="full">
              <el-option v-for="option in categoryOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="成色" prop="condition">
            <el-select v-model="form.condition" size="large" class="full">
              <el-option v-for="option in conditionOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
        </div>

        <div class="three-col">
          <el-form-item label="售价（元）" prop="price">
            <el-input v-model="form.price" size="large" type="number" placeholder="0.00" />
          </el-form-item>
          <el-form-item label="原价（元）" prop="originalPrice">
            <el-input v-model="form.originalPrice" size="large" type="number" placeholder="0.00" />
          </el-form-item>
          <el-form-item label="入手价（元，选填）">
            <el-input v-model="form.purchasePrice" size="large" type="number" placeholder="0.00" />
          </el-form-item>
        </div>

        <div class="two-col">
          <el-form-item label="来源">
            <el-radio-group v-model="form.source">
              <el-radio value="offline">线下获得</el-radio>
              <el-radio value="platform">平台内购买</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="笔记资料">
            <el-radio-group v-model="form.notesTag">
              <el-radio value="no_notes">无笔记</el-radio>
              <el-radio value="with_notes">含笔记</el-radio>
              <el-radio value="with_answers">含答案</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </section>

      <section class="block">
        <h2>物品描述</h2>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="使用时长、磨损情况、配件是否齐全、交付偏好等"
          />
        </el-form-item>
      </section>

      <section class="block">
        <h2>物品图片</h2>
        <el-upload
          v-model:file-list="files"
          list-type="picture-card"
          :auto-upload="false"
          :limit="3"
          accept="image/*"
          :on-change="onFileChange"
          :on-exceed="onExceed"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p class="upload-hint">演示环境不会真正上传文件，提交后会用渐变色占位封面展示。</p>
      </section>

      <div class="submit-bar">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button type="primary" size="large" :loading="loading" @click="submit">
          提交审核
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.form-card {
  padding: 28px 30px;
}

.block {
  padding-bottom: 22px;
  margin-bottom: 22px;
  border-bottom: 1px dashed var(--sgj-line);
}

.block:last-of-type {
  border-bottom: 0;
}

.block h2 {
  margin: 0 0 16px;
  font-size: 17px;
}

.full {
  width: 100%;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.three-col {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.upload-hint {
  margin: 8px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
}

.submit-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

@media (max-width: 760px) {
  .form-card {
    padding: 20px;
  }

  .two-col,
  .three-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
