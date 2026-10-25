<script setup lang="ts">
import DashboardTemplate from '../components/templates/DashboardTemplate.vue'
import PromoBanner from '../components/organisms/PromoBanner.vue'
import ApplicationsSection from '../components/organisms/ApplicationsSection.vue'
import ProductsSection from '../components/organisms/ProductsSection.vue'
import { useAuthStore } from '@/stores/authStore.ts'

const auth = useAuthStore();
</script>

<template>
  <DashboardTemplate>
    <header>
      <h1>Здравствуйте!</h1>
      <p>Здесь вы можете подать заявку на кредит, отслеживать её статус и управлять финансами бизнеса.</p>
    </header>
    <PromoBanner />
    <ApplicationsSection v-if="auth.isLoggedIn" />
    <ProductsSection />
  </DashboardTemplate>
</template>

<style scoped>
h1 { 
  font-size: 22px; 
}

header p { 
  font-size: 13px; 
  color: var(--muted); 
  margin-top: 6px; 
}
</style>
