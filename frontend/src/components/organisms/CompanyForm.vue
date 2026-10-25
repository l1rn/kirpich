<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { http } from '@/api/api'
import FormField from '../molecules/FormField.vue'
import BaseInput from '../atoms/BaseInput.vue'
import BaseButton from '../atoms/BaseButton.vue'
import type { Company } from '@/types/company.js'

const form = reactive<Company>({ name: '', inn: '', ogrn: '', legalAddress: '', revenue: '' })
let snapshot: Company = { ...form }

const hasCompany = ref(false)
const editing = ref(false)
const error = ref('')
const loading = ref(false)

const locked = computed(() => loading.value || (hasCompany.value && !editing.value))

const errorText = (e: any, fallback: string) => {
  const msg = e?.response?.data
  return typeof msg === 'string' && msg ? msg : fallback
}

const getMy = async () => {
  error.value = ''
  loading.value = true
  try {
    const { data } = await http.get('/companies/my-company')
    if (data) {
      Object.assign(form, data)
      snapshot = { ...form }
      hasCompany.value = true
    }
  } catch (e: any) {
    error.value = errorText(e, 'Не удалось получить компанию')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  error.value = ''
  loading.value = true
  const payload = {
    name: form.name,
    inn: form.inn,
    ogrn: form.ogrn,
    legalAddress: form.legalAddress,
    revenue: Number(form.revenue),
  }
  try {
    if (hasCompany.value) {
      await http.put('/companies/change', payload)
      editing.value = false
    } else {
      await http.post('/companies/create', payload)
      hasCompany.value = true
    }
    snapshot = { ...form }
  } catch (e: any) {
    error.value = errorText(e, 'Не удалось сохранить компанию')
  } finally {
    loading.value = false
  }
}

const cancel = () => {
  Object.assign(form, snapshot)
  editing.value = false
  error.value = ''
}

onMounted(getMy)
</script>

<template>
  <form class="form" @submit.prevent="submit">
    <h2>Данные компании</h2>
    <FormField label="Название">
      <BaseInput v-model="form.name" :disabled="locked" required />
    </FormField>
    <FormField label="ИНН">
      <BaseInput v-model="form.inn" :disabled="locked" inputmode="numeric" maxlength="12" required />
    </FormField>
    <FormField label="ОГРН">
      <BaseInput v-model="form.ogrn" :disabled="locked" inputmode="numeric" maxlength="15" required />
    </FormField>
    <FormField label="Юридический адрес">
      <BaseInput v-model="form.legalAddress" :disabled="locked" required />
    </FormField>
    <FormField label="Годовая выручка, ₽">
      <BaseInput v-model="form.revenue" :disabled="locked" type="number" min="0" required />
    </FormField>

    <p v-if="error" class="error">{{ error }}</p>

    <BaseButton v-if="!hasCompany" :disabled="loading">Сохранить</BaseButton>

    <BaseButton v-else-if="!editing" type="button" @click="editing = true">Редактировать</BaseButton>

    <div v-else class="actions">
      <BaseButton :disabled="loading">Сохранить</BaseButton>
      <button type="button" class="cancel" @click="cancel">Отмена</button>
    </div>
  </form>
</template>

<style scoped>
.form { display: grid; gap: 16px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); padding: 24px; max-width: 420px; }
h2 { font-size: 16px; }
.error { font-size: 13px; color: #d33; }
.actions { display: flex; align-items: center; gap: 16px; }
.cancel { border: 0; background: none; color: var(--muted); font-weight: 600; font-size: 14px; cursor: pointer; }
</style>