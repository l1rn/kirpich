<template>
  <section class="section">
    <SectionHeader title="Доступные продукты" link="Все продукты" to="/credit-products" />
    <p v-if="error" class="muted">{{ error }}</p>
    <div class="grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '@/api/api'
import SectionHeader from '../molecules/SectionHeader.vue'
import ProductCard from '../molecules/ProductCard.vue'
import type { CreditProduct } from '@/types/creditProduct'

const props = withDefaults(defineProps<{ limit?: number }>(), { limit: 3 })

const products = ref<CreditProduct[]>([])
const error = ref('')

onMounted(async () => {
  try {
    const { data } = await http.get('/credit-products', {
      params: { page: 0, size: props.limit, sort: 'id,asc' },
    })
    products.value = data.content
  } catch {
    error.value = 'Не удалось загрузить продукты'
  }
})
</script>

<style scoped>
.section { 
  display: grid; 
  gap: 12px; 
}
.grid { 
  display: grid; 
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); 
  gap: 12px; 
}
.muted { 
  font-size: 13px; 
  color: var(--muted); 
}
</style>