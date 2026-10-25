<script setup lang="ts">
import ApplicationHistoryCard from '../molecules/ApplicationHistoryCard.vue'
import BaseButton from '../atoms/BaseButton.vue'
import { useMyApplications } from '@/composables/useMyApplications'

const { items, loading, error, hasMore, loadMore } = useMyApplications(5)
</script>

<template>
  <section class="section">
    <h2>История заявок</h2>
    <p v-if="error" class="muted">{{ error }}</p>
    <p v-else-if="!loading && items.length === 0" class="muted">Вы ещё не подавали заявок.</p>
    <ApplicationHistoryCard v-for="a in items" :key="a.id" :application="a" />
    <BaseButton v-if="hasMore" :disabled="loading" @click="loadMore">Показать ещё</BaseButton>
  </section>
</template>

<style scoped>
.section { display: grid; gap: 12px; max-width: 520px; }
h2 { font-size: 18px; }
.muted { font-size: 13px; color: var(--muted); }
</style>