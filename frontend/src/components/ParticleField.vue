<template>
  <canvas ref="cv" class="particle-canvas" :style="{ opacity }"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(
  defineProps<{
    count?: number      // 粒子数量
    color?: string      // "r, g, b" 基础色，便于复用主题色
    maxRadius?: number  // 最大半径(px)
    speed?: number      // 漂移速度
    opacity?: number    // 整体透明度
  }>(),
  { count: 24, color: '232, 137, 75', maxRadius: 2.4, speed: 0.25, opacity: 0.55 },
)

const cv = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let raf = 0
let particles: {
  x: number; y: number; r: number; vx: number; vy: number; a: number; tw: number
}[] = []
let w = 0
let h = 0
let ro: ResizeObserver | null = null

const reduced =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

function resize() {
  const el = cv.value
  const parent = el?.parentElement
  if (!el || !parent || !ctx) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = parent.clientWidth
  h = parent.clientHeight
  el.width = Math.max(1, Math.floor(w * dpr))
  el.height = Math.max(1, Math.floor(h * dpr))
  el.style.width = w + 'px'
  el.style.height = h + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function initParticles() {
  particles = Array.from({ length: props.count }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    r: Math.random() * props.maxRadius + 0.6,
    vx: (Math.random() - 0.5) * props.speed,
    vy: (Math.random() - 0.5) * props.speed - 0.04, // 略微向上漂
    a: Math.random() * 0.5 + 0.3,
    tw: Math.random() * Math.PI * 2, // 闪烁相位
  }))
}

function render() {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    p.tw += 0.02
    if (p.x < -5) p.x = w + 5
    if (p.x > w + 5) p.x = -5
    if (p.y < -5) p.y = h + 5
    if (p.y > h + 5) p.y = -5
    const alpha = p.a * (0.6 + 0.4 * Math.sin(p.tw))
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${props.color},${alpha.toFixed(3)})`
    ctx.fill()
  }
  raf = requestAnimationFrame(render)
}

function renderStatic() {
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) {
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${props.color},${p.a})`
    ctx.fill()
  }
}

onMounted(() => {
  ctx = cv.value?.getContext('2d') || null
  if (!ctx) return
  resize()
  initParticles()
  if (reduced) {
    renderStatic()
    return
  }
  render()
  const parent = cv.value?.parentElement
  if (parent && 'ResizeObserver' in window) {
    ro = new ResizeObserver(() => resize())
    ro.observe(parent)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  ro?.disconnect()
})
</script>

<style scoped>
.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
