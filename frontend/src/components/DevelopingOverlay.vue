<template>
  <div class="dev-overlay">
    <div class="dev-polaroid">
      <div class="dev-img photo-develop">
        <div class="dev-img-inner" />
      </div>
      <div class="dev-caption">{{ text }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), {
  text: '照片正在显影…',
})
</script>

<style scoped>
/* 主题化加载层：把等待包装成「拍立得正在冲洗」的暖色反馈 */
.dev-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 248, 240, 0.72);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  z-index: 5;
  border-radius: inherit;
}
.dev-polaroid {
  background: var(--bg-white);
  padding: 10px 10px 20px;
  border-radius: 4px;
  box-shadow: var(--shadow-polaroid);
  text-align: center;
  animation: fade-scale 0.4s ease-out;
}
.dev-img {
  width: 120px;
  height: 90px;
  border-radius: 2px;
  overflow: hidden;
  background: linear-gradient(135deg, #e8894b 0%, #d4956b 100%);
}
.dev-img-inner {
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 40% 35%, rgba(255, 255, 255, 0.4), transparent 60%);
}
.dev-caption {
  font-family: var(--font-hand);
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 10px;
}
</style>
