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
const lot = computed(() => String((Math.abs(props.seed) % 900) + 1).padStart(3, '0'))
</script>

<template>
  <div class="cover" :style="{ background: coverGradient(seed) }">
    <span class="lot">LOT {{ lot }}</span>
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
  container-type: inline-size;
}

.cover::before {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 24% 16%, rgba(247, 243, 233, 0.09), transparent 42%),
    radial-gradient(circle at 82% 88%, rgba(0, 0, 0, 0.24), transparent 52%);
  content: '';
}

.cover::after {
  position: absolute;
  inset: 9px;
  border: 1px solid rgba(247, 243, 233, 0.13);
  content: '';
  pointer-events: none;
}

.lot {
  position: absolute;
  top: 13px;
  left: 15px;
  z-index: 1;
  color: rgba(247, 243, 233, 0.46);
  font-family: var(--sgj-font-body);
  font-size: 9px;
  letter-spacing: 0.24em;
}

.glyph {
  position: relative;
  z-index: 1;
  color: rgba(247, 243, 233, 0.62);
  font-family: var(--sgj-font-display);
  font-size: clamp(28px, 3.4vw, 56px);
  font-weight: 600;
  letter-spacing: 0.12em;
  text-align: center;
  transition: transform 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  user-select: none;
}

.cover:hover .glyph {
  transform: scale(1.04);
}

.badge {
  position: absolute;
  bottom: 13px;
  left: 15px;
  z-index: 1;
  padding: 1px 7px;
  border: 1px solid rgba(247, 243, 233, 0.28);
  border-radius: 0;
  color: rgba(247, 243, 233, 0.78);
  font-size: 10px;
  line-height: 18px;
  letter-spacing: 0.08em;
}

@container (max-width: 120px) {
  .lot,
  .badge {
    display: none;
  }

  .glyph {
    font-size: 18px;
    letter-spacing: 0.04em;
  }

  .cover::after {
    inset: 5px;
  }
}
</style>
