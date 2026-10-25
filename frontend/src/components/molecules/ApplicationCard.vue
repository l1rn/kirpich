<script setup lang="ts">
import { computed } from 'vue'
import BaseBadge from '../atoms/BaseBadge.vue'
import { rub, statusMeta } from '@/utils/application'
import type { Application } from '@/types/application'

const props = defineProps<{ application: Application }>()
const meta = computed(() => statusMeta(props.application.status))
</script>

<template>
  <div class="card">
    <div class="row">
      <div>
        <p class="title">Заявка №{{ application.id }}</p>
        <p class="muted">{{ application.productTitle }}</p>
      </div>
      <BaseBadge :tone="meta.tone">{{ meta.label }}</BaseBadge>
    </div>
    <p class="amount">{{ rub(application.requestedAmount) }}</p>
  </div>
</template>

<style scoped>
.card { border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; background: var(--surface); display: grid; gap: 12px; }
.row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.title { font-size: 13px; font-weight: 600; }
.muted { font-size: 12px; color: var(--muted); }
.amount { font-size: 16px; font-weight: 700; }
</style>