<script setup lang="ts">
import SectionHeader from '../molecules/SectionHeader.vue'
import ApplicationCard from '../molecules/ApplicationCard.vue'
</script>

<template>
  <section class="section">
    <SectionHeader title="Мои заявки" link="Все заявки" />
    <ApplicationCard number="123456" type="Оборотное кредитование" amount="2 000 000 ₽" rate="12,5%" daysLeft="2 дня" />
  </section>
</template>

<style scoped>
.section { display: grid; gap: 12px; }
</style>
