<!-- molecules/ApplicationHistoryCard.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import BaseBadge from '../atoms/BaseBadge.vue'
import VerificationSteps from './VerificationSteps.vue'
import { date, rub, statusMeta } from '@/utils/application'
import type { Application } from '@/types/application'

const props = defineProps<{ application: Application }>()
const meta = computed(() => statusMeta(props.application.status))
</script>

<template>
  <article class="card">
    <header>
      <div>
        <h3>Заявка №{{ application.id }}</h3>
        <p class="muted">{{ date(application.createdAt) }}</p>
      </div>
      <BaseBadge :tone="meta.tone">{{ meta.label }}</BaseBadge>
    </header>

    <p class="product">{{ application.productTitle }}</p>
    <dl>
      <div><dt>Сумма</dt><dd>{{ rub(application.requestedAmount) }}</dd></div>
      <div><dt>Срок</dt><dd>{{ application.termMonths }} мес.</dd></div>
    </dl>

    <VerificationSteps :bki="application.bkiStatus" :egrul="application.egrulStatus" :reasons="application.reasons" />
  </article>
</template>

<style scoped>
.card { border: 1px solid var(--line); border-radius: var(--r-lg); padding: 20px; background: var(--surface); display: grid; gap: 16px; }
header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
h3 { font-size: 15px; }
.muted { font-size: 12px; color: var(--muted); margin-top: 2px; }
.product { font-size: 14px; font-weight: 600; }
dl { display: flex; gap: 32px; }
dt { font-size: 12px; color: var(--muted); }
dd { margin: 2px 0 0; font-size: 16px; font-weight: 700; }
</style>