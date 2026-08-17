<template>
  <div class="home-page stagger-in" v-if="ready">
    <!-- 欢迎横幅：暖色渐变 -->
    <div class="welcome-banner grain-overlay">
      <div class="welcome-ambient">
        <ParticleField :count="26" color="255, 248, 240" :opacity="0.6" :speed="0.18" />
      </div>
      <div class="welcome-text">
        <h1>{{ greeting }}，{{ username }}</h1>
        <p>每一天都值得被珍藏，每一刻都在创造记忆</p>
      </div>
      <div class="welcome-deco">
        <svg width="130" height="90" viewBox="0 0 130 90" class="spin-slow" style="opacity:0.7">
          <circle cx="25" cy="45" r="18" fill="rgba(255,255,255,0.1)"/>
          <circle cx="65" cy="28" r="22" fill="rgba(255,255,255,0.07)"/>
          <circle cx="105" cy="50" r="14" fill="rgba(255,255,255,0.12)"/>
          <rect v-for="i in 7" :key="i" :x="18+(i-1)*12" :y="26+Math.sin(i)*16" width="3" :height="14+Math.abs(Math.sin(i*2))*14" rx="1.5" fill="rgba(255,255,255,0.3)"/>
        </svg>
      </div>
    </div>

    <!-- 拍立得统计卡片 -->
    <div class="polaroid-row">
      <div class="polaroid-card card-polaroid" style="--tilt:-1.5deg" @click="$router.push('/memories')">
        <div class="pol-icon-wrap"><span class="pol-icon">📚</span></div>
        <div class="pol-num count-anim">{{ stats.total }}</div>
        <div class="pol-label">记忆总数</div>
      </div>
      <div class="polaroid-card card-polaroid" style="--tilt:1.2deg" @click="$router.push('/timeline')">
        <div class="pol-icon-wrap"><span class="pol-icon">📅</span></div>
        <div class="pol-num count-anim">{{ stats.timeline || stats.total }}</div>
        <div class="pol-label">时间节点</div>
      </div>
      <div class="polaroid-card card-polaroid" style="--tilt:-0.8deg" @click="$router.push('/upload')">
        <div class="pol-icon-wrap"><span class="pol-icon">📷</span></div>
        <div class="pol-num count-anim">+</div>
        <div class="pol-label">记录新记忆</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="section-label">快捷操作</div>
    <div class="quick-actions">
      <div class="action-card card-warm warm-shine" @click="$router.push('/upload')">
        <div class="ac-icon"><span>📤</span></div>
        <div class="ac-text"><h4>上传记忆</h4><span>添加照片、视频或日记</span></div>
        <svg class="ac-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </div>
      <div class="action-card card-warm warm-shine" @click="$router.push('/story')">
        <div class="ac-icon"><span>✍️</span></div>
        <div class="ac-text"><h4>AI 回忆生成</h4><span>生成你的人生故事</span></div>
        <svg class="ac-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </div>
    </div>

    <!-- 最近记忆 -->
    <div class="section-label" v-if="recent.length">最近记忆</div>
    <div class="recent-row" v-if="recent.length">
      <div v-for="(item, i) in recent" :key="item.id" class="recent-card card-polaroid"
        :style="{ '--tilt': (i - 1) * 0.8 + 'deg' }" @click="$router.push('/memories')">
        <div class="pol-img-wrap" v-if="item.fileUrl">
          <img :src="item.fileUrl" :alt="item.title" class="polaroid-img photo-develop" />
        </div>
        <div class="pol-img-wrap pol-placeholder" v-else>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <div class="polaroid-caption">{{ item.title }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMemoryStatsApi, getMemoryListApi } from '@/api/memory'
import { getUser } from '@/utils/auth'
import ParticleField from '@/components/ParticleField.vue'

const username = computed(() => getUser()?.username || '用户')
const ready = ref(false)
const greeting = computed(() => { const h = new Date().getHours(); return h < 6 ? '夜深了' : h < 9 ? '早上好' : h < 12 ? '上午好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好' })
const stats = ref({ total: 0, photos: 0, timeline: 0 })
const recent = ref<any[]>([])

onMounted(async () => {
  try { const r: any = await getMemoryStatsApi(); stats.value = { total: r.data?.total || 0, photos: 0, timeline: 0 } } catch {}
  try { const r: any = await getMemoryListApi({ page: 1, size: 3 }); recent.value = (r.data || []).map((m: any) => ({ id: m.id, title: m.title, fileUrl: m.fileUrl, date: new Date(m.createdAt).toLocaleDateString('zh-CN') })) } catch {}
  ready.value = true
})
</script>

<style scoped>
.home-page { max-width: 960px; }

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #E8894B 0%, #C56E38 50%, #D4956B 100%);
  border-radius: var(--radius-xl); padding: 28px 32px;
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 28px; color: #fff; overflow: hidden; position: relative;
}
.welcome-ambient { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; border-radius: inherit; }
.welcome-banner::before {
  content: ''; position: absolute; top: -50%; right: -15%;
  width: 180px; height: 180px; background: rgba(255,255,255,0.05); border-radius: 50%;
  animation: float-gentle 6s ease-in-out infinite; pointer-events: none;
}
.welcome-text { position: relative; z-index: 1; }
.welcome-text h1 { font-family: var(--font-display); font-size: 22px; font-weight: 700; margin-bottom: 6px; }
.welcome-text p { font-size: 13px; opacity: 0.85; }
.welcome-deco { flex-shrink: 0; position: relative; z-index: 1; }

/* 拍立得卡片行 */
.polaroid-row {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px;
  margin-bottom: 28px;
}
.polaroid-card {
  cursor: pointer; text-align: center; padding: 20px 12px 24px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.pol-icon-wrap { margin-bottom: 4px; }
.pol-icon { font-size: 28px; }
.pol-num { font-family: var(--font-display); font-size: 32px; font-weight: 700; color: var(--text-ink); line-height: 1.2; }
.pol-label { font-family: var(--font-hand); font-size: 13px; color: var(--text-muted); }

/* 分区标签 */
.section-label {
  font-family: var(--font-display); font-size: 15px; font-weight: 600;
  color: var(--text-primary); margin-bottom: 12px;
  display: flex; align-items: center; gap: 8px;
}
.section-label::after {
  content: ''; flex: 1; height: 1px;
  background: linear-gradient(90deg, rgba(200,160,120,0.2), transparent);
}

/* 快捷操作 */
.quick-actions { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 28px; }
.action-card { display: flex; align-items: center; gap: 14px; padding: 18px 20px; cursor: pointer; overflow: hidden; }
.ac-icon { width: 46px; height: 46px; border-radius: var(--radius-md); background: rgba(232,137,75,0.06); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ac-icon span { font-size: 22px; }
.ac-text h4 { font-size: 15px; color: var(--text-primary); margin-bottom: 2px; }
.ac-text span { font-size: 12px; color: var(--text-muted); }
.ac-arrow { margin-left: auto; transition: transform 0.2s; flex-shrink: 0; }
.action-card:hover .ac-arrow { transform: translateX(3px); }

/* 最近记忆 */
.recent-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.recent-card { cursor: pointer; }
.pol-img-wrap { width: 100%; aspect-ratio: 4/3; border-radius: 2px; overflow: hidden; background: rgba(232,137,75,0.04); }
.pol-img-wrap img { width: 100%; height: 100%; object-fit: cover; }
.pol-placeholder { display: flex; align-items: center; justify-content: center; }
</style>