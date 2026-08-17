<template>
  <div class="upload-page">
    <div class="page-header">
      <h2>上传记忆</h2>
      <p>记录下每一个值得珍藏的瞬间</p>
    </div>

    <el-tabs v-model="activeTab" class="upload-tabs">
      <el-tab-pane label="文件上传" name="file">
        <div class="upload-card-wrap">
        <div class="card-warm upload-card">
          <!-- 胶片负片上传区 -->
          <div class="film-strip-upload">
            <div class="film-holes left">
              <span v-for="i in 6" :key="i" class="film-hole" />
            </div>
            <div class="film-holes right">
              <span v-for="i in 6" :key="i" class="film-hole" />
            </div>
            <el-upload class="upload-area" drag :auto-upload="false" :limit="1"
              :on-change="handleFileChange" :file-list="fileList" accept="image/*,video/*">
              <div class="upload-icon-wrap">
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
              </div>
              <div class="upload-text">将文件拖到此处，或<span>点击上传</span></div>
              <template #tip><div class="upload-tip">支持 jpg / png / mp4，单个文件不超过 100MB</div></template>
            </el-upload>
          </div>

          <el-form :model="fileForm" label-position="top" style="margin-top:20px">
            <el-form-item label="照片名称" required>
              <el-input v-model="fileForm.title" placeholder="给照片取个名字，如「大学毕业典礼」" maxlength="100" show-word-limit />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="地点">
                  <el-input v-model="fileForm.location" placeholder="在哪拍的？">
                    <template #prefix><span>📍</span></template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="心情">
                  <el-select v-model="fileForm.emotion" placeholder="当时的心情" style="width:100%">
                    <el-option v-for="e in emotions" :key="e.value" :label="e.label" :value="e.value">
                      <span>{{ e.icon }} {{ e.label }}</span>
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="类型">
                  <el-select v-model="fileForm.memoryType" placeholder="选择类型" style="width:100%">
                    <el-option label="📷 照片" value="photo" />
                    <el-option label="🎬 视频" value="video" />
                    <el-option label="✈️ 旅行" value="travel" />
                    <el-option label="📖 学习" value="study" />
                    <el-option label="🌿 日常" value="daily" />
                    <el-option label="🎉 聚会" value="party" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述">
              <el-input v-model="fileForm.description" type="textarea" :rows="2" placeholder="简单描述这张照片背后的故事..."/>
            </el-form-item>
            <el-form-item>
              <el-button class="btn-primary" :loading="uploading" @click="handleUploadFile" size="large">
                <el-icon><Upload /></el-icon>
                {{ uploading ? '上传中...' : '上传记忆' }}
              </el-button>
              <span class="upload-hint">照片名称会用在 AI 助手搜索、回忆文章生成和时间轴展示中</span>
            </el-form-item>
          </el-form>
        </div>
        <DevelopingOverlay v-if="uploading" text="照片正在显影…" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="写日记" name="text">
        <div class="upload-card-wrap">
        <div class="card-warm upload-card">
          <el-form :model="textForm" label-position="top">
            <el-row :gutter="16">
              <el-col :span="16">
                <el-form-item label="标题" required>
                  <el-input v-model="textForm.title" placeholder="给日记起个标题" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="类型">
                  <el-select v-model="textForm.memoryType" style="width:100%">
                    <el-option label="日记" value="diary" />
                    <el-option label="笔记" value="note" />
                    <el-option label="感悟" value="thought" />
                    <el-option label="学习" value="study" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="内容" required>
              <el-input v-model="textForm.content" type="textarea" :rows="8" placeholder="今天发生了什么值得记录的事情..."/>
            </el-form-item>
            <el-form-item>
              <el-button class="btn-primary" :loading="saving" @click="handleSaveText" size="large">
                {{ saving ? '保存中...' : '保存记忆' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <DevelopingOverlay v-if="saving" text="回忆正在保存…" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { uploadMemoryApi, createTextMemoryApi } from '@/api/memory'
import DevelopingOverlay from '@/components/DevelopingOverlay.vue'

const activeTab = ref('file')
const uploading = ref(false)
const saving = ref(false)

const emotions = [
  { value: '开心', label: '开心', icon: '😊' },
  { value: '感动', label: '感动', icon: '🥹' },
  { value: '兴奋', label: '兴奋', icon: '🤩' },
  { value: '温馨', label: '温馨', icon: '💕' },
  { value: '平静', label: '平静', icon: '😌' },
  { value: '思考', label: '思考', icon: '🤔' },
  { value: '低落', label: '低落', icon: '😢' },
]

const fileList = ref<UploadFile[]>([])
const selectedFile = ref<File | null>(null)
const fileForm = reactive({ title: '', description: '', memoryType: 'photo', emotion: '', location: '' })

const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw || null
  if (!fileForm.title) fileForm.title = file.name
}

const handleUploadFile = async () => {
  if (!selectedFile.value) { ElMessage.warning('请先选择文件'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('title', fileForm.title)
    fd.append('description', fileForm.description)
    fd.append('memoryType', fileForm.memoryType)
    fd.append('emotion', fileForm.emotion)
    fd.append('location', fileForm.location)
    await uploadMemoryApi(fd)
    ElMessage.success('上传成功！')
    fileForm.title = ''; fileForm.description = ''; fileForm.emotion = ''; fileForm.location = ''
    fileList.value = []; selectedFile.value = null
  } catch (err: any) { ElMessage.error(err?.message || '上传失败') }
  finally { uploading.value = false }
}

const textForm = reactive({ title: '', content: '', memoryType: 'diary' })

const handleSaveText = async () => {
  if (!textForm.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!textForm.content.trim()) { ElMessage.warning('请输入内容'); return }
  saving.value = true
  try {
    await createTextMemoryApi({ title: textForm.title, content: textForm.content, memoryType: textForm.memoryType })
    ElMessage.success('保存成功！')
    textForm.title = ''; textForm.content = ''
  } catch (err: any) { ElMessage.error(err?.message || '保存失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.upload-page { max-width: 800px; }
.upload-card-wrap { position: relative; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-family: var(--font-display); font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
.page-header p { color: var(--text-muted); font-size: 13px; }
.upload-card { padding: 28px; }

/* 胶片负片 */
.film-strip-upload {
  position: relative;
  background: linear-gradient(180deg, #2D1F15 0%, #3D2B1F 100%);
  border-radius: var(--radius-lg);
  padding: 8px 28px;
  overflow: hidden;
}
.film-holes {
  position: absolute; top: 0; bottom: 0;
  display: flex; flex-direction: column; justify-content: space-around;
  padding: 10px 0;
}
.film-holes.left { left: 8px; }
.film-holes.right { right: 8px; }
.film-hole {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: var(--bg-base);
  border: 1px solid rgba(200,160,120,0.3);
}

:deep(.upload-tabs .el-tabs__header) { margin-bottom: 20px; }
:deep(.upload-tabs .el-tabs__item.is-active) { color: var(--color-primary); }
:deep(.upload-tabs .el-tabs__active-bar) { background: var(--gradient-warm); }
:deep(.el-upload-dragger) {
  border: 2px dashed rgba(255,255,255,0.25) !important;
  border-radius: var(--radius-md) !important;
  background: rgba(255,255,255,0.06) !important;
}
:deep(.el-upload-dragger:hover) { border-color: rgba(232,137,75,0.6) !important; background: rgba(255,255,255,0.1) !important; }
.upload-icon-wrap { margin-bottom: 8px; }
.upload-text { font-size: 14px; color: rgba(255,255,255,0.7); }
.upload-text span { color: var(--color-primary-light); font-weight: 600; }
.upload-tip { color: rgba(255,255,255,0.35); font-size: 12px; margin-top: 8px; }

.upload-hint { display: inline-block; margin-left: 12px; color: var(--text-muted); font-size: 12px; vertical-align: middle; }
:deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 500; }
:deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px rgba(200, 160, 120, 0.15) inset;
}
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px rgba(232, 137, 75, 0.25) inset !important; }
:deep(.el-select .el-input__wrapper) { box-shadow: 0 0 0 1px rgba(200, 160, 120, 0.15) inset; }
</style>