<script setup lang="ts">
import { useRouter } from 'vue-router';

defineProps<{ title: string; link: string }>()
const router = useRouter();

</script>

<template>
  <div class="head">
    <h2>{{ title }}</h2>
    <a @click="router.push('/me')">{{ link }}</a>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: baseline; }
h2 { font-size: 16px; }
a { 
  font-size: 12px; 
  font-weight: 600; 
  color: var(--accent); 
  text-decoration: none;
  cursor: pointer;
}
</style>
