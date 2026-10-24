<script setup lang="ts">
defineProps<{ label: string; active?: boolean }>()
</script>

<template>
  <a href="#" class="nav" :class="{ active }">{{ label }}</a>
</template>

<style scoped>
.nav { display: block; padding: 10px 14px; border-radius: var(--r-sm); color: #aab0c8; text-decoration: none; font-size: 14px; }
.active { background: #252b4a; color: #fff; }
</style>
