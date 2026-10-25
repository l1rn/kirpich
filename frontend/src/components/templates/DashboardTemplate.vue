<script setup lang="ts">
import SidebarNav from '../organisms/SidebarNav.vue'
</script>

<template>
  <div class="layout">
    <SidebarNav />
    <main class="main"><slot /></main>
  </div>
</template>

<style scoped>
.layout { 
  display: flex; 
}

.main { 
  flex: 1; 
  padding: 32px; 
  display: grid; 
  gap: 28px; 
  align-content: start; 
  max-width: 860px; 
}
</style>
