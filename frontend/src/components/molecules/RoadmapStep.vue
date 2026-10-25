<!-- components/molecules/RoadmapStep.vue -->
<script setup lang="ts">
import type { JourneyStep } from '@/types/journey'

defineProps<{ step: JourneyStep; index: number }>()
</script>

<template>
  <li class="step" :class="step.state" :aria-current="step.state === 'current' ? 'step' : undefined">
    <span class="marker">
      <template v-if="step.state === 'done'">✓</template>
      <template v-else-if="step.state === 'failed'">!</template>
      <template v-else>{{ index + 1 }}</template>
    </span>
    <h3>{{ step.title }}</h3>
    <p class="hint">{{ step.hint }}</p>
    <RouterLink v-if="step.to" :to="step.to">{{ step.action }}</RouterLink>
  </li>
</template>

<style scoped>
.step { position: relative; display: grid; gap: 6px; align-content: start; }
.marker { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; font-size: 13px; font-weight: 700; background: #eceefa; color: var(--muted); position: relative; z-index: 1; }
.step:not(:last-child)::after { content: ''; position: absolute; top: 13px; left: 36px; right: -8px; height: 2px; background: var(--line); }
.done:not(:last-child)::after { background: var(--ok); }
.done .marker { background: var(--ok); color: #fff; }
.current .marker { background: var(--accent); color: #fff; }
.failed .marker { background: #d33; color: #fff; }
h3 { font-size: 14px; margin-top: 4px; }
.hint { font-size: 12px; color: var(--muted); }
.todo h3 { color: var(--muted); }
.failed .hint { color: #d33; }
a { font-size: 12px; font-weight: 600; color: var(--accent); }
@media (max-width: 720px) { .step:not(:last-child)::after { display: none; } }
</style>    