<script setup lang="ts">
defineProps<{ variant?: 'primary' | 'dark' }>()
</script>

<template>
  <button class="btn" :class="variant ?? 'primary'"><slot /></button>
</template>

<style scoped>
.btn { 
  border: 0; 
  padding: 12px 20px; 
  font-weight: 600; 
  font-size: 14px; 
  color: #fff; 
  cursor: pointer; 
  border-radius: var(--r-sm); 
}

.primary { 
  background: linear-gradient(90deg, var(--accent), var(--accent-2)); 
}

.dark { 
  background: var(--navy); 
  border-radius: 999px; 
}

.btn:focus-visible { 
  outline: 2px solid var(--accent); 
  outline-offset: 2px; 
}
</style>
