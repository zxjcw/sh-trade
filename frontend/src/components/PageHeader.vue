<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    eyebrow?: string
  }>(),
  { subtitle: '', eyebrow: '' }
)
</script>

<template>
  <header class="page-header">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--sgj-accent-deep);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2px;
}

h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
  line-height: 1.2;
}

.subtitle {
  margin: 8px 0 0;
  color: var(--sgj-muted);
  font-size: 14px;
  line-height: 1.6;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
