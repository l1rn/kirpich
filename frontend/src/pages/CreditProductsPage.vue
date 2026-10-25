<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/api'
import DashboardTemplate from '@/components/templates/DashboardTemplate.vue'
import ProductCard from '@/components/molecules/ProductCard.vue'
import BaseButton from '@/components/atoms/BaseButton.vue'
import type { CreditProduct } from '@/types/creditProduct'

const router = useRouter()

const products = ref<CreditProduct[]>([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/credit-products', {
      params: { page: page.value, size: 6 },
    })
    products.value = data.content
    totalPages.value = data.totalPages
  } catch {
    error.value = 'Не удалось загрузить продукты'
  } finally {
    loading.value = false
  }
}

function go(delta: number) {
  page.value += delta
  load()
}

function select(p: CreditProduct) {
  router.push({ path: '/applications/new', query: { productId: p.id } })
}

onMounted(load)
</script>

<template>
  <DashboardTemplate>
    <header>
      <h1>Кредитные продукты</h1>
      <p>Выберите продукт, и мы подберём условия под ваш бизнес.</p>
    </header>

    <p v-if="error" class="muted">{{ error }}</p>
    <p v-else-if="!loading && products.length === 0" class="muted">Пока нет доступных продуктов.</p>

    <div class="grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" @select="select(p)" />
    </div>

    <div v-if="totalPages > 1" class="pager">
      <BaseButton :disabled="page === 0 || loading" @click="go(-1)">Назад</BaseButton>
      <span class="muted">{{ page + 1 }} из {{ totalPages }}</span>
      <BaseButton :disabled="page + 1 >= totalPages || loading" @click="go(1)">Далее</BaseButton>
    </div>
  </DashboardTemplate>
</template>

<style scoped>
h1 { font-size: 22px; }
header p { font-size: 13px; color: var(--muted); margin-top: 6px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.pager { display: flex; align-items: center; gap: 16px; }
.muted { font-size: 13px; color: var(--muted); }
</style>