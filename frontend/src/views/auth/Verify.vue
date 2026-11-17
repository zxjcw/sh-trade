<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  studentNo: auth.user?.studentNo ?? '',
  name: auth.user?.nickname ?? '',
  college: auth.user?.college ?? '',
  major: auth.user?.major ?? '',
  grade: auth.user?.grade ?? ''
})

const rules: FormRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }]
}

const verified = computed(() => auth.user?.verifyStatus === 'verified')

function fillDemo() {
  form.studentNo = '20230001'
  form.name = '林小满'
  form.college = '计算机学院'
  form.major = '软件工程'
  form.grade = '2023'
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const user = await userApi.verify(form)
    auth.setUser(user)
    ElMessage.success('学籍认证通过，已获得交易资格')
    const redirect = route.query.redirect as string | undefined
    router.push(redirect || '/')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page page-narrow">
    <div v-if="verified" class="verified surface">
      <span class="badge">
        <el-icon><CircleCheckFilled /></el-icon>
      </span>
      <h1>已完成学籍认证</h1>
      <p>你现在可以发布物品、下单购买、发布求购和发起置换。</p>
      <el-descriptions :column="2" border class="info">
        <el-descriptions-item label="学号">{{ auth.user?.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ auth.user?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ auth.user?.college }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ auth.user?.major }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ auth.user?.grade }}</el-descriptions-item>
        <el-descriptions-item label="信用分">{{ auth.user?.creditScore }}</el-descriptions-item>
      </el-descriptions>
      <div class="actions">
        <el-button type="primary" round @click="router.push('/')">去逛逛</el-button>
        <el-button round @click="router.push('/publish')">发布物品</el-button>
      </div>
    </div>

    <div v-else class="verify-layout">
      <section class="intro">
        <p class="eyebrow">STUDENT VERIFICATION</p>
        <h1>学籍认证</h1>
        <p class="lead">
          平台只面向本校学生开放交易。认证信息会与校方学籍名册比对，通过后绑定学号并获得发布、下单、求购、置换资格。
        </p>
        <ul class="rules">
          <li>一个学号只能绑定一个账号</li>
          <li>每次提交独立留档，不覆盖历史记录</li>
          <li>未认证账号仅可浏览，不能交易</li>
        </ul>
        <div class="demo-tip">
          <strong>演示提示</strong>
          <span>点击「填入演示学籍」可使用 20230001 / 林小满 通过校验。</span>
        </div>
      </section>

      <section class="form-card surface">
        <div class="form-head">
          <h2>填写学籍信息</h2>
          <el-button link type="primary" @click="fillDemo">填入演示学籍</el-button>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="学号" prop="studentNo">
            <el-input v-model="form.studentNo" size="large" placeholder="如 20230001" />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" size="large" placeholder="与学籍名册一致" />
          </el-form-item>
          <el-form-item label="学院" prop="college">
            <el-input v-model="form.college" size="large" />
          </el-form-item>
          <div class="two-col">
            <el-form-item label="专业" prop="major">
              <el-input v-model="form.major" size="large" />
            </el-form-item>
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" size="large" />
            </el-form-item>
          </div>
          <el-button class="submit" type="primary" size="large" :loading="loading" @click="submit">
            提交认证
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.verify-layout {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 30px;
  align-items: start;
  padding-top: 18px;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--sgj-accent-deep);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
}

.intro h1 {
  margin: 0;
  font-size: 34px;
  letter-spacing: 2px;
}

.lead {
  margin: 16px 0 22px;
  color: var(--sgj-muted);
  font-size: 14px;
  line-height: 1.9;
}

.rules {
  margin: 0;
  padding-left: 18px;
  color: #5c675f;
  font-size: 13px;
  line-height: 2.1;
}

.demo-tip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 24px;
  padding: 14px 16px;
  border: 1px dashed rgba(201, 138, 46, 0.4);
  border-radius: 12px;
  background: #fbf5e8;
  font-size: 12px;
}

.demo-tip strong {
  color: var(--sgj-accent-deep);
}

.demo-tip span {
  color: #8a7b60;
}

.form-card {
  padding: 28px 30px;
}

.form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.form-head h2 {
  margin: 0;
  font-size: 20px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.verified {
  max-width: 620px;
  margin: 40px auto;
  padding: 40px;
  text-align: center;
}

.badge {
  display: inline-grid;
  width: 64px;
  height: 64px;
  place-items: center;
  border-radius: 50%;
  background: var(--sgj-primary-soft);
  color: var(--sgj-primary);
  font-size: 34px;
}

.verified h1 {
  margin: 16px 0 6px;
  font-size: 26px;
}

.verified p {
  margin: 0 0 24px;
  color: var(--sgj-muted);
  font-size: 14px;
}

.info {
  text-align: left;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 26px;
}

@media (max-width: 820px) {
  .verify-layout {
    grid-template-columns: 1fr;
  }

  .form-card {
    padding: 22px 20px;
  }

  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
