<script setup lang="ts">
import FormField from '../molecules/FormField.vue'
import BaseInput from '../atoms/BaseInput.vue'
import BaseSelect from '../atoms/BaseSelect.vue'
import BaseButton from '../atoms/BaseButton.vue'
</script>

<template>
  <div class="form">
    <h2>Основная информация</h2>
    <FormField label="Сумма кредита">
      <BaseInput placeholder="2 000 000 ₽" />
    </FormField>
    <FormField label="Срок">
      <BaseSelect :options="['12 месяцев', '24 месяца', '36 месяцев']" />
    </FormField>
    <FormField label="Цель финансирования">
      <BaseSelect :options="['Пополнение оборотных средств', 'Покупка оборудования']" />
    </FormField>
    <BaseButton>Далее</BaseButton>
  </div>
</template>

<style scoped>
.form { display: grid; gap: 16px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-lg); padding: 24px; max-width: 420px; }
h2 { font-size: 16px; }
</style>
