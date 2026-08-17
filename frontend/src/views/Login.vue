<template>
  <div class="login-page">
    <!-- 背景光斑（模拟大光圈虚化） -->
    <div class="bokeh-bg">
      <div v-for="i in 8" :key="i" class="bokeh-circle"
        :style="{
          width: (60 + i * 25) + 'px', height: (60 + i * 25) + 'px',
          left: (10 + i * 11) + '%', top: (5 + i * 10) + '%',
          animationDelay: (i * 0.7) + 's',
          opacity: 0.06 + i * 0.01
        }" />
    </div>

    <!-- 取景框 -->
    <div class="viewfinder">
      <div class="viewfinder-inner">
        <!-- 四角标记 -->
        <div class="vf-corner vf-tl" />
        <div class="vf-corner vf-tr" />
        <div class="vf-corner vf-bl" />
        <div class="vf-corner vf-br" />

        <div class="login-card">
          <div class="login-brand">
            <div class="brand-icon-wrap float">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <rect width="48" height="48" rx="10" fill="url(#lgg)"/>
                <path d="M14 20L24 14L34 20V34C34 36.2 32.2 38 30 38H18C15.8 38 14 36.2 14 34V20Z" fill="#FFFDF9" opacity="0.95"/>
                <circle cx="22" cy="26" r="4" fill="url(#lgg)"/>
                <defs><linearGradient id="lgg" x1="0" y1="0" x2="48" y2="48"><stop stop-color="#E8894B"/><stop offset="1" stop-color="#D4956B"/></linearGradient></defs>
              </svg>
            </div>
            <h1 class="brand-name">LifeArchive</h1>
            <p class="brand-tagline">珍藏每一刻 · 温暖一生</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
            <el-form-item prop="username" label="用户名">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password" label="密码">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock"
                show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button class="btn-primary login-btn" :loading="loading" @click="handleLogin" size="large">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button class="register-link-btn" @click="$router.push('/register')" text>
                还没有账号？<span class="link-highlight">立即注册</span>
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <p class="login-footer">基于AI的个人数字记忆管理平台</p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginApi } from '@/api/user'
import { setToken, setUser } from '@/utils/auth'

const router = useRouter(); const formRef = ref(); const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  loading.value = true
  try {
    const res: any = await loginApi({ username: form.username, password: form.password })
    setToken(res.data.token); setUser({ userId: 0, username: form.username })
    ElMessage.success('欢迎回来！'); router.push('/home')
  } catch (err: any) { ElMessage.error(err?.message || '登录失败，请重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 24px;
  background: linear-gradient(160deg, #FFF5EB 0%, #FDE8D0 30%, #FFF8F0 60%, #FFF0E5 100%);
  overflow: hidden; position: relative;
}

/* 光斑背景 */
.bokeh-bg { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.bokeh-circle {
  position: absolute; border-radius: 50%;
  background: radial-gradient(circle, rgba(232,137,75,0.25) 0%, transparent 70%);
  animation: float-gentle 5s ease-in-out infinite;
}

/* 取景框 */
.viewfinder {
  position: relative; z-index: 1;
  padding: 8px;
  border: 2px solid rgba(232, 137, 75, 0.2);
  border-radius: 24px;
  background: rgba(255,253,249,0.3);
  backdrop-filter: blur(4px);
}
.viewfinder-inner {
  position: relative;
  background: var(--bg-card);
  border-radius: 16px;
  padding: 4px;
  box-shadow: 0 4px 24px rgba(139, 90, 43, 0.08);
}
/* 取景框四角 */
.vf-corner {
  position: absolute; width: 20px; height: 20px; z-index: 2;
  border-color: var(--color-primary); border-style: solid;
  opacity: 0.5;
}
.vf-tl { top: 12px; left: 12px; border-width: 2px 0 0 2px; border-radius: 4px 0 0 0; }
.vf-tr { top: 12px; right: 12px; border-width: 2px 2px 0 0; border-radius: 0 4px 0 0; }
.vf-bl { bottom: 12px; left: 12px; border-width: 0 0 2px 2px; border-radius: 0 0 0 4px; }
.vf-br { bottom: 12px; right: 12px; border-width: 0 2px 2px 0; border-radius: 0 0 4px 0; }

.login-card {
  width: 400px; padding: 44px 40px 32px;
  position: relative; z-index: 1;
}
.login-brand { text-align: center; margin-bottom: 28px; }
.brand-icon-wrap { display: inline-flex; margin-bottom: 12px; }
.brand-name {
  font-family: var(--font-display); font-size: 28px; font-weight: 700;
  background: var(--gradient-warm);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 4px;
}
.brand-tagline { font-size: 13px; color: var(--text-muted); letter-spacing: 2px; }

.login-btn { width: 100%; height: 46px !important; font-size: 16px !important; }

.register-link-btn { width: 100%; color: var(--text-secondary); font-size: 13px; }
.link-highlight { color: var(--color-primary); font-weight: 700; margin-left: 4px; }

.login-footer { margin-top: 28px; color: var(--text-muted); font-size: 12px; z-index: 1; letter-spacing: 1px; }

:deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 500; }
:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px rgba(200, 160, 120, 0.15) inset;
  transition: box-shadow 0.15s ease;
}
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px rgba(232, 137, 75, 0.25) inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(232, 137, 75, 0.3) inset !important; }
</style>