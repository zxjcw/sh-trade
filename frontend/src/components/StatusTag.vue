<script setup lang="ts">
import { computed } from 'vue'
import {
  bountyStatusLabels,
  intentStatusLabels,
  itemStatusLabels,
  itemStatusTypes,
  planStatusLabels,
  tradeStatusLabels,
  tradeStatusTypes
} from '@/utils/status'
import type { ItemStatus, TradeStatus } from '@/types'

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

const props = defineProps<{
  kind: 'item' | 'trade' | 'bounty' | 'intent' | 'plan'
  status: string
}>()

const label = computed(() => {
  switch (props.kind) {
    case 'item':
      return itemStatusLabels[props.status as ItemStatus] ?? props.status
    case 'trade':
      return tradeStatusLabels[props.status as TradeStatus] ?? props.status
    case 'bounty':
      return bountyStatusLabels[props.status as keyof typeof bountyStatusLabels] ?? props.status
    case 'intent':
      return intentStatusLabels[props.status as keyof typeof intentStatusLabels] ?? props.status
    default:
      return planStatusLabels[props.status as keyof typeof planStatusLabels] ?? props.status
  }
})

const type = computed<TagType>(() => {
  if (props.kind === 'item') {
    return itemStatusTypes[props.status as ItemStatus] ?? 'info'
  }
  if (props.kind === 'trade') {
    return tradeStatusTypes[props.status as TradeStatus] ?? 'info'
  }
  if (props.kind === 'bounty') {
    return props.status === 'open' ? 'success' : props.status === 'matched' ? 'primary' : 'info'
  }
  if (props.kind === 'intent') {
    return props.status === 'active' ? 'success' : 'info'
  }
  return props.status === 'pending' ? 'warning' : props.status === 'active' ? 'success' : 'info'
})
</script>

<template>
  <el-tag :type="type" effect="plain" size="small">{{ label }}</el-tag>
</template>
