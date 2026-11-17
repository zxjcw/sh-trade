<script setup lang="ts">
import { computed } from 'vue'
import { coverGradient } from '@/utils/status'

const props = withDefaults(
  defineProps<{
    seed?: number
    title?: string
    badge?: string
  }>(),
  { seed: 1, title: '', badge: '' }
)

const glyph = computed(() => props.title.trim().slice(0, 2) || '拾光')
</script>

<template>
  <div class="cover" :style="{ background: coverGradient(seed) }">
    <span class="glyph">{{ glyph }}</span>
    <span v-if="badge" class="badge">{{ badge }}</span>
  </div>
</template>

<style scoped>
.cover {
  position: relative;
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  overflow: hidden;
  isolation: isolate;
}

.cover::before {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 18%, rgba(255, 255, 255, 0.22) 0, transparent 32%),
    radial-gradient(circle at 82% 82%, rgba(0, 0, 0, 0.18) 0, transparent 44%);
  content: '';
}

.cover::after {
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    118deg,
    rgba(255, 255, 255, 0.045) 0,
    rgba(255, 255, 255, 0.045) 1px,
    transparent 1px,
    transparent 12px
  );
  content: '';
}

.glyph {
  position: relative;
  z-index: 1;
  color: rgba(255, 255, 255, 0.82);
  font-size: clamp(26px, 3vw, 42px);
  font-weight: 700;
  letter-spacing: 3px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  user-select: none;
}

.badge {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 1;
  padding: 2px 10px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.95);
  font-size: 12px;
  line-height: 20px;
  backdrop-filter: blur(6px);
}
</style>
