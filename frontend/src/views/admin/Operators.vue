<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { User } from '@/types'
import { formatDate } from '@/utils/format'

const operators = ref<User[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  nickname: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    operators.value = await adminApi.operators()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.username = ''
  form.nickname = ''
  form.password = ''
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await adminApi.createOperator({ ...form })
    ElMessage.success('运营账号已创建')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="surface toolbar">
      <div>
        <h2>运营账号</h2>
        <p>运营负责物品审核、仲裁与报表，不参与学籍维护；管理员负责创建运营账号。</p>
      </div>
      <el-button type="primary" round @click="openCreate">创建运营账号</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="operators" stripe>
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="nickname" label="昵称" width="160" />
        <el-table-column label="角色" width="120">
          <template #default>
            <el-tag type="warning" effect="light" round size="small">运营</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="创建运营账号" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="展示名称" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
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
