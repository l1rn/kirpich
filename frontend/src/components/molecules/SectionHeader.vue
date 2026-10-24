<script setup lang="ts">
defineProps<{ title: string; link: string }>()
</script>

<template>
  <div class="head">
    <h2>{{ title }}</h2>
    <a href="#">{{ link }}</a>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: baseline; }
h2 { font-size: 16px; }
a { font-size: 12px; font-weight: 600; color: var(--accent); text-decoration: none; }
</style>
