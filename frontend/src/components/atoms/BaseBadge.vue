<script setup lang="ts">
defineProps<{ tone?: 'ok' | 'bad' | 'neutral' }>()
</script>

<template>
  <span class="badge" :class="tone ?? 'ok'"><slot /></span>
</template>

<style scoped>
.badge { font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 999px; white-space: nowrap; }
.ok { background: var(--ok-bg); color: var(--ok); }
.bad { background: #fde8e8; color: #d33; }
.neutral { background: #eceefa; color: var(--muted); }
</style>