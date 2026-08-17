<template>
  <div class="stats-page grain-overlay" v-loading="loading">
    <!-- ── 概览卡片：拍立得风格 ── -->
    <div class="polaroid-row stagger-in">
      <div
        v-for="(card, i) in ovCards"
        :key="card.label"
        class="polaroid-card card-polaroid polaroid-in"
        :style="{ '--tilt': (i % 2 === 0 ? -1.8 : 1.6) + 'deg', animationDelay: (i * 0.08) + 's' }"
      >
        <div class="pol-icon-wrap" :style="{ background: card.bg }">
          <span class="pol-icon">{{ card.icon }}</span>
        </div>
        <div class="pol-num count-anim">{{ card.value }}</div>
        <div class="pol-label">{{ card.label }}</div>
      </div>
    </div>

    <!-- ── 第一行：年度趋势 + 情绪分布（唱盘图） ── -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <div class="chart-card card-warm">
          <div class="chart-hd">
            <span class="chart-hd-icon">📊</span>
            <span>年度记忆趋势</span>
            <span class="chart-hd-line"></span>
          </div>
          <div ref="c1" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card card-warm vinyl-card">
          <div class="chart-hd">
            <span class="chart-hd-icon">😊</span>
            <span>情绪分布</span>
            <span class="chart-hd-line"></span>
          </div>
          <div ref="c2" class="chart-box vinyl-chart"></div>
        </div>
      </el-col>
    </el-row>

    <!-- ── 第二行：月度活跃度 + 地点分布 ── -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <div class="chart-card card-warm">
          <div class="chart-hd">
            <span class="chart-hd-icon">🗓️</span>
            <span>月度活跃度</span>
            <span class="chart-hd-line"></span>
          </div>
          <div ref="c3" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card card-warm">
          <div class="chart-hd">
            <span class="chart-hd-icon">🗺️</span>
            <span>地点分布</span>
            <span class="chart-hd-line"></span>
          </div>
          <div ref="c4" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- ── 第三行：类型分布 + 最近记忆 ── -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="10">
        <div class="chart-card card-warm">
          <div class="chart-hd">
            <span class="chart-hd-icon">🏷️</span>
            <span>类型分布</span>
            <span class="chart-hd-line"></span>
          </div>
          <div ref="c5" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="14">
        <div class="chart-card card-warm">
          <div class="chart-hd">
            <span class="chart-hd-icon">🕐</span>
            <span>最近记忆</span>
            <span class="chart-hd-line"></span>
          </div>
          <el-table :data="d.recentMemories || []" size="small" class="warm-table" stripe>
            <el-table-column prop="title" label="标题" min-width="140">
              <template #default="{ row }">
                <span class="mem-title">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="70">
              <template #default="{ row }">
                <el-tag size="small" :type="tagType(row.type)">{{ tl(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="情绪" width="80">
              <template #default="{ row }">
                <span class="emotion-tag">{{ row.emotion || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="日期" width="110">
              <template #default="{ row }">
                <span class="mem-date">{{ (row.date || '').substring(0, 10) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboardApi } from '@/api/stats'

const d = ref<any>({
  totalMemories: 0,
  yearlyStats: [],
  monthlyStats: [],
  locationStats: [],
  emotionStats: [],
  typeStats: [],
  recentMemories: [],
})
const loading = ref(false)
const c1 = ref<HTMLElement>()
const c2 = ref<HTMLElement>()
const c3 = ref<HTMLElement>()
const c4 = ref<HTMLElement>()
const c5 = ref<HTMLElement>()

const ovCards = computed(() => [
  { icon: '📚', bg: 'rgba(232,137,75,0.08)', value: d.value.totalMemories || 0, label: '记忆总数' },
  { icon: '📍', bg: 'rgba(212,149,107,0.08)', value: (d.value.locationStats || []).length, label: '涉及地点' },
  { icon: '📅', bg: 'rgba(240,167,107,0.08)', value: (d.value.yearlyStats || []).length, label: '跨越年度' },
  { icon: '📈', bg: 'rgba(232,201,168,0.10)', value: (d.value.monthlyStats || []).length, label: '活跃月份' },
])

onMounted(async () => {
  loading.value = true
  try {
    const r: any = await getDashboardApi()
    d.value = r.data || d.value
    await nextTick()
    renderAll()
  } catch (e: any) {
    ElMessage.error(e?.message)
  } finally {
    loading.value = false
  }
})

// ── 暖色调色板 ──
const warmPalette = {
  amber: '#E8894B',
  amberLight: '#F0A76B',
  amberDark: '#C56E38',
  cream: '#E8C9A8',
  rose: '#D4956B',
  walnut: '#4A3528',
  paper: '#FFF8F0',
  gold: '#D4A76A',
  terra: '#B8734A',
}

const renderAll = () => {
  renderYearlyTrend()
  renderEmotionVinyl()
  renderMonthlyActivity()
  renderLocationBar()
  renderTypeDonut()
}

// ── 年度记忆趋势：暖色渐变柱状图 + 曲线 ──
const renderYearlyTrend = () => {
  makeChart(c1.value, {
    tooltip: { trigger: 'axis', backgroundColor: '#FFFDF9', borderColor: 'rgba(200,160,120,0.3)', textStyle: { color: '#4A3528' } },
    grid: { top: 20, right: 30, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: (d.value.yearlyStats || []).map((x: any) => x.year + '年'),
      axisLine: { lineStyle: { color: 'rgba(200,160,120,0.3)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8B7355', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(200,160,120,0.1)', type: 'dashed' } },
      axisLabel: { color: '#8B7355', fontSize: 11 },
    },
    series: [
      {
        name: '记忆',
        type: 'bar',
        data: (d.value.yearlyStats || []).map((x: any) => x.count),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: warmPalette.amberLight },
            { offset: 0.5, color: warmPalette.amber },
            { offset: 1, color: warmPalette.amberDark },
          ]),
          borderRadius: [8, 8, 0, 0],
          borderColor: 'rgba(255,255,255,0.3)',
          borderWidth: 1,
        },
        barWidth: '50%',
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#F5B87A' },
              { offset: 1, color: '#D47842' },
            ]),
          },
        },
      },
      {
        type: 'line',
        data: (d.value.yearlyStats || []).map((x: any) => x.count),
        smooth: true,
        lineStyle: { color: warmPalette.rose, width: 2.5, type: 'solid' },
        itemStyle: { color: warmPalette.rose },
        symbol: 'circle',
        symbolSize: 7,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(212,149,107,0.15)' },
            { offset: 1, color: 'rgba(212,149,107,0.0)' },
          ]),
        },
      },
    ],
  })
}

// ── 情绪分布：唱盘环形图 ──
const renderEmotionVinyl = () => {
  const emotionColors: any = {
    '开心': '#E8894B',
    '兴奋': '#F0A76B',
    '感动': '#D4956B',
    '温馨': '#E8C9A8',
    '平静': '#C5A88C',
    '思考': '#B8937A',
    '低落': '#A08070',
  }
  const data = (d.value.emotionStats || []).map((x: any) => ({
    name: x.name,
    value: x.value,
    itemStyle: { color: emotionColors[x.name] || '#B8A088' },
  }))

  makeChart(c2.value, {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#FFFDF9',
      borderColor: 'rgba(200,160,120,0.3)',
      textStyle: { color: '#4A3528' },
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8B7355', fontSize: 10 },
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 12,
    },
    series: [
      // 外层唱盘纹路
      {
        type: 'pie',
        radius: ['80%', '82%'],
        center: ['50%', '42%'],
        silent: true,
        label: { show: false },
        data: [{ value: 1, itemStyle: { color: 'rgba(200,160,120,0.12)' } }],
      },
      {
        type: 'pie',
        radius: ['76%', '78%'],
        center: ['50%', '42%'],
        silent: true,
        label: { show: false },
        data: [{ value: 1, itemStyle: { color: 'rgba(200,160,120,0.08)' } }],
      },
      {
        type: 'pie',
        radius: ['72%', '74%'],
        center: ['50%', '42%'],
        silent: true,
        label: { show: false },
        data: [{ value: 1, itemStyle: { color: 'rgba(200,160,120,0.10)' } }],
      },
      // 主数据环
      {
        type: 'pie',
        radius: ['38%', '70%'],
        center: ['50%', '42%'],
        roseType: 'area',
        itemStyle: {
          borderRadius: 4,
          borderColor: '#FFFDF9',
          borderWidth: 2.5,
        },
        label: {
          fontSize: 10,
          color: '#8B7355',
          formatter: '{b}\n{d}%',
        },
        labelLine: {
          lineStyle: { color: 'rgba(200,160,120,0.4)' },
        },
        emphasis: {
          scaleSize: 8,
          label: { fontSize: 13, fontWeight: 'bold' },
        },
        data,
      },
      // 中心唱片标签
      {
        type: 'pie',
        radius: ['0%', '36%'],
        center: ['50%', '42%'],
        silent: true,
        label: { show: false },
        data: [
          {
            value: 1,
            itemStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                { offset: 0, color: '#FFFDF9' },
                { offset: 0.7, color: '#FFF8F0' },
                { offset: 1, color: '#F5EDE0' },
              ]),
              borderColor: 'rgba(200,160,120,0.2)',
              borderWidth: 1.5,
              borderRadius: 8,
            },
          },
        ],
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '36%',
        style: {
          text: '😊',
          textAlign: 'center',
          fontSize: 18,
        },
      },
    ],
  })
}

// ── 月度活跃度：暖色面积图 ──
const renderMonthlyActivity = () => {
  makeChart(c3.value, {
    tooltip: { trigger: 'axis', backgroundColor: '#FFFDF9', borderColor: 'rgba(200,160,120,0.3)', textStyle: { color: '#4A3528' } },
    grid: { top: 20, right: 30, bottom: 35, left: 55 },
    xAxis: {
      type: 'category',
      data: (d.value.monthlyStats || []).map((x: any) => x.month),
      axisLabel: { rotate: 45, fontSize: 10, color: '#8B7355' },
      axisLine: { lineStyle: { color: 'rgba(200,160,120,0.3)' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(200,160,120,0.08)', type: 'dashed' } },
      axisLabel: { color: '#8B7355', fontSize: 11 },
    },
    series: [
      {
        type: 'line',
        data: (d.value.monthlyStats || []).map((x: any) => x.count),
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(232,137,75,0.25)' },
            { offset: 0.6, color: 'rgba(232,137,75,0.06)' },
            { offset: 1, color: 'rgba(232,137,75,0.0)' },
          ]),
        },
        lineStyle: { color: warmPalette.amber, width: 2.5 },
        itemStyle: { color: warmPalette.amber },
        symbol: 'circle',
        symbolSize: 4,
        emphasis: {
          focus: 'series',
          itemStyle: { borderWidth: 2, borderColor: '#fff' },
        },
      },
    ],
  })
}

// ── 地点分布：暖色横向柱状图 ──
const renderLocationBar = () => {
  const loc = (d.value.locationStats || []).slice(0, 12).reverse()
  makeChart(c4.value, {
    tooltip: { trigger: 'axis', backgroundColor: '#FFFDF9', borderColor: 'rgba(200,160,120,0.3)', textStyle: { color: '#4A3528' } },
    grid: { top: 10, right: 35, bottom: 10, left: 70 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(200,160,120,0.08)', type: 'dashed' } },
      axisLabel: { color: '#8B7355', fontSize: 10 },
    },
    yAxis: {
      type: 'category',
      data: loc.map((x: any) => x.name),
      axisLabel: { fontSize: 11, color: '#4A3528' },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    series: [
      {
        type: 'bar',
        data: loc.map((x: any, i: number) => ({
          value: x.value,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: warmPalette.amber },
              { offset: 1, color: `hsl(${25 + i * 2}, 65%, ${62 + i * 1.5}%)` },
            ]),
            borderRadius: [0, 6, 6, 0],
            borderColor: 'rgba(255,255,255,0.3)',
            borderWidth: 0.5,
          },
        })),
        barWidth: '60%',
        label: { show: true, position: 'right', fontSize: 10, color: '#8B7355' },
      },
    ],
  })
}

// ── 类型分布：暖色环形图 ──
const renderTypeDonut = () => {
  const typeColors = [warmPalette.amber, warmPalette.amberLight, warmPalette.rose, warmPalette.cream, warmPalette.gold, warmPalette.terra, warmPalette.walnut]
  makeChart(c5.value, {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#FFFDF9',
      borderColor: 'rgba(200,160,120,0.3)',
      textStyle: { color: '#4A3528' },
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8B7355', fontSize: 10 },
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 14,
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '72%'],
        center: ['50%', '42%'],
        itemStyle: {
          borderRadius: 4,
          borderColor: '#FFFDF9',
          borderWidth: 2.5,
        },
        label: { fontSize: 11, color: '#8B7355', formatter: '{b}\n{d}%' },
        labelLine: { lineStyle: { color: 'rgba(200,160,120,0.4)' } },
        emphasis: {
          scaleSize: 6,
          label: { fontSize: 13, fontWeight: 'bold' },
        },
        data: (d.value.typeStats || []).map((x: any, i: number) => ({
          name: tl(x.name),
          value: x.value,
          itemStyle: { color: typeColors[i % typeColors.length] },
        })),
      },
    ],
  })
}

const makeChart = (el: any, opt: any) => {
  if (!el) return
  const c = echarts.init(el)
  c.setOption(opt)
  window.addEventListener('resize', () => c.resize())
}

const tl = (t: string) =>
  (
    {
      photo: '照片',
      video: '视频',
      diary: '日记',
      travel: '旅行',
      study: '学习',
      daily: '日常',
      note: '笔记',
      thought: '感悟',
    } as any
  )[t] || t

const tagType = (t: string) => {
  const m: any = { photo: 'warning', video: 'danger', diary: '', travel: 'success', study: 'info', daily: '', note: '', thought: 'info' }
  return m[t] || ''
}
</script>

<style scoped>
.stats-page {
  max-width: 1200px;
  position: relative;
}

/* ── 拍立得概览卡片 ── */
.polaroid-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.polaroid-card {
  cursor: default;
  min-height: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.pol-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pol-icon {
  font-size: 20px;
  line-height: 1;
}

.pol-num {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.pol-label {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-hand);
  letter-spacing: 0.5px;
}

/* ── 图表行 ── */
.chart-row {
  margin-bottom: 16px;
}

.chart-row:last-child {
  margin-bottom: 0;
}

/* ── 图表卡片 ── */
.chart-card {
  padding: 20px;
  margin-bottom: 0;
  height: 100%;
  position: relative;
  overflow: hidden;
}

/* ── 图表标题 ── */
.chart-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(200, 160, 120, 0.12);
}

.chart-hd-icon {
  font-size: 16px;
}

.chart-hd-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(200, 160, 120, 0.15) 0%,
    transparent 100%
  );
  margin-left: 4px;
}

/* ── 图表容器 ── */
.chart-box {
  height: 280px;
}

/* ── 唱盘卡片特殊样式 ── */
.vinyl-card {
  background: linear-gradient(135deg, #FFFDF9 0%, #FFF8F0 50%, #FDF5EA 100%);
}

.vinyl-chart {
  height: 300px;
}

/* ── 暖色表格 ── */
.warm-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(232, 137, 75, 0.04);
  --el-table-row-hover-bg-color: rgba(232, 137, 75, 0.05);
  --el-table-border-color: rgba(200, 160, 120, 0.1);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-text-color: var(--text-primary);
  font-size: 13px;
}

.warm-table :deep(.el-table__header th) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
}

.warm-table :deep(.el-table__body td) {
  padding: 8px 0;
  border-bottom: 1px solid rgba(200, 160, 120, 0.06);
}

.mem-title {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-primary);
}

.emotion-tag {
  font-size: 12px;
  color: var(--text-secondary);
}

.mem-date {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-hand);
}

/* ── ElTag 暖色覆盖 ── */
:deep(.el-tag) {
  border-radius: var(--radius-sm);
  font-size: 11px;
  border: 1px solid rgba(200, 160, 120, 0.15);
  background: rgba(232, 137, 75, 0.06);
  color: var(--text-secondary);
}

:deep(.el-tag--warning) {
  background: rgba(232, 137, 75, 0.08);
  border-color: rgba(232, 137, 75, 0.2);
  color: #C56E38;
}

:deep(.el-tag--success) {
  background: rgba(180, 160, 130, 0.08);
  border-color: rgba(180, 160, 130, 0.2);
  color: #7A6B58;
}

:deep(.el-tag--danger) {
  background: rgba(200, 120, 90, 0.08);
  border-color: rgba(200, 120, 90, 0.2);
  color: #B06850;
}

:deep(.el-tag--info) {
  background: rgba(180, 160, 140, 0.06);
  border-color: rgba(180, 160, 140, 0.15);
  color: #8B7355;
}

/* ── responsiv ── */
@media (max-width: 1024px) {
  .polaroid-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .polaroid-row {
    grid-template-columns: 1fr;
  }
}
</style>