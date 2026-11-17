<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import type { RosterEntry } from '@/types'

const roster = ref<RosterEntry[]>([])
const loading = ref(true)
const keyword = ref('')
const importVisible = ref(false)
const importing = ref(false)
const rawText = ref('')

async function load() {
  loading.value = true
  try {
    roster.value = await adminApi.roster(keyword.value.trim())
  } finally {
    loading.value = false
  }
}

function fillSample() {
  rawText.value = '20240007,沈知夏,经济管理学院,工商管理,2024\n20240008,顾清和,计算机学院,人工智能,2024'
}

function parseRows(): RosterEntry[] {
  return rawText.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [studentNo, name, college, major, grade] = line.split(',').map((part) => part.trim())
      return { studentNo, name, college, major, grade }
    })
    .filter((row) => row.studentNo && row.name)
}

async function doImport() {
  const rows = parseRows()
  if (!rows.length) {
    ElMessage.warning('没有可导入的数据，请按格式填写')
    return
  }
  importing.value = true
  try {
    const result = await adminApi.importRoster(rows)
    ElMessage.success(`已导入 ${result.count} 条学籍记录`)
    importVisible.value = false
    rawText.value = ''
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="surface toolbar">
      <div>
        <h2>学籍管理</h2>
        <p>学籍表是 F2 认证的比对源，修改会影响后续认证。运营与管理员账号没有学号。</p>
      </div>
      <el-button type="primary" round @click="importVisible = true">导入学籍</el-button>
    </div>

    <div class="surface filter">
      <el-input
        v-model="keyword"
        placeholder="搜索学号 / 姓名 / 学院 / 专业"
        clearable
        size="large"
        @keyup.enter="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button size="large" @click="load">搜索</el-button>
    </div>

    <div v-loading="loading" class="surface table-wrap">
      <el-table :data="roster" stripe>
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="college" label="学院" min-width="180" />
        <el-table-column prop="major" label="专业" min-width="180" />
        <el-table-column prop="grade" label="年级" width="100" />
      </el-table>
    </div>

    <el-dialog v-model="importVisible" title="导入学籍" width="560px">
      <p class="dialog-tip">
        每行一条，格式：学号,姓名,学院,专业,年级。学号已存在时会覆盖更新。
      </p>
      <el-input
        v-model="rawText"
        type="textarea"
        :rows="8"
        placeholder="20240001,张三,计算机学院,软件工程,2024"
      />
      <template #footer>
        <el-button @click="fillSample">填入示例</el-button>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">开始导入</el-button>
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

.filter {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.filter .el-input {
  max-width: 380px;
}

.table-wrap {
  padding: 8px 16px 16px;
}

.dialog-tip {
  margin: 0 0 12px;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.7;
}
</style>
