<template>
  <DashboardTemplate>
    <h1>Моя компания</h1>
    <CompanyForm />
  </DashboardTemplate>
</template>

<script setup lang="ts">
import DashboardTemplate from '@/components/templates/DashboardTemplate.vue'
import CompanyForm from '@/components/organisms/CompanyForm.vue'
</script>

<style scoped>
h1 { font-size: 22px; }
</style>