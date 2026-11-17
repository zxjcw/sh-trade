<script setup lang="ts">
withDefaults(defineProps<{ description?: string }>(), { description: '这里还没有内容' })
</script>

<template>
  <div class="empty-wrap">
    <el-empty :description="description" :image-size="90" />
    <div class="action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.action {
  display: flex;
  justify-content: center;
}
</style>
