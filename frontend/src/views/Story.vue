<template>
  <div class="story-page">
    <!-- 阶段一：选择主题 -->
    <div v-if="phase === 'select'">
      <div class="page-header">
        <h2>AI 回忆文章生成</h2>
        <p>选择主题，让 AI 为你撰写专属的回忆故事</p>
      </div>

      <div class="theme-grid">
        <div v-for="t in themes" :key="t"
             class="theme-card card-warm"
             :class="{ active: selected === t }"
             @click="pickTheme(t)">
          <div class="th-icon">{{ themeIcon(t) }}</div>
          <div class="th-name">{{ t }}</div>
        </div>
        <div class="theme-card card-warm custom-card"
             :class="{ active: isCustom }"
             @click="openCustom">
          <div class="th-icon">✏️</div>
          <div class="th-name">自定义</div>
        </div>
      </div>

      <div v-if="isCustom" class="custom-row card-warm">
        <el-input
          ref="customInputRef"
          v-model="customText"
          placeholder="例如：我的考研之路"
          size="large"
          @keyup.enter="startGenerate"
        />
      </div>

      <div class="gen-row">
        <el-button
          class="btn-primary"
          size="large"
          :disabled="!hasTheme"
          :loading="loading"
          @click="startGenerate"
        >
          <el-icon><MagicStick /></el-icon>
          {{ loading ? 'AI 撰写中...' : '开始生成' }}
        </el-button>
        <p class="hint">AI 将分析你的全部记忆数据，可能需要几秒钟</p>
      </div>
    </div>

    <!-- 阶段二：展示结果 —— 简洁卡片布局 -->
    <div v-else class="result-view">
      <div class="result-actions">
        <el-button text @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <el-button class="btn-primary" size="small" @click="copyArticle">复制文章</el-button>
      </div>

      <!-- 文章卡片：扁平单栏，内联样式兜底 -->
      <div
        class="card-warm story-card"
        style="
          padding: 32px;
          border-radius: 12px;
          box-shadow: 0 4px 20px rgba(100,60,30,0.08);
          background: #FFFBF5;
        "
      >
        <!-- 标题区 -->
        <div style="margin-bottom:24px;padding-bottom:16px;border-bottom:1px solid rgba(200,160,120,0.18)">
          <h2 style="font-size:22px;color:#4A3528;margin:0 0 10px;font-family:serif">
            {{ storyTitle }}
          </h2>
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <span style="background:#e8f5e9;color:#2e7d32;padding:3px 10px;border-radius:12px;font-size:13px">
              📊 {{ wordCount }} 字
            </span>
            <span style="background:#fff3e0;color:#e65100;padding:3px 10px;border-radius:12px;font-size:13px">
              📚 参考 {{ memoryCount }} 条记忆
            </span>
          </div>
        </div>

        <!-- 正文渲染区：marked 输出 + 内联样式确保可见 -->
        <div
          class="story-body"
          style="color:#4A3528;line-height:2;font-size:15px;word-break:break-word"
          v-html="renderedHtml"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, ArrowLeft } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { generateStoryApi } from '@/api/story'

// ==================== 主题配置 ====================
const themes = [
  '我的大学生活', '我的旅行回忆', '我的成长故事',
  '我的美食之旅', '珍贵的友情', '我的学习之路',
]
const themeIcons: Record<string, string> = {
  '我的大学生活': '🎓', '我的旅行回忆': '✈️',
  '我的成长故事': '🌱', '我的美食之旅': '🍜',
  '珍贵的友情': '💕', '我的学习之路': '📖',
}

// ==================== 状态 ====================
type Phase = 'select' | 'result'
const phase = ref<Phase>('select')
const loading = ref(false)
const selected = ref('')
const customText = ref('')
const isCustom = ref(false)
const customInputRef = ref<any>(null)

const storyTitle = ref('加载中...')
const rawContent = ref('')
const wordCount = ref(0)
const memoryCount = ref(0)

// ==================== 计算属性 ====================

const hasTheme = computed(() => !!(selected.value || customText.value.trim()))
const activeTheme = computed(() => customText.value.trim() || selected.value || '')

/** 用 marked 渲染 Markdown → HTML */
const renderedHtml = computed(() => {
  const text = rawContent.value
  if (!text || !text.trim()) {
    return '<p style="color:#999;text-align:center;padding:40px 0">暂无内容</p>'
  }
  try {
    return marked.parse(text, { async: false }) as string
  } catch (e) {
    console.error('[Story] marked 渲染失败:', e)
    const esc = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    return `<pre style="white-space:pre-wrap;line-height:1.8;color:#4A3528">${esc}</pre>`
  }
})

// ==================== 方法 ====================

function themeIcon(t: string): string {
  return themeIcons[t] || '📝'
}

function pickTheme(t: string) {
  selected.value = t
  isCustom.value = false
  customText.value = ''
}

function openCustom() {
  selected.value = ''
  isCustom.value = true
  nextTick(() => customInputRef.value?.focus())
}

function goBack() {
  phase.value = 'select'
}

async function startGenerate() {
  const theme = activeTheme.value
  if (!theme) {
    ElMessage.warning('请先选择或输入一个主题')
    return
  }
  loading.value = true
  try {
    const res: any = await generateStoryApi(theme)
    console.log('[Story] API 返回:', JSON.stringify(res).substring(0, 300))

    if (!res) throw new Error('服务器无响应')
    if (res.code !== undefined && res.code !== 200 && res.code !== 0) {
      throw new Error(res.message || `服务器错误(${res.code})`)
    }
    const data = res.data
    if (!data) throw new Error('服务器未返回文章数据')

    // 赋值
    storyTitle.value = data.title || `《${theme}》`
    rawContent.value = data.content || ''
    wordCount.value = data.wordCount || 0
    memoryCount.value = data.memoryCount || 0

    // 切页
    phase.value = 'result'
    ElMessage.success('生成完成！')
  } catch (e: any) {
    console.error('[Story] 失败:', e)
    ElMessage.error(e?.message || e?.response?.data?.message || '生成失败，请重试')
  } finally {
    loading.value = false
  }
}

function copyArticle() {
  const text = rawContent.value
  if (!text) { ElMessage.warning('没有可复制的内容'); return }
  navigator.clipboard.writeText(text)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => {
      const ta = document.createElement('textarea')
      ta.value = text; document.body.appendChild(ta)
      ta.select(); document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('已复制到剪贴板')
    })
}
</script>

<style scoped>
.story-page { max-width: 900px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h2 {
  font-family: var(--font-display); font-size: 20px;
  font-weight: 700; color: var(--text-primary); margin-bottom: 4px;
}
.page-header p { color: var(--text-muted); font-size: 13px; }

.theme-grid {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 12px; margin-bottom: 20px;
}
.theme-card {
  cursor: pointer; text-align: center; padding: 24px 12px;
  border: 2px solid transparent; transition: all .25s;
  border-radius: var(--radius-md);
}
.theme-card:hover { transform: translateY(-2px); }
.theme-card.active {
  border-color: var(--color-primary);
  background: rgba(232,137,75,0.04);
}
.th-icon { font-size: 32px; margin-bottom: 8px; }
.th-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.custom-card {
  border-style: dashed; border-color: rgba(200,160,120,0.25);
}
.custom-row { padding: 16px; margin-bottom: 20px; border-radius: var(--radius-md); }

.gen-row { text-align: center; padding: 20px 0; }
.gen-row .hint { color: var(--text-muted); font-size: 12px; margin-top: 10px; }

.result-view { min-height: 200px; }
.result-actions { display: flex; gap: 8px; margin-bottom: 14px; }

/* ====== Markdown 正文样式 ====== */
.story-body :deep(h1) {
  font-family: serif; font-size: 22px; color: #4A3528;
  text-align: center; margin: 28px 0 16px;
}
.story-body :deep(h2) {
  font-family: serif; font-size: 19px; color: #c97830;
  margin: 28px 0 14px; padding-bottom: 8px;
  border-bottom: 2px solid rgba(200,160,120,0.2);
}
.story-body :deep(h3) {
  font-family: serif; font-size: 16px; color: #4A3528;
  margin: 18px 0 8px;
}
.story-body :deep(p) {
  line-height: 2; color: #4A3528; text-indent: 2em; margin: 6px 0;
}
.story-body :deep(blockquote) {
  border-left: 4px solid #c97830; padding: 8px 16px;
  margin: 12px 0; background: rgba(232,137,75,0.03);
  color: #7a6b5d; font-style: italic;
}
.story-body :deep(hr) {
  border: none; border-top: 1px dashed rgba(200,160,120,0.25);
  margin: 24px 0;
}
.story-body :deep(strong) { color: #b85c00; }
.story-body :deep(em) { color: #7a6b5d; }
.story-body :deep(ul), .story-body :deep(ol) {
  padding-left: 1.8em; margin: 8px 0; line-height: 2;
}
.story-body :deep(li) { color: #4A3528; margin: 2px 0; }
.story-body :deep(code) {
  background: rgba(200,160,120,0.1); padding: 2px 6px;
  border-radius: 3px; font-size: 0.9em;
}
.story-body :deep(pre) {
  background: rgba(200,160,120,0.06); padding: 16px;
  border-radius: 6px; overflow-x: auto; margin: 12px 0;
}
.story-body :deep(pre code) { background: none; padding: 0; }
</style>
