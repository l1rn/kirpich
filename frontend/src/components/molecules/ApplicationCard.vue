<script setup lang="ts">
import BaseBadge from '../atoms/BaseBadge.vue'
defineProps<{ number: string; type: string; amount: string; rate: string; daysLeft: string }>()
</script>

<template>
  <div class="card">
    <div class="row">
      <div>
        <p class="title">Заявка №{{ number }}</p>
        <p class="muted">{{ type }}</p>
      </div>
      <BaseBadge>На рассмотрении</BaseBadge>
    </div>
    <div class="row bottom">
      <p><strong>{{ amount }}</strong> <span class="muted">Ставка от {{ rate }}</span></p>
      <p class="muted">Осталось: {{ daysLeft }}</p>
    </div>
  </div>
</template>

<style scoped>
.card { border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; background: var(--surface); display: grid; gap: 16px; }
.row { display: flex; justify-content: space-between; align-items: flex-start; }
.bottom { align-items: baseline; }
.title { font-size: 13px; font-weight: 600; }
.muted { font-size: 12px; color: var(--muted); }
strong { font-size: 15px; margin-right: 8px; }
</style>
