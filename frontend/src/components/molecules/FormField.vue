<script setup lang="ts">
defineProps<{ label: string }>()
</script>

<template>
  <label class="field">
    <span class="label">{{ label }}</span>
    <slot />
  </label>
</template>

<style scoped>
.field { display: grid; gap: 6px; }
.label { font-size: 13px; color: var(--muted); }
</style>
