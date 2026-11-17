<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: 'student',
  password: '123456'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demos = [
  { label: '林小满', username: 'student', desc: '学生 · 已认证' },
  { label: '陈屿', username: 'seller', desc: '学生 · 卖家视角' },
  { label: '运营小周', username: 'operator', desc: '审核 / 仲裁 / 报表' },
  { label: '校方管理员', username: 'admin', desc: '学籍 / 分类 / 自提点' }
]

function fill(username: string) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const user = await auth.login(form.username, form.password)
    ElMessage.success(`欢迎回来，${user.nickname}`)
    const redirect = route.query.redirect as string | undefined
    if (redirect) {
      router.push(redirect)
    } else if (user.role === 'admin') {
      router.push('/admin/categories')
    } else if (user.role === 'operator') {
      router.push('/ops/reviews')
    } else {
      router.push('/')
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="brand-panel">
      <router-link class="brand" to="/">
        <span class="brand-mark">拾</span>
        <span>拾光集</span>
      </router-link>

      <div class="brand-copy">
        <p class="eyebrow">CAMPUS MARKETPLACE</p>
        <h1>让旧物<br />遇见下一位主人</h1>
        <p class="lead">
          每件物品都有履历，每笔交易都有托管，每次履约都有信用记录。
        </p>
      </div>

      <div class="feature-row">
        <div class="feature">
          <strong>履历</strong>
          <span>一物一码，转手可查</span>
        </div>
        <div class="feature">
          <strong>托管</strong>
          <span>确认收货再放款</span>
        </div>
        <div class="feature">
          <strong>信用</strong>
          <span>履约记录独立计分</span>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <div class="form-head">
          <p class="eyebrow">WELCOME BACK</p>
          <h2>登录拾光集</h2>
          <p class="hint">使用校园账号登录，未认证用户仅可浏览。</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button class="submit" type="primary" size="large" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>

        <div class="demo">
          <p class="demo-title">演示账号（密码均为 123456，点击填充）</p>
          <div class="demo-grid">
            <button
              v-for="demo in demos"
              :key="demo.username"
              type="button"
              class="demo-item"
              @click="fill(demo.username)"
            >
              <strong>{{ demo.label }}</strong>
              <span>{{ demo.desc }}</span>
            </button>
          </div>
        </div>

        <p class="switch">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 1.05fr 1fr;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 44px 56px;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 12%, rgba(232, 217, 184, 0.24), transparent 38%),
    radial-gradient(circle at 12% 88%, rgba(201, 138, 46, 0.18), transparent 40%),
    linear-gradient(150deg, #1d3a2b 0%, #2f6b4f 55%, #3f7f5e 100%);
  color: #f4efe3;
}

.brand-panel::after {
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    115deg,
    rgba(255, 255, 255, 0.035) 0,
    rgba(255, 255, 255, 0.035) 1px,
    transparent 1px,
    transparent 16px
  );
  content: '';
  pointer-events: none;
}

.brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  background: #e8d9b8;
  color: #1d3a2b;
  font-size: 22px;
  font-weight: 700;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin: auto 0;
}

.eyebrow {
  margin: 0 0 14px;
  color: rgba(244, 239, 227, 0.62);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 4px;
}

.brand-copy h1 {
  margin: 0;
  font-size: clamp(40px, 4.4vw, 64px);
  font-weight: 600;
  letter-spacing: 2px;
  line-height: 1.22;
}

.lead {
  max-width: 420px;
  margin: 22px 0 0;
  color: rgba(244, 239, 227, 0.72);
  font-size: 15px;
  line-height: 1.9;
}

.feature-row {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 32px;
  padding-top: 26px;
  border-top: 1px solid rgba(244, 239, 227, 0.16);
}

.feature {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature strong {
  color: #e8d9b8;
  font-size: 16px;
  letter-spacing: 2px;
}

.feature span {
  color: rgba(244, 239, 227, 0.55);
  font-size: 12px;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: var(--sgj-bg);
}

.form-card {
  width: min(420px, 100%);
}

.form-head h2 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 1px;
}

.hint {
  margin: 8px 0 28px;
  color: var(--sgj-muted);
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.demo {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed var(--sgj-line);
}

.demo-title {
  margin: 0 0 10px;
  color: var(--sgj-muted);
  font-size: 12px;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.demo-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 9px 11px;
  border: 1px solid var(--sgj-line);
  border-radius: 10px;
  background: var(--sgj-surface);
  color: var(--sgj-ink);
  cursor: pointer;
  text-align: left;
  transition: all 0.16s ease;
}

.demo-item:hover {
  border-color: var(--sgj-primary);
  background: var(--sgj-primary-soft);
}

.demo-item strong {
  font-size: 13px;
}

.demo-item span {
  color: var(--sgj-muted);
  font-size: 11px;
}

.switch {
  margin: 22px 0 0;
  color: var(--sgj-muted);
  font-size: 13px;
  text-align: center;
}

.switch a {
  color: var(--sgj-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 28px 24px 36px;
  }

  .brand-copy {
    margin: 36px 0;
  }

  .brand-copy h1 {
    font-size: 38px;
  }

  .feature-row {
    display: none;
  }
}
</style>
