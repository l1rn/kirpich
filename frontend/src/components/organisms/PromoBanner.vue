<script setup lang="ts">
import BaseButton from '../atoms/BaseButton.vue'
</script>

<template>
  <section class="banner">
    <h2>Нужен кредит для развития бизнеса?</h2>
    <p>Подберём подходящий продукт и поможем оформить заявку за несколько минут.</p>
    <BaseButton>Подать заявку</BaseButton>
  </section>
</template>

<style scoped>
.banner { background: linear-gradient(120deg, #eef0ff, #e1e3fb); border-radius: var(--r-lg); padding: 24px; display: grid; gap: 8px; justify-items: start; }
h2 { font-size: 18px; }
p { font-size: 13px; color: var(--muted); max-width: 360px; margin-bottom: 8px; }
</style>
