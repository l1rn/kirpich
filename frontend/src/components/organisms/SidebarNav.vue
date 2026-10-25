<script setup lang="ts">
import { computed } from 'vue';
import BaseLogo from '../atoms/BaseLogo.vue'
import NavItem from '../molecules/NavItem.vue'
import { useAuthStore } from '@/stores/authStore.ts';

interface NavItemType {
  label: string;
  to: string;
  auth: boolean;
}

const auth = useAuthStore();

const navItems: NavItemType[] = [
  { label: 'Главная', to: '/home', auth: false },
  { label: 'Заявки', to: '/applications/new', auth: true },
  { label: 'Продукты', to: '/credit-products', auth: false },
  { label: 'Документы', to: '/company', auth: true },
  { label: 'Профиль', to: '/me', auth: true }
];

const visibleItem = computed(() => 
  navItems.filter(i => !i.auth || auth.isLoggedIn)
);
</script>

<template>
  <aside class="sidebar">
    <BaseLogo />
    <nav>
      <NavItem v-for="i in visibleItem" :key="i.to" :label="i.label" :to="i.to" />      
    </nav>
    <div>

    </div>
  </aside>
</template>

<style scoped>
.sidebar { 
  background: var(--navy); 
  width: 220px; 
  min-height: 100vh; 
  padding: 24px 16px; 
  display: flex; 
  flex-direction: column; 
  gap: 32px; 
}
nav { 
  display: grid; 
  gap: 4px; 
}
.user { 
  margin-top: auto; 
  color: #fff; 
  font-size: 13px; 
}
@media (max-width: 720px) { 
  .sidebar { display: none; } 
}
</style>
