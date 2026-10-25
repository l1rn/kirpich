<script setup lang="ts">
defineProps<{ variant?: 'primary' | 'dark' }>()
</script>

<template>
  <button class="btn" :class="variant ?? 'primary'"><slot /></button>
</template>

<style scoped scss>
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
  background: var(--accent); 
}

.dark { 
  background: var(--navy); 
  border-radius: 999px;
  transition: 0.3s all; 
  &:hover{
    background: #1b203d;
  }
}

.btn:focus-visible { 
  outline: 2px solid var(--accent); 
  outline-offset: 2px; 
}
</style>
