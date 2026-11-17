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
  padding: 1px 7px;
  border: 1px solid var(--sgj-line);
  border-radius: 1px;
  background: var(--sgj-brass-soft);
  color: var(--sgj-brass);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.countdown.expired {
  border-color: var(--sgj-line);
  background: var(--sgj-paper-deep);
  color: var(--sgj-faint);
}
</style>
