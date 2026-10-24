<script setup lang="ts">
import SectionHeader from '../molecules/SectionHeader.vue'
import ProductCard from '../molecules/ProductCard.vue'
</script>

<template>
  <section class="section">
    <SectionHeader title="Доступные продукты" link="Все продукты" />
    <div class="grid">
      <ProductCard title="Оборотное кредитование" limit="до 50 млн ₽" />
      <ProductCard title="Инвестиционный кредит" limit="до 200 млн ₽" />
      <ProductCard title="Кредитная линия" limit="до 100 млн ₽" />
    </div>
  </section>
</template>

<style scoped>
.section { display: grid; gap: 12px; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
</style>
