<script setup lang="ts">
defineProps<{ title: string; limit: string }>()
</script>

<template>
  <div class="card">
    <div class="icon" />
    <h3>{{ title }}</h3>
    <p>{{ limit }}</p>
  </div>
</template>

<style scoped>
.card { border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; background: var(--surface); }
.icon { width: 36px; height: 36px; border-radius: var(--r-sm); background: #e8e9ff; margin-bottom: 14px; }
h3 { font-size: 14px; }
p { font-size: 12px; color: var(--muted); margin-top: 4px; }
</style>
