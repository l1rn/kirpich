<script setup lang="ts">
defineProps<{ options: string[] }>()
</script>

<template>
  <select class="select">
    <option v-for="o in options" :key="o">{{ o }}</option>
  </select>
</template>

<style scoped>
.select { width: 100%; padding: 12px 14px; border: 1px solid var(--line); border-radius: var(--r-sm); background: var(--surface); }
.select:focus-visible { outline: 2px solid var(--accent); outline-offset: 1px; }
</style>
