<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { formatCountdown, remainingSeconds } from '@/utils/format'

const props = defineProps<{ deadline?: string }>()
const remaining = ref(0)
let timer: number | undefined

function tick() {
  remaining.value = remainingSeconds(props.deadline)
}

onMounted(() => {
  tick()
  timer = window.setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

const text = computed(() => formatCountdown(remaining.value))
const expired = computed(() => remaining.value <= 0)
</script>

<template>
  <span v-if="deadline" class="countdown" :class="{ expired }">{{ text }}</span>
</template>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 6px;
  background: #fdf3e3;
  color: var(--sgj-accent-deep);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.countdown.expired {
  background: #f4f4f4;
  color: #9a9a9a;
}
</style>
