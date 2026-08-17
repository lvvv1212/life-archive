// v-tilt 指令：给卡片添加「跟随光标的暖色高光 + 轻微 3D 视差倾斜」。
// 设计上完全复用 LifeArchive 的暖色胶片风格（--color-primary / 拍立得质感），
// 且不引入任何第三方库；在 prefers-reduced-motion 下自动禁用。

const reduced =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

interface TiltHandlers {
  onMove: (e: MouseEvent) => void
  onLeave: () => void
  release: () => void
}

export const vTilt = {
  mounted(el: HTMLElement) {
    // 尊重无障碍设置：直接跳过，不做任何动效
    if (reduced) return

    if (!el.classList.contains('card-interactive')) el.classList.add('card-interactive')

    // 缓存卡片自身的基础倾斜角（polaroid-in 用到的 --tilt 变量）
    const baseTilt = getComputedStyle(el).getPropertyValue('--tilt').trim() || '0deg'

    // 入场动画 polaroid-in 用了 animation-fill-mode: forwards，会锁死 transform，
    // 导致后续的 hover/tilt 失效。动画结束后释放它，让 transform 回归可交互状态。
    const release = () => {
      if (el.classList.contains('polaroid-in')) {
        el.classList.remove('polaroid-in')
        el.style.opacity = '1'
      }
      el.removeEventListener('animationend', release)
    }
    el.addEventListener('animationend', release)

    const onMove = (e: MouseEvent) => {
      const r = el.getBoundingClientRect()
      const px = (e.clientX - r.left) / r.width
      const py = (e.clientY - r.top) / r.height
      el.style.setProperty('--mx', (px * 100).toFixed(1) + '%')
      el.style.setProperty('--my', (py * 100).toFixed(1) + '%')
      const ry = (px - 0.5) * 10 // 左右倾斜
      const rx = (0.5 - py) * 10 // 上下倾斜
      el.style.transform = `perspective(700px) rotateX(${rx.toFixed(2)}deg) rotateY(${ry.toFixed(2)}deg) rotate(${baseTilt}) scale(1.02)`
    }
    const onLeave = () => {
      el.style.transform = `perspective(700px) rotateX(0deg) rotateY(0deg) rotate(${baseTilt})`
    }

    el.addEventListener('mousemove', onMove)
    el.addEventListener('mouseleave', onLeave)
    ;(el as any)._tiltHandlers = { onMove, onLeave, release } as TiltHandlers
  },
  unmounted(el: HTMLElement) {
    const h = (el as any)._tiltHandlers as TiltHandlers | undefined
    if (h) {
      el.removeEventListener('mousemove', h.onMove)
      el.removeEventListener('mouseleave', h.onLeave)
      el.removeEventListener('animationend', h.release)
    }
  },
}
