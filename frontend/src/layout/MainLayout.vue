<template>
  <el-container class="main-layout">
    <!-- 侧边栏：胡桃木色 -->
    <el-aside width="230px" class="main-sidebar">
      <div class="sidebar-logo" @click="$router.push('/home')">
        <svg width="36" height="36" viewBox="0 0 48 48" fill="none" class="float">
          <rect width="48" height="48" rx="10" fill="url(#sg3)"/>
          <path d="M14 20L24 14L34 20V34C34 36.2 32.2 38 30 38H18C15.8 38 14 36.2 14 34V20Z" fill="#FFFDF9" opacity="0.95"/>
          <circle cx="22" cy="26" r="4" fill="url(#sg3)"/>
          <defs><linearGradient id="sg3" x1="0" y1="0" x2="48" y2="48"><stop stop-color="#E8894B"/><stop offset="1" stop-color="#D4956B"/></linearGradient></defs>
        </svg>
        <span class="logo-text">LifeArchive</span>
      </div>

      <div class="sidebar-divider" />

      <el-menu
        :default-active="activeMenu" router
        :background-color="'transparent'"
        :text-color="'#C4A882'" :active-text-color="'#E8894B'"
        class="sidebar-menu"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>
        <el-menu-item index="/memories">
          <el-icon><Collection /></el-icon><span>记忆列表</span>
        </el-menu-item>
        <el-menu-item index="/upload">
          <el-icon><Upload /></el-icon><span>上传记忆</span>
        </el-menu-item>
        <el-menu-item index="/timeline">
          <el-icon><Clock /></el-icon><span>时间轴</span>
        </el-menu-item>
        <el-menu-item index="/assistant">
          <el-icon><ChatDotRound /></el-icon><span>AI助手</span>
        </el-menu-item>
        <el-menu-item index="/story">
          <el-icon><Edit /></el-icon><span>回忆生成</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon><span>数据分析</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-avatar">{{ username.charAt(0).toUpperCase() }}</div>
          <span class="user-name">{{ username }}</span>
        </div>
        <el-button class="logout-btn" text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header class="main-header">
        <span class="header-title">{{ pageTitle }}</span>
      </el-header>
      <el-main class="main-content grain-overlay">
        <div class="orb orb-1" />
        <div class="orb orb-2" />
        <div class="ambient-layer">
          <ParticleField :count="16" :opacity="0.35" :speed="0.16" />
        </div>
        <div class="content-layer"><router-view /></div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Collection, Upload, Clock, ChatDotRound, Edit, DataAnalysis, SwitchButton } from '@element-plus/icons-vue'
import { getUser, removeToken } from '@/utils/auth'
import ParticleField from '@/components/ParticleField.vue'

const route = useRoute(); const router = useRouter()
const activeMenu = computed(() => route.path)
const username = computed(() => getUser()?.username || '用户')
const pageTitle = computed(() => route.meta.title as string || '')

const handleLogout = () => { removeToken(); ElMessage.success('已退出登录'); router.push('/login') }
</script>

<style scoped>
.main-layout { height: 100vh; }

.main-sidebar {
  background: linear-gradient(180deg, #3D2B1F 0%, #2D1F15 50%, #1F140E 100%);
  display: flex; flex-direction: column; overflow-y: auto;
  position: relative;
}
/* 木纹纹理 */
.main-sidebar::before {
  content: '';
  position: absolute; inset: 0;
  opacity: 0.04;
  background: repeating-linear-gradient(
    90deg,
    transparent, transparent 3px,
    rgba(255,255,255,0.1) 3px, rgba(255,255,255,0.1) 4px
  );
  pointer-events: none;
}
.sidebar-logo { display: flex; align-items: center; gap: 10px; padding: 20px 18px; cursor: pointer; position: relative; z-index: 1; }
.logo-text { color: #F5E6D3; font-family: var(--font-display); font-size: 17px; font-weight: 700; letter-spacing: 0.5px; }
.sidebar-divider { height: 1px; margin: 0 16px 8px; background: linear-gradient(90deg, transparent, rgba(212,184,150,0.2), transparent); }

.sidebar-menu { flex: 1; border-right: none !important; padding: 4px 10px; position: relative; z-index: 1; }
:deep(.sidebar-menu .el-menu-item) {
  height: 44px; line-height: 44px; margin: 2px 0; border-radius: 8px;
  font-size: 14px; position: relative; overflow: hidden;
  transition: all 0.25s ease;
}
:deep(.sidebar-menu .el-menu-item::before) {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0;
  width: 3px; background: var(--gradient-warm); border-radius: 0 3px 3px 0;
  transform: scaleX(0); transition: transform 0.3s ease;
}
:deep(.sidebar-menu .el-menu-item:hover) {
  background: rgba(232, 137, 75, 0.1) !important;
  color: #F0C090 !important; transform: translateX(2px);
}
:deep(.sidebar-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(232,137,75,0.22), rgba(212,149,107,0.1)) !important;
  color: #E8894B !important; font-weight: 600;
}
:deep(.sidebar-menu .el-menu-item.is-active::before) { transform: scaleX(1); }
:deep(.sidebar-menu .el-menu-item .el-icon) { font-size: 18px; }

.sidebar-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border-top: 1px solid rgba(255,255,255,0.05);
  position: relative; z-index: 1;
}
.user-info { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--gradient-warm); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display); font-weight: 700; font-size: 14px;
}
.user-name { color: #D4B896; font-size: 13px; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout-btn { color: rgba(255,255,255,0.25) !important; padding: 6px !important; transition: color 0.2s; }
.logout-btn:hover { color: #E8894B !important; }

.main-header {
  background: rgba(255, 253, 249, 0.85); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(200, 160, 120, 0.12);
  display: flex; align-items: center; padding: 0 28px; height: 56px;
}
.header-title { font-family: var(--font-display); font-size: 15px; font-weight: 600; color: var(--text-primary); letter-spacing: 0.5px; }

.main-content { background: var(--bg-base); padding: 24px; overflow-y: auto; position: relative; }
.ambient-layer { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.content-layer { position: relative; z-index: 1; }
</style>