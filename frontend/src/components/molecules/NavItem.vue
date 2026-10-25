<script setup lang="ts">
defineProps<{ label: string; to: string }>()
</script>

<template>
  <RouterLink :to="to" class="nav" exact-active-class="active">{{ label }}</RouterLink>
</template>

<style scoped>
.nav { 
  display: block; 
  padding: 10px 14px; 
  border-radius: var(--r-sm); 
  color: #aab0c8; 
  text-decoration: none; 
  font-size: 14px;
  border-radius: 8px;
  font-size: 16px;

}
.active { 
  background: #252b4a; 
  color: #fff; 
}
</style>
