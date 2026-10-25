<!-- organisms/ApplicationsSection.vue -->
<script setup lang="ts">
import SectionHeader from '../molecules/SectionHeader.vue'
import ApplicationCard from '../molecules/ApplicationCard.vue'
import { useMyApplications } from '@/composables/useMyApplications.ts';

const { items, error } = useMyApplications(3)
</script>

<template>
  <section class="section">
    <SectionHeader title="Мои заявки" link="Вся история" to="/me" />
    <p v-if="error" class="muted">{{ error }}</p>
    <p v-else-if="items.length === 0" class="muted">
      Заявок пока нет. <RouterLink to="/credit-products">Выберите продукт</RouterLink>, чтобы подать первую.
    </p>
    <ApplicationCard v-for="a in items" :key="a.id" :application="a" />
  </section>
</template>

<style scoped>
.section { display: grid; gap: 12px; }
.muted { font-size: 13px; color: var(--muted); }
a { color: var(--accent); font-weight: 600; }
</style>