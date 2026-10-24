<script setup lang="ts">
import BaseLogo from '../atoms/BaseLogo.vue'
import NavItem from '../molecules/NavItem.vue'
</script>

<template>
  <aside class="sidebar">
    <BaseLogo />
    <nav>
      <NavItem label="Главная" active />
      <NavItem label="Заявки" />
      <NavItem label="Продукты" />
      <NavItem label="Документы" />
      <NavItem label="Профиль" />
    </nav>
    <p class="user">ИП Иванов А.А.</p>
  </aside>
</template>

<style scoped>
.sidebar { background: var(--navy); width: 220px; min-height: 100vh; padding: 24px 16px; display: flex; flex-direction: column; gap: 32px; }
nav { display: grid; gap: 4px; }
.user { margin-top: auto; color: #fff; font-size: 13px; }
@media (max-width: 720px) { .sidebar { display: none; } }
</style>
