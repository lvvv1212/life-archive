<template>
  <div class="memories-page">
    <div class="filter-bar card-warm">
      <el-select v-model="filterType" placeholder="全部类型" clearable @change="fetchList" style="width:140px">
        <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input v-model="searchKey" placeholder="搜索记忆..." :prefix-icon="Search" @keyup.enter="fetchList" style="width:220px" />
      <span class="total-text">共 {{ memories.length }} 条</span>
      <el-button class="btn-primary" @click="$router.push('/upload')">
        <el-icon><Plus /></el-icon> 新增
      </el-button>
    </div>

    <div class="memory-grid-wrap">
      <div class="memory-grid" v-show="!loading">
        <el-empty v-if="!memories.length" description="还没有记忆，快去添加吧！" />
      <div v-for="(item, i) in memories" :key="item.id" class="memory-card card-polaroid polaroid-in" v-tilt
        :style="{ '--tilt': (i % 3 - 1) * 1.2 + 'deg', animationDelay: (i * 0.06) + 's' }"
        @click="showDetail(item)">
        <div class="mc-img-wrap" v-if="item.fileType==='image'">
          <img :src="item.fileUrl" :alt="item.title" class="polaroid-img" />
        </div>
        <div class="mc-img-wrap mc-video-placeholder" v-else-if="item.fileType==='video'">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
        </div>
        <!-- 日记 / 文本类记忆：信纸预览卡片 -->
        <div class="mc-img-wrap mc-diary-card" v-else>
          <div class="diary-paper">
            <div class="diary-header">
              <span class="diary-icon">📖</span>
              <span class="diary-date">{{ fmt(item.createdAt || item.eventTime) }}</span>
            </div>
            <div class="diary-lines">
              <span class="diary-line" />
              <span class="diary-line" />
              <span class="diary-line" />
              <span class="diary-line" />
              <span class="diary-line" />
            </div>
            <div class="diary-preview">
              <span class="diary-quote-mark">"</span>
              {{ item.content ? item.content.substring(0, 60) + (item.content.length > 60 ? '...' : '') : '暂无内容' }}
            </div>
          </div>
        </div>
        <div class="polaroid-caption">{{ item.title }}</div>
        <div class="mc-meta">
          <el-tag size="small" :type="typeColor(item.memoryType)">{{ typeLabel(item.memoryType) }}</el-tag>
          <span class="mc-date">{{ fmt(item.createdAt || item.eventTime) }}</span>
        </div>
        <div class="mc-actions" @click.stop>
          <el-button size="small" type="danger" @click="del(item)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>
      </div>
      <DevelopingOverlay v-if="loading" text="正在冲洗记忆…" />
    </div>

    <el-dialog v-model="dlg" :title="editing ? '编辑记忆' : (cur?.title || '记忆详情')" width="580px" class="warm-dialog" @close="editing=false">
      <div v-if="cur">
        <!-- 查看模式 -->
        <template v-if="!editing">
          <div v-if="cur.fileType==='image'" class="dlg-img"><img :src="cur.fileUrl" /></div>
          <div v-if="cur.content" class="dlg-content-block">
            <h4>📖 日记内容</h4>
            <div class="content-paper">{{ cur.content }}</div>
          </div>
          <!-- 完整信息卡片 -->
          <div class="detail-info-grid">
            <div class="info-item" v-if="cur.title">
              <span class="info-label">标题</span>
              <span class="info-value">{{ cur.title }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">时间</span>
              <span class="info-value">{{ fmt(cur.eventTime || cur.createdAt) }}</span>
            </div>
            <div class="info-item" v-if="cur.emotion">
              <span class="info-label">心情</span>
              <el-tag size="small" type="warning" effect="plain">{{ cur.emotion }}</el-tag>
            </div>
            <div class="info-item" v-if="cur.location">
              <span class="info-label">地点</span>
              <el-tag size="small" type="success" effect="plain">📍 {{ cur.location }}</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">类型</span>
              <el-tag size="small">{{ typeLabel(cur.memoryType) }}</el-tag>
            </div>
            <div class="info-item full-width" v-if="cur.description">
              <span class="info-label">描述</span>
              <p class="info-desc">{{ cur.description }}</p>
            </div>
          </div>
          <!-- AI 分析（有则展示） -->
          <div v-if="cur.aiSummary" class="ai-box">
            <h4>AI 分析</h4>
            <p>{{ cur.aiSummary }}</p>
            <div class="ai-tags">
              <el-tag v-for="t in parseTags(cur.tags)" :key="t" effect="plain">{{ t }}</el-tag>
            </div>
          </div>
          <div class="dlg-footer">
            <el-button size="small" :loading="analyzing" @click="handleAnalyze">
              <el-icon><MagicStick /></el-icon> AI 分析
            </el-button>
            <el-button class="btn-primary" size="small" @click="startEdit"><el-icon><Edit /></el-icon> 编辑</el-button>
          </div>
        </template>

        <!-- 编辑模式 -->
        <template v-else>
          <el-form :model="editForm" label-position="top">
            <el-form-item label="标题" required>
              <el-input v-model="editForm.title" placeholder="记忆标题" maxlength="100" />
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="editForm.eventTime" type="datetime"
                placeholder="记忆发生的时间" format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
            </el-form-item>
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item label="心情">
                  <el-select v-model="editForm.emotion" placeholder="当时的心情" style="width:100%">
                    <el-option v-for="e in emotions" :key="e.value" :label="e.label" :value="e.value">
                      <span>{{ e.icon }} {{ e.label }}</span>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="地点">
                  <el-input v-model="editForm.location" placeholder="在哪发生的？">
                    <template #prefix><span>📍</span></template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="类型">
              <el-select v-model="editForm.memoryType" placeholder="选择类型" style="width:100%">
                <el-option v-for="o in typeOptions.filter(t=>t.value)" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="简单描述这段记忆..."/>
            </el-form-item>
            <el-form-item v-if="cur.fileType==='text'||cur.content" label="内容">
              <el-input v-model="editForm.content" type="textarea" :rows="6" placeholder="日记内容..."/>
            </el-form-item>
          </el-form>
          <div class="dlg-footer">
            <el-button @click="editing=false">取消</el-button>
            <el-button class="btn-primary" :loading="saving" @click="handleSave">保存修改</el-button>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Delete, Edit, MagicStick } from '@element-plus/icons-vue'
import { getMemoryListApi, deleteMemoryApi, updateMemoryApi } from '@/api/memory'
import { analyzeMemoryApi } from '@/api/ai'
import DevelopingOverlay from '@/components/DevelopingOverlay.vue'

interface M { id:number;title:string;description:string;content:string;fileUrl:string;fileType:string;memoryType:string;location:string;emotion:string;tags:string;aiSummary:string;createdAt:string;eventTime:string }

const typeOptions = [
  {label:'全部',value:''},{label:'照片',value:'photo'},{label:'视频',value:'video'},
  {label:'日记',value:'diary'},{label:'旅行',value:'travel'},{label:'学习',value:'study'},{label:'日常',value:'daily'},
]
const emotions = [
  { value: '开心', label: '开心', icon: '😊' },
  { value: '感动', label: '感动', icon: '🥹' },
  { value: '兴奋', label: '兴奋', icon: '🤩' },
  { value: '温馨', label: '温馨', icon: '💕' },
  { value: '平静', label: '平静', icon: '😌' },
  { value: '思考', label: '思考', icon: '🤔' },
  { value: '低落', label: '低落', icon: '😢' },
]
const memories = ref<M[]>([])
const loading = ref(false)
const filterType=ref('')
const searchKey=ref('')
const dlg=ref(false)
const cur=ref<M|null>(null)
const editing=ref(false)
const saving=ref(false)
const analyzing=ref(false)
const editForm = reactive({ title: '', eventTime: '', emotion: '', location: '', memoryType: '', description: '', content: '' })

// 将后端返回的 LocalDateTime 字符串规整为 el-date-picker 的 ISO value-format
const normTime = (s?: string) => s ? s.replace(' ', 'T').replace(/\.\d+$/, '').slice(0, 19) : ''

onMounted(()=>fetchList())
const fetchList=async()=>{
  loading.value=true
  try{const r:any=await getMemoryListApi({memoryType:filterType.value||undefined,page:1,size:50});memories.value=r.data||[]}
  catch(e:any){ElMessage.error(e?.message||'加载失败')}
  finally{loading.value=false}
}
const showDetail=(m:M)=>{cur.value=m;dlg.value=true;editing.value=false}
const handleAnalyze=async()=>{
  if(!cur.value) return
  analyzing.value=true
  try{
    const r:any=await analyzeMemoryApi(cur.value.id)
    Object.assign(cur.value, r.data || {})
    ElMessage.success('AI 分析完成')
    fetchList()
  }catch(e:any){ElMessage.error(e?.message||'AI 分析失败')}
  finally{analyzing.value=false}
}
const startEdit=()=>{
  if(!cur.value) return
  editForm.title = cur.value.title || ''
  editForm.emotion = cur.value.emotion || ''
  editForm.location = cur.value.location || ''
  editForm.memoryType = cur.value.memoryType || ''
  editForm.description = cur.value.description || ''
  editForm.content = cur.value.content || ''
  editForm.eventTime = normTime(cur.value.eventTime) || normTime(cur.value.createdAt) || ''
  editing.value = true
}
const handleSave=async()=>{
  if(!cur.value) return
  if(!editForm.title.trim()){ElMessage.warning('标题不能为空');return}
  saving.value=true
  try{
    const data:Record<string,any> = {
      title: editForm.title.trim(),
      emotion: editForm.emotion,
      location: editForm.location,
      memoryType: editForm.memoryType,
      description: editForm.description,
    }
    if (editForm.eventTime) data.eventTime = editForm.eventTime
    if(cur.value.fileType==='text'||cur.value.content) data.content = editForm.content

    console.log('[Memories] 发送更新请求:', JSON.stringify(data))
    const res:any = await updateMemoryApi(cur.value.id, data)
    console.log('[Memories] 更新响应:', res)

    ElMessage.success('更新成功')
    Object.assign(cur.value, data)
    editing.value = false
    fetchList()
  }catch(e:any){
    // 打印完整错误信息到控制台，方便定位
    console.error('[Memories] 更新失败完整错误:', e)
    console.error('[Memories] 错误响应:', e?.response?.data)
    const errMsg = e?.response?.data?.message || e?.message || '更新失败'
    ElMessage.error(errMsg)
  }
  finally{saving.value=false}
}
const del=async(m:M)=>{
  try{await ElMessageBox.confirm('确定删除？','提示',{type:'warning'});await deleteMemoryApi(m.id);ElMessage.success('已删除');fetchList()}
  catch{}
}
const fmt=(s:string)=>s?new Date(s).toLocaleDateString('zh-CN'):''
const parseTags=(t?:string)=>t?t.split(',').filter(Boolean).map(x=>x.trim()):[]
const typeLabel=(t:string)=>({photo:'照片',video:'视频',diary:'日记',travel:'旅行',study:'学习',daily:'日常',note:'笔记',thought:'感悟'}as any)[t]||t
const typeColor=(t:string)=>({photo:'success',video:'warning',diary:'',travel:'danger',study:'info'}as any)[t]||'info'
</script>

<style scoped>
.memories-page{max-width:1100px}
.memory-grid-wrap{position:relative;min-height:240px}
.filter-bar{display:flex;gap:12px;align-items:center;padding:14px 20px;margin-bottom:16px}
.total-text{color:var(--text-muted);font-size:13px;margin-right:auto}
.memory-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:18px}
.memory-card{
  cursor:pointer;position:relative;overflow:visible;
  padding: 10px 10px 22px 10px;
}
.mc-img-wrap{width:100%;aspect-ratio:4/3;border-radius:2px;overflow:hidden;background:rgba(232,137,75,0.04)}
.mc-img-wrap img{width:100%;height:100%;object-fit:cover;filter:sepia(0.06) brightness(1.02)}
.mc-video-placeholder{display:flex;align-items:center;justify-content:center}
/* 日记卡片 */
.mc-diary-card{
  background:linear-gradient(175deg, #FFFDF9 0%, #FFF8F0 40%, #FDF5EA 100%);
  border:1px solid rgba(200,160,120,0.18);
  overflow:hidden;
}
.diary-paper{
  width:100%;height:100%;
  padding:12px 14px;
  display:flex;flex-direction:column;
  position:relative;
}
.diary-paper::before{
  content:'';
  position:absolute;inset:0;
  background:repeating-linear-gradient(
    0deg, transparent, transparent 23px, rgba(200,160,120,0.06) 23px, rgba(200,160,120,0.06) 24px
  );
  pointer-events:none;
}
.diary-header{
  display:flex;align-items:center;gap:6px;
  margin-bottom:8px;
  position:relative;z-index:1;
}
.diary-icon{font-size:14px;line-height:1}
.diary-date{
  font-family:var(--font-hand);
  font-size:10px;
  color:var(--text-muted);
}
.diary-lines{
  display:flex;flex-direction:column;gap:3px;
  margin-bottom:8px;
  position:relative;z-index:1;
}
.diary-line{
  display:block;height:1px;
  background:rgba(200,160,120,0.12);
  border-radius:1px;
}
.diary-line:nth-child(2){width:92%}
.diary-line:nth-child(3){width:85%}
.diary-line:nth-child(4){width:96%}
.diary-line:nth-child(5){width:78%}
.diary-preview{
  font-family:var(--font-hand);
  font-size:11px;
  line-height:1.7;
  color:var(--text-secondary);
  overflow:hidden;
  position:relative;z-index:1;
  flex:1;
  display:-webkit-box;
  -webkit-line-clamp:3;
  -webkit-box-orient:vertical;
}
.diary-quote-mark{
  font-family:var(--font-display);
  font-size:18px;
  color:var(--color-primary-light);
  line-height:0;
  vertical-align:middle;
  margin-right:2px;
}
.mc-meta{display:flex;justify-content:space-between;align-items:center;margin-top:8px;padding:0 2px}
.mc-date{font-family:var(--font-hand);font-size:11px;color:var(--text-muted)}
.mc-actions{position:absolute;top:14px;right:14px;opacity:0;transition:opacity 0.2s}
.memory-card:hover .mc-actions{opacity:1}
.dlg-img{margin-bottom:16px}
.dlg-img img{max-width:100%;border-radius:var(--radius-md)}
.ai-box{background:rgba(232,137,75,0.04);border:1px solid rgba(232,137,75,0.12);border-radius:var(--radius-md);padding:14px;margin:12px 0}
.ai-box h4{color:var(--color-primary);margin-bottom:8px}
.ai-tags{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}
.dlg-meta{display:flex;gap:10px;align-items:center;color:var(--text-muted);font-size:13px;margin-top:14px}
/* 详情信息网格 */
.detail-info-grid{
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:10px 20px;
  background:linear-gradient(180deg,#FFFDF9 0%,#FFF8F0 100%);
  border:1px solid rgba(200,160,120,0.15);
  border-radius:var(--radius-md);
  padding:16px 18px;
  margin:12px 0;
}
.info-item{display:flex;align-items:center;gap:8px}
.info-item.full-width{grid-column:1/-1}
.info-label{
  font-size:12px;color:var(--text-muted);white-space:nowrap;
  min-width:36px;font-weight:500;
}
.info-value{font-size:13px;color:var(--text-primary);font-weight:500}
.info-desc{margin:4px 0 0;font-size:13px;color:var(--text-secondary);line-height:1.6}
.dlg-footer{display:flex;justify-content:flex-end;gap:8px;margin-top:16px;padding-top:12px;border-top:1px solid rgba(200,160,120,0.1)}
.dlg-content-block{margin:12px 0}
.dlg-content-block h4{color:var(--text-secondary);font-size:13px;margin-bottom:8px}
.content-paper{
  background:linear-gradient(180deg, #FFFDF9 0%, #FFF8F0 100%);
  border:1px solid rgba(200,160,120,0.15);
  border-radius:var(--radius-md);
  padding:16px 18px;
  font-family:var(--font-hand);
  font-size:14px;
  line-height:2;
  color:var(--text-primary);
  white-space:pre-wrap;
  position:relative;
}
.content-paper::before{
  content:'';
  position:absolute;inset:0;
  background:repeating-linear-gradient(
    0deg, transparent, transparent 27px, rgba(200,160,120,0.05) 27px, rgba(200,160,120,0.05) 28px
  );
  pointer-events:none;border-radius:inherit;
}
:deep(.warm-dialog .el-dialog){border-radius:var(--radius-lg)!important}
:deep(.el-input__wrapper){box-shadow:0 0 0 1px rgba(200,160,120,0.15) inset}
:deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 2px rgba(232,137,75,0.25) inset!important}
</style>
