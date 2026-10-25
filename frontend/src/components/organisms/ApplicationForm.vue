<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/api/api'
import FormField from '../molecules/FormField.vue'
import BaseInput from '../atoms/BaseInput.vue'
import BaseButton from '../atoms/BaseButton.vue'
import type { CreditProduct } from '@/types/creditProduct'
import type { Application } from '@/types/application'

const emit = defineEmits<{ done: [result: Application] }>()
const productId = Number(useRoute().query.productId)

const product = ref<CreditProduct | null>(null)
const amount = ref('')
const term = ref('')
const error = ref('')
const loading = ref(false)

const rub = (n: number) => new Intl.NumberFormat('ru-RU').format(n) + ' ₽'

onMounted(async () => {
  if (!productId) return
  try {
    const { data } = await http.get<CreditProduct>(`/credit-products/${productId}`)
    product.value = data
    amount.value = String(data.minAmount)
    term.value = String(data.minTermMonths)
  } catch {
    error.value = 'Продукт не найден'
  }
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const { data } = await http.post<Application>('/applications/create', {
      creditProductId: productId,
      requestedAmount: Number(amount.value),
      termMonths: Number(term.value),
    })
    emit('done', data)
  } catch (e: any) {
    const msg = e?.response?.data
    error.value = typeof msg === 'string' && msg ? msg : 'Не удалось отправить заявку'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="!productId" class="form">
    <p>Сначала выберите продукт.</p>
    <RouterLink to="/credit-products">К продуктам</RouterLink>
  </div>

  <form v-else-if="product" class="form" @submit.prevent="submit">
    <h2>{{ product.title }}</h2>
    <FormField :label="`Сумма: ${rub(product.minAmount)} – ${rub(product.maxAmount)}`">
      <BaseInput v-model="amount" type="number" :min="product.minAmount" :max="product.maxAmount" required />
    </FormField>
    <FormField :label="`Срок, мес.: ${product.minTermMonths} – ${product.maxTermMonths}`">
      <BaseInput v-model="term" type="number" :min="product.minTermMonths" :max="product.maxTermMonths" required />
    </FormField>

    <p v-if="error" class="error">
      {{ error }}
      <RouterLink v-if="error.includes('компани')" to="/company">Добавить компанию</RouterLink>
    </p>
    <BaseButton :disabled="loading">Подать заявку</BaseButton>
  </form>

  <p v-else class="muted">{{ error || 'Загрузка…' }}</p>
</template>

<style scoped>
.form { display: grid; gap: 16px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); padding: 24px; max-width: 420px; }
h2 { font-size: 16px; }
.error { font-size: 13px; color: #d33; }
.muted { font-size: 13px; color: var(--muted); }
a { color: var(--accent); font-weight: 600; font-size: 13px; }
</style>