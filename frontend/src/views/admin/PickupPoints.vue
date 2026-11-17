<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { PickupPoint } from '@/types'

const points = ref<PickupPoint[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const dialogTitle = ref('新增自提点')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({ name: '', address: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入自提点名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    points.value = await adminApi.pickupPoints()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增自提点'
  form.name = ''
  form.address = ''
  dialogVisible.value = true
}

function openEdit(row: PickupPoint) {
  editingId.value = row.id
  dialogTitle.value = '编辑自提点'
  form.name = row.name
  form.address = row.address
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingId.value) {
      await adminApi.updatePickupPoint(editingId.value, { ...form })
      ElMessage.success('自提点已更新')
    } else {
      await adminApi.createPickupPoint({ ...form })
      ElMessage.success('自提点已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

async function remove(row: PickupPoint) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？被订单引用的自提点不可删除。`, '删除自提点', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await adminApi.deletePickupPoint(row.id)
    ElMessage.success('自提点已删除')
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
        <h2>自提点管理</h2>
        <p>自提点用于「自提」交付方式；被订单或交付记录引用的自提点不可删除。</p>
      </div>
      <el-button type="primary" round @click="openCreate">新增自提点</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="points" stripe>
        <el-table-column prop="id" label="编号" width="100" />
        <el-table-column prop="name" label="名称" min-width="220" />
        <el-table-column prop="address" label="地址" min-width="280" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" size="large" placeholder="如：菜鸟驿站（一食堂店）" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" size="large" placeholder="校内具体位置" />
        </el-form-item>
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
</style>
