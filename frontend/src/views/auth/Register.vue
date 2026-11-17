<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirm: '',
  contact: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名 3–20 个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      contact: form.contact
    })
    ElMessage.success('注册成功，完成学籍认证后即可交易')
    router.push('/verify')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="panel">
      <router-link class="back" to="/login">
        <el-icon><ArrowLeft /></el-icon>
        返回登录
      </router-link>

      <div class="head">
        <span class="mark">拾</span>
        <h1>加入拾光集</h1>
        <p>注册后需完成学籍认证，才能发布、下单、求购和置换。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="登录账号，注册后不可修改" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" size="large" placeholder="展示给其他同学的名字" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirm">
            <el-input v-model="form.confirm" size="large" type="password" show-password />
          </el-form-item>
        </div>
        <el-form-item label="联系方式（选填）">
          <el-input v-model="form.contact" size="large" placeholder="手机号 / 微信号" />
        </el-form-item>
        <el-button class="submit" type="primary" size="large" :loading="loading" @click="submit">
          注册并前往认证
        </el-button>
      </el-form>

      <p class="note">
        注册即表示同意平台规则：只交易合法物品，如实描述成色，按约定完成交付。
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background:
    radial-gradient(circle at 15% 10%, rgba(47, 107, 79, 0.1), transparent 32%),
    radial-gradient(circle at 88% 90%, rgba(201, 138, 46, 0.1), transparent 34%),
    var(--sgj-bg);
}

.panel {
  width: min(520px, 100%);
  padding: 36px 38px 30px;
  border: 1px solid var(--sgj-line);
  border-radius: 2px;
  background: var(--sgj-surface);
}

.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--sgj-muted);
  font-size: 13px;
}

.head {
  margin: 26px 0 26px;
  text-align: center;
}

.mark {
  display: grid;
  width: 46px;
  height: 46px;
  margin: 0 auto 14px;
  place-items: center;
  border: 1px solid var(--sgj-green-deep);
  border-radius: 1px;
  background: var(--sgj-green);
  color: #f4efe3;
  font-family: var(--sgj-font-display);
  font-size: 22px;
  font-weight: 600;
}

.head h1 {
  margin: 0;
  font-family: var(--sgj-font-display);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.head p {
  margin: 8px 0 0;
  color: var(--sgj-muted);
  font-size: 13px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.note {
  margin: 18px 0 0;
  color: #a0a6a1;
  font-size: 11px;
  line-height: 1.7;
  text-align: center;
}

@media (max-width: 560px) {
  .panel {
    padding: 28px 22px;
  }

  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
