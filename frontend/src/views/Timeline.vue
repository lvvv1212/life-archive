<template>
  <div class="timeline-page">
    <div class="page-header">
      <h2>人生时间轴</h2>
      <p>回顾你走过的每一步</p>
    </div>

    <div class="year-filter-bar card-warm">
      <el-radio-group v-model="selectedYear" size="large" @change="onYear">
        <el-radio-button :value="0">全部</el-radio-button>
        <el-radio-button v-for="y in yearList" :key="y" :value="y">{{ y }}年</el-radio-button>
      </el-radio-group>
    </div>

    <div class="tl-wrap">
      <div v-show="!loading" style="min-height:200px">
        <el-empty v-if="!timeline.length" description="还没有记忆，去上传吧！">
          <el-button class="btn-primary" @click="$router.push('/upload')">上传记忆</el-button>
        </el-empty>

      <div v-for="yg in timeline" :key="yg.year" class="year-section">
        <div class="year-head">
          <div class="postmark">{{ yg.year }}</div>
          <el-tag effect="plain" size="small" style="background:transparent">{{ yg.count }} 条记忆</el-tag>
        </div>

        <div class="events-list">
          <!-- 缝线 SVG -->
          <svg class="stitch-svg" :style="{ height: (yg.events.length * 140 + 20) + 'px' }" preserveAspectRatio="none">
            <line x1="15" y1="0" x2="15" y2="100%" class="stitch-line" />
          </svg>

          <div v-for="ev in yg.events" :key="ev.memoryId" class="event-wrapper fade-scale-in">
            <div class="event-dot"><span></span></div>
            <div class="event-card card-warm" v-tilt @click="openDetail(ev)">
              <div class="ev-img" v-if="ev.image"><img :src="ev.image" :alt="ev.title" class="photo-develop" /></div>
              <div class="ev-body">
                <div class="ev-date">{{ ev.time }}</div>
                <h3>{{ ev.title }}</h3>
                <p v-if="ev.description">{{ truncate(ev.description,100) }}</p>
                <div class="ev-tags" v-if="ev.tags?.length">
                  <el-tag v-for="t in ev.tags.slice(0,4)" :key="t" size="small" effect="plain">{{ t.trim() }}</el-tag>
                </div>
                <div class="ev-meta">
                  <span v-if="ev.location">📍 {{ ev.location }}</span>
                  <span v-if="ev.emotion">{{ emj(ev.emotion) }} {{ ev.emotion }}</span>
                  <el-tag size="small" :type="tc(ev.memoryType)">{{ tl(ev.memoryType) }}</el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
      <DevelopingOverlay v-if="loading" text="正在冲洗时间轴…" />
    </div>

    <el-dialog v-model="dlg" :title="sel?.title" width="600px">
      <div v-if="sel">
        <div v-if="sel.image" class="dlg-img"><img :src="sel.image" /></div>
        <div class="detail-row"><span class="lbl">📅 时间</span><span>{{ sel.time }}</span></div>
        <div class="detail-row" v-if="sel.location"><span class="lbl">📍 地点</span><span>{{ sel.location }}</span></div>
        <div class="detail-row" v-if="sel.emotion"><span class="lbl">😊 情绪</span><span>{{ sel.emotion }}</span></div>
        <div class="detail-row" v-if="sel.description"><span class="lbl">📝 详情</span><span>{{ sel.description }}</span></div>
        <div class="detail-row" v-if="sel.aiSummary"><span class="lbl">🤖 AI摘要</span><span>{{ sel.aiSummary }}</span></div>
        <div style="margin-top:12px"><el-tag v-for="t in (sel.tags||[])" :key="t" style="margin:2px">{{ t.trim() }}</el-tag></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTimelineApi } from '@/api/timeline'
import DevelopingOverlay from '@/components/DevelopingOverlay.vue'
interface E { memoryId:number;time:string;month:number;title:string;description:string;memoryType:string;image:string|null;tags:string[];emotion:string;location:string;aiSummary:string }
interface G { year:number;count:number;events:E[] }
const timeline=ref<G[]>([]);const yearList=ref<number[]>([]);const selectedYear=ref(0);const loading=ref(false)
const dlg=ref(false);const sel=ref<E|null>(null)
onMounted(()=>fetchTl())
const fetchTl=async(y?:number)=>{
  loading.value=true
  try{const r:any=await getTimelineApi();const d:G[]=r.data||[]
    yearList.value=d.map(g=>g.year).sort((a,b)=>b-a)
    timeline.value=(y&&y>0?d.filter(g=>g.year===y):d).sort((a,b)=>b.year-a.year)}
  catch(e:any){ElMessage.error(e?.message)}
  finally{loading.value=false}
}
const onYear=(y:number)=>fetchTl(y||undefined)
const openDetail=(e:E)=>{sel.value=e;dlg.value=true}
const truncate=(t:string,n:number)=>t.length>n?t.slice(0,n)+'...':t
const emj=(e:string)=>({开心:'😊',感动:'🥹',平静:'😌',兴奋:'🤩',低落:'😢',温馨:'💕',思考:'🤔'}as any)[e]||''
const tl=(t:string)=>({photo:'照片',video:'视频',diary:'日记',travel:'旅行',study:'学习',daily:'日常'}as any)[t]||t
const tc=(t:string)=>({photo:'success',video:'warning',diary:'',travel:'danger',study:'info'}as any)[t]||'info'
</script>

<style scoped>
.timeline-page{max-width:900px;margin:0 auto}
.tl-wrap{position:relative}
.page-header{margin-bottom:20px}
.page-header h2{font-family:var(--font-display);font-size:20px;font-weight:700;color:var(--text-primary);margin-bottom:4px}
.page-header p{color:var(--text-muted);font-size:13px}
.year-filter-bar{display:flex;justify-content:center;padding:12px;margin-bottom:24px}

.year-section{margin-bottom:36px}
.year-head{display:flex;align-items:center;gap:14px;margin-bottom:20px;padding-left:6px}

/* 缝线时间轴 */
.events-list{position:relative;padding-left:40px}
.stitch-svg{position:absolute;left:0;top:0;width:30px;pointer-events:none}
.event-wrapper{position:relative;margin-bottom:16px}
.event-dot{
  position:absolute;left:-40px;top:20px;
  width:16px;height:16px;background:var(--bg-card);
  border:2px solid var(--color-primary);border-radius:50%;z-index:1;
  display:flex;align-items:center;justify-content:center;
}
.event-dot span{width:6px;height:6px;background:var(--color-primary);border-radius:50%}
.event-card{cursor:pointer;overflow:hidden;transition:all var(--transition-base)}
.event-card:hover{transform:translateX(4px);box-shadow:var(--shadow-md)}
.ev-img{height:160px;overflow:hidden;margin:-1px -1px 0 -1px}
.ev-img img{width:100%;height:100%;object-fit:cover}
.ev-body{padding:16px}
.ev-date{font-family:var(--font-hand);font-size:12px;color:var(--text-muted);margin-bottom:6px}
.ev-body h3{font-family:var(--font-display);font-size:16px;color:var(--text-primary);margin-bottom:6px}
.ev-body p{font-size:13px;color:var(--text-secondary);line-height:1.5;margin-bottom:10px}
.ev-tags{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:8px}
.ev-meta{display:flex;gap:10px;align-items:center;font-size:13px;color:var(--text-secondary);flex-wrap:wrap}
.dlg-img{margin-bottom:16px}
.dlg-img img{max-width:100%;border-radius:var(--radius-md)}
.detail-row{display:flex;gap:12px;padding:6px 0;border-bottom:1px solid rgba(139,90,43,0.04)}
.lbl{color:var(--text-muted);min-width:70px}
:deep(.el-radio-button__inner){border:none!important}
:deep(.el-radio-button.is-active .el-radio-button__inner){background:var(--gradient-warm)!important;color:white!important;box-shadow:none!important}
</style>