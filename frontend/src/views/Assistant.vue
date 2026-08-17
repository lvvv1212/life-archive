<template>
  <div class="assistant-page">
    <div class="chat-container" ref="chatBox">
      <div v-if="!msgs.length" class="welcome">
        <div class="welcome-icon-wrap">
          <svg width="64" height="64" viewBox="0 0 64 64">
            <circle cx="32" cy="32" r="30" fill="url(#wag2)"/>
            <text x="32" y="42" text-anchor="middle" font-size="28">🤖</text>
            <defs><linearGradient id="wag2"><stop stop-color="#E8894B"/><stop offset="1" stop-color="#D4956B"/></linearGradient></defs>
          </svg>
        </div>
        <h2>AI 记忆助手</h2>
        <p>向我提问，我会从你的记忆档案中寻找答案</p>
        <div class="suggest-row">
          <el-button v-for="q in tips" :key="q" class="suggest-btn" @click="send(q)">{{ q }}</el-button>
        </div>
      </div>

      <div v-for="(m,i) in msgs" :key="i" class="msg-row" :class="m.role">
        <div class="msg-avatar">{{ m.role==='user'?'👤':'🤖' }}</div>
        <div class="msg-bubble-wrap">
          <div class="msg-bubble" :class="m.role">
            <div v-html="md(m.content)"/>
            <div v-if="m.sources?.length" class="msg-src">
              <el-divider/>
              <span class="src-title">📚 参考记忆：</span>
              <el-tag v-for="(s,si) in m.sources" :key="si" size="small" effect="plain" style="margin:2px">{{ s.title }}</el-tag>
            </div>
          </div>
          <button class="msg-del-btn" @click="delMsg(i)" title="删除这条消息">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/></svg>
          </button>
        </div>
      </div>

      <div v-if="thinking" class="msg-row assistant">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble assistant thinking">思考中<span class="dots">...</span></div>
      </div>
    </div>

    <div class="input-bar">
      <div class="input-top-actions" v-if="msgs.length">
        <el-button text size="small" class="clear-all-btn" @click="clearAll">
          <el-icon><Delete /></el-icon> 清空对话
        </el-button>
      </div>
      <el-input v-model="input" placeholder="问点关于你的事..." @keydown.enter.exact="send()" resize="none" :rows="2" type="textarea"/>
      <div class="input-foot">
        <span>Enter 发送</span>
        <el-button class="btn-primary" :disabled="!input.trim()||thinking" :loading="thinking" @click="send()">
          <el-icon><Promotion /></el-icon> 发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Delete } from '@element-plus/icons-vue'
import { chatApi } from '@/api/assistant'

const msgs = ref<Array<{role:string;content:string;sources?:any[]}>>([])
const input = ref('');const thinking = ref(false);const chatBox = ref<HTMLElement>()

const tips=['我有哪些重要的记忆？','我最近一次旅行是什么时候？','总结一下我的大学生活','去年最开心的一件事是什么？']

onMounted(()=>{
  const s=localStorage.getItem('assistant_history');if(s)try{msgs.value=JSON.parse(s)}catch{}
})

const send = async (text?: string) => {
  const t = (text || input.value).trim(); if (!t || thinking.value) return
  input.value=''; msgs.value.push({role:'user',content:t});save();scroll()
  thinking.value=true
  try {
    const history = msgs.value.slice(-12).map(m=>({role:m.role,content:m.content}))
    const r:any = await chatApi(t, history)
    msgs.value.push({role:'assistant',content:r.data.answer||'抱歉，无法回答。',sources:r.data.sources||[]})
  } catch (e:any) { msgs.value.push({role:'assistant',content:'抱歉，系统忙，请稍后再试。'});ElMessage.error(e?.message) }
  finally { thinking.value=false;save();scroll() }
}

const delMsg = (idx: number) => { msgs.value.splice(idx, 1); save(); ElMessage.success('已删除') }

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有对话记录吗？', '清空对话', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    msgs.value = []; save(); ElMessage.success('对话已清空')
  } catch { /* 取消 */ }
}

const scroll = async () => { await nextTick(); chatBox.value&&(chatBox.value.scrollTop=chatBox.value.scrollHeight) }
const save = () => localStorage.setItem('assistant_history',JSON.stringify(msgs.value.slice(-50)))
const md = (t:string) => t.replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>').replace(/\n/g,'<br>')
</script>

<style scoped>
.assistant-page{display:flex;flex-direction:column;height:calc(100vh - 60px - 48px);max-width:780px;margin:0 auto}
.chat-container{flex:1;overflow-y:auto;padding:20px 0}
.welcome{text-align:center;padding:60px 20px}
.welcome-icon-wrap{margin-bottom:16px}
.welcome h2{font-family:var(--font-display);color:var(--text-primary);margin-bottom:6px}
.welcome p{color:var(--text-muted);margin-bottom:20px;font-size:14px}
.suggest-row{display:flex;flex-wrap:wrap;justify-content:center;gap:8px}
.suggest-btn{border-radius:20px;border:1px solid rgba(200,160,120,0.25);color:var(--color-primary-dark);font-size:13px;background:var(--bg-card)}
.suggest-btn:hover{background:rgba(232,137,75,0.06);border-color:var(--color-primary)}

.msg-row{display:flex;gap:10px;margin-bottom:18px;padding:0 12px}
.msg-row.user{flex-direction:row-reverse}
.msg-avatar{width:34px;height:34px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:18px;background:rgba(232,137,75,0.06);flex-shrink:0}

/* 信纸气泡 */
.msg-bubble-wrap{display:flex;align-items:center;gap:4px;max-width:75%}
.msg-row.user .msg-bubble-wrap{flex-direction:row-reverse}
.msg-bubble{width:100%;padding:12px 15px;border-radius:12px;font-size:14px;line-height:1.6}
.msg-bubble.user{
  background: linear-gradient(135deg, #E8F0FE 0%, #F0F4FF 100%);
  border: 1px solid rgba(100,140,200,0.15);
  color: var(--text-primary);
  border-bottom-right-radius: 2px;
}
.msg-bubble.assistant{
  background: var(--bg-card);
  border: 1px solid rgba(200,160,120,0.15);
  border-bottom-left-radius: 2px;
  position: relative;
}
/* 信纸横线 */
.msg-bubble.assistant::before {
  content: '';
  position: absolute; inset: 0;
  background: repeating-linear-gradient(
    0deg, transparent, transparent 27px, rgba(200,160,120,0.06) 27px, rgba(200,160,120,0.06) 28px
  );
  pointer-events: none; border-radius: inherit;
}
.msg-bubble.thinking{color:var(--text-muted)}

.msg-del-btn{
  display:flex;align-items:center;justify-content:center;
  width:26px;height:26px;border-radius:50%;border:none;
  background:transparent;color:var(--text-muted);cursor:pointer;
  opacity:0;transition:opacity 0.2s,background 0.2s,color 0.2s;flex-shrink:0
}
.msg-bubble-wrap:hover .msg-del-btn{opacity:1}
.msg-del-btn:hover{background:rgba(245,108,108,0.1);color:#f56c6c}

.msg-src{font-size:12px;position:relative;z-index:1}
.src-title{color:var(--text-muted)}

.input-bar{background:var(--bg-card);border-top:1px solid rgba(200,160,120,0.1);padding:12px 16px;border-radius:0 0 var(--radius-lg) var(--radius-lg)}
.input-top-actions{display:flex;justify-content:flex-end;margin-bottom:8px}
.clear-all-btn{color:var(--text-muted);font-size:12px;padding:4px 8px}
.clear-all-btn:hover{color:#f56c6c}
.input-foot{display:flex;justify-content:space-between;align-items:center;margin-top:8px}
.input-foot span{color:var(--text-muted);font-size:12px}
:deep(.el-textarea__inner){border-radius:var(--radius-md);border-color:rgba(200,160,120,0.15)}
:deep(.el-textarea__inner:focus){border-color:var(--color-primary-light);box-shadow:0 0 0 2px rgba(232,137,75,0.12)}
</style>