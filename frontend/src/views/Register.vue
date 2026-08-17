<template>
  <div class="register-page">
    <div class="bokeh-bg">
      <div v-for="i in 6" :key="i" class="bokeh-circle"
        :style="{
          width: (50 + i * 20) + 'px', height: (50 + i * 20) + 'px',
          left: (8 + i * 13) + '%', top: (8 + i * 11) + '%',
          animationDelay: (i * 0.8) + 's',
          opacity: 0.05 + i * 0.01
        }" />
    </div>

    <div class="viewfinder">
      <div class="viewfinder-inner">
        <div class="vf-corner vf-tl" />
        <div class="vf-corner vf-tr" />
        <div class="vf-corner vf-bl" />
        <div class="vf-corner vf-br" />

        <div class="register-card">
          <div class="brand">
            <h1>创建账号</h1>
            <p>加入 LifeArchive，珍藏你的人生故事</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
            <el-form-item prop="username" label="用户名">
              <el-input v-model="form.username" placeholder="3-20位用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="email" label="邮箱">
              <el-input v-model="form.email" placeholder="选填，用于找回密码" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="password" label="密码">
              <el-input v-model="form.password" type="password" placeholder="至少6位密码" :prefix-icon="Lock"
                show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword" label="确认密码">
              <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock"
                show-password @keyup.enter="handleRegister" />
            </el-form-item>
            <el-form-item>
              <el-button class="btn-primary register-btn" :loading="loading" @click="handleRegister" size="large">
                {{ loading ? '注册中...' : '注 册' }}
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button class="back-link" @click="$router.push('/login')" text>
                <el-icon><ArrowLeft /></el-icon> 已有账号？返回登录
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, ArrowLeft } from '@element-plus/icons-vue'
import { registerApi } from '@/api/user'

const router = useRouter(); const formRef = ref(); const loading = ref(false)
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: any) => {
  callback(value !== form.password ? new Error('两次输入的密码不一致') : undefined)
}
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return
  loading.value = true
  try {
    await registerApi({ username: form.username, password: form.password, email: form.email || undefined })
    ElMessage.success('注册成功！请登录'); router.push('/login')
  } catch (err: any) { ElMessage.error(err?.message || '注册失败，请重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px;
  background: linear-gradient(160deg, #FFF5EB 0%, #FDE8D0 30%, #FFF8F0 60%, #FFF0E5 100%);
  overflow: hidden; position: relative;
}
.bokeh-bg { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.bokeh-circle {
  position: absolute; border-radius: 50%;
  background: radial-gradient(circle, rgba(232,137,75,0.2) 0%, transparent 70%);
  animation: float-gentle 5s ease-in-out infinite;
}

.viewfinder {
  position: relative; z-index: 1; padding: 8px;
  border: 2px solid rgba(232, 137, 75, 0.2); border-radius: 24px;
  background: rgba(255,253,249,0.3); backdrop-filter: blur(4px);
}
.viewfinder-inner {
  position: relative; background: var(--bg-card); border-radius: 16px;
  padding: 4px; box-shadow: 0 4px 24px rgba(139, 90, 43, 0.08);
}
.vf-corner {
  position: absolute; width: 20px; height: 20px; z-index: 2;
  border-color: var(--color-primary); border-style: solid; opacity: 0.5;
}
.vf-tl { top: 12px; left: 12px; border-width: 2px 0 0 2px; border-radius: 4px 0 0 0; }
.vf-tr { top: 12px; right: 12px; border-width: 2px 2px 0 0; border-radius: 0 4px 0 0; }
.vf-bl { bottom: 12px; left: 12px; border-width: 0 0 2px 2px; border-radius: 0 0 0 4px; }
.vf-br { bottom: 12px; right: 12px; border-width: 0 2px 2px 0; border-radius: 0 0 4px 0; }

.register-card { width: 420px; padding: 40px 38px 32px; position: relative; z-index: 1; }
.brand { text-align: center; margin-bottom: 28px; }
.brand h1 { font-family: var(--font-display); font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.brand p { font-size: 13px; color: var(--text-muted); }

.register-btn { width: 100%; height: 46px !important; font-size: 16px !important; }
.back-link { width: 100%; color: var(--text-secondary); font-size: 13px; justify-content: center; }

:deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 500; }
:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px rgba(200, 160, 120, 0.15) inset;
  transition: box-shadow 0.15s ease;
}
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px rgba(232, 137, 75, 0.25) inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(232, 137, 75, 0.3) inset !important; }
</style>