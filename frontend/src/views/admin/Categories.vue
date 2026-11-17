<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { Category } from '@/types'

const categories = ref<Category[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  parentId: null as number | null
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    categories.value = await adminApi.categories()
  } finally {
    loading.value = false
  }
}

function openCreate(parent?: Category) {
  editingId.value = null
  dialogTitle.value = parent ? `新增「${parent.name}」子分类` : '新增一级分类'
  form.name = ''
  form.parentId = parent ? parent.id : null
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  form.name = row.name
  form.parentId = row.parentId
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingId.value) {
      await adminApi.updateCategory(editingId.value, { name: form.name })
      ElMessage.success('分类已更新')
    } else {
      await adminApi.createCategory({ name: form.name, parentId: form.parentId })
      ElMessage.success('分类已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

async function toggleEnabled(row: Category, value: string | number | boolean) {
  const enabled = Boolean(value)
  try {
    await adminApi.updateCategory(row.id, { enabled })
    ElMessage.success(enabled ? '分类已启用' : '分类已停用')
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
    await load()
  }
}

async function remove(row: Category) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除分类', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await adminApi.deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="surface toolbar">
      <div>
        <h2>分类管理</h2>
        <p>分类最多两级；被物品引用的分类不可删除，一级分类名不可重名。</p>
      </div>
      <el-button type="primary" round @click="openCreate()">新增一级分类</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="categories" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="分类名" min-width="220" />
        <el-table-column prop="id" label="分类号" width="100" />
        <el-table-column label="层级" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" round>{{ row.parentId === null ? '一级' : '二级' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="toggleEnabled(row, $event)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.parentId === null" size="small" @click="openCreate(row)">加子分类</el-button>
            <el-button size="small" @click="openEdit(row)">改名</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名" prop="name">
          <el-input v-model="form.name" size="large" maxlength="20" placeholder="如：考研专区" />
        </el-form-item>
        <p v-if="form.parentId" class="hint">将创建在所选一级分类下，分类最多两级。</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
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

.hint {
  margin: 0;
  color: var(--sgj-muted);
  font-size: 12px;
}
</style>
