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
        <span class="brand-text">
          <strong>拾光集</strong>
          <small>CAMPUS EXCHANGE</small>
        </span>
      </router-link>

      <div class="brand-copy">
        <p class="eyebrow">VOL. 01 / 2026 AUTUMN</p>
        <h1>让旧物<br />遇见<span class="accent">下一位主人</span></h1>
        <p class="lead">
          每件物品都有履历，每笔交易都有托管，每次履约都有信用记录。
        </p>
      </div>

      <div class="feature-row">
        <div class="feature">
          <span class="feature-num">I</span>
          <strong>履历</strong>
          <em>一物一码，转手可查</em>
        </div>
        <div class="feature">
          <span class="feature-num">II</span>
          <strong>托管</strong>
          <em>确认收货再放款</em>
        </div>
        <div class="feature">
          <span class="feature-num">III</span>
          <strong>信用</strong>
          <em>履约记录独立计分</em>
        </div>
      </div>

      <p class="brand-foot">EST. 2026 / CAMPUS ONLY / CURATED SECOND-HAND</p>
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
          <p class="demo-title">演示账号 / 密码均为 123456</p>
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
  min-height: 100dvh;
  grid-template-columns: 1.08fr 0.92fr;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 46px 56px 40px;
  overflow: hidden;
  background:
    radial-gradient(90% 100% at 82% 6%, rgba(216, 180, 119, 0.1), transparent 46%),
    var(--sgj-green-deep);
  color: #f4efe3;
}

.brand-panel::before {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.08'/%3E%3C/svg%3E");
  content: '';
  pointer-events: none;
}

.brand,
.brand-copy,
.feature-row,
.brand-foot {
  position: relative;
  z-index: 1;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid rgba(244, 239, 227, 0.4);
  background: transparent;
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: 20px;
  font-weight: 600;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-family: var(--sgj-font-display);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.brand-text small {
  margin-top: 3px;
  color: rgba(244, 239, 227, 0.45);
  font-size: 8px;
  letter-spacing: 0.24em;
}

.brand-copy {
  margin: auto 0;
  padding: 64px 0 48px;
}

.eyebrow {
  margin: 0 0 18px;
  color: rgba(216, 180, 119, 0.8);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.26em;
}

.brand-copy h1 {
  margin: 0;
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: clamp(42px, 4.2vw, 64px);
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.16;
}

.accent {
  color: #d8b477;
}

.lead {
  max-width: 26em;
  margin: 24px 0 0;
  color: rgba(244, 239, 227, 0.6);
  font-size: 13px;
  line-height: 2;
}

.feature-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding-top: 22px;
  border-top: 1px solid rgba(244, 239, 227, 0.16);
}

.feature {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.feature-num {
  color: rgba(216, 180, 119, 0.7);
  font-family: var(--sgj-font-display);
  font-size: 15px;
}

.feature strong {
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.feature em {
  color: rgba(244, 239, 227, 0.4);
  font-size: 9px;
  font-style: normal;
  letter-spacing: 0.08em;
}

.brand-foot {
  margin: 30px 0 0;
  color: rgba(244, 239, 227, 0.28);
  font-size: 8px;
  letter-spacing: 0.22em;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: var(--sgj-paper);
}

.form-card {
  width: min(380px, 100%);
}

.form-head h2 {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.hint {
  margin: 10px 0 30px;
  color: var(--sgj-muted);
  font-size: 12px;
  line-height: 1.8;
}

.form-card :deep(.el-input__wrapper) {
  padding: 4px 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  border-bottom: 1px solid var(--sgj-line-strong);
}

.form-card :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--sgj-ink);
  box-shadow: none;
}

.form-card :deep(.el-form-item__label) {
  color: var(--sgj-muted);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.submit {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  letter-spacing: 0.12em;
}

.demo {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--sgj-line);
}

.demo-title {
  margin: 0 0 10px;
  color: var(--sgj-faint);
  font-size: 9px;
  letter-spacing: 0.14em;
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
  background: transparent;
  color: var(--sgj-ink);
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.demo-item:hover {
  border-color: var(--sgj-green);
  background: var(--sgj-paper-deep);
}

.demo-item strong {
  font-size: 12px;
  font-weight: 500;
}

.demo-item span {
  color: var(--sgj-faint);
  font-size: 10px;
}

.switch {
  margin: 24px 0 0;
  color: var(--sgj-muted);
  font-size: 12px;
  text-align: center;
}

.switch a {
  color: var(--sgj-green);
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 4px;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 28px 24px 32px;
  }

  .brand-copy {
    padding: 40px 0 34px;
  }

  .brand-copy h1 {
    font-size: 38px;
  }

  .feature-row {
    display: none;
  }

  .brand-foot {
    display: none;
  }

  .form-panel {
    padding: 44px 24px 64px;
  }
}
</style>
