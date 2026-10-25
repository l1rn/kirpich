<script setup lang="ts">
import BaseButton from '../atoms/BaseButton.vue'
import type { CreditProduct } from '@/types/creditProduct'

defineProps<{ product: CreditProduct }>()
const emit = defineEmits<{ select: [] }>()

const mln = (n: number) => `${new Intl.NumberFormat('ru-RU').format(n / 1_000_000)} млн ₽`
</script>

<template>
  <div class="card">
    <div class="icon" />
    <h3>{{ product.title }}</h3>
    <dl>
      <div><dt>Сумма</dt><dd>до {{ mln(product.maxAmount) }}</dd></div>
      <div><dt>Ставка</dt><dd>от {{ product.interestRate }}%</dd></div>
      <div><dt>Срок</dt><dd>{{ product.minTermMonths }}–{{ product.maxTermMonths }} мес.</dd></div>
    </dl>
    <BaseButton @click="emit('select')">Подать заявку</BaseButton>
  </div>
</template>

<style scoped>
.card { border: 1px solid var(--line); border-radius: var(--r-md); padding: 16px; background: var(--surface); display: grid; gap: 14px; align-content: start; }
.icon { width: 36px; height: 36px; border-radius: var(--r-sm); background: #e8e9ff; }
h3 { font-size: 14px; }
dl { display: grid; gap: 6px; }
dl div { display: flex; justify-content: space-between; font-size: 12px; }
dt { color: var(--muted); }
dd { margin: 0; font-weight: 600; }
</style>