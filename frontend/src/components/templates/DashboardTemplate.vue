<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore.ts';
import BaseButton from '../atoms/BaseButton.vue';
import SidebarNav from '../organisms/SidebarNav.vue'
import BaseModal from '../atoms/BaseModal.vue';
import { ref } from 'vue';
import AuthForm from '../organisms/AuthForm.vue';

const auth = useAuthStore();
const showAuth = ref(false);
</script>

<template>
  <div class="layout">
    <SidebarNav />
    <main class="main"><slot /></main>
    <div v-if="!auth.isLoggedIn" class="login-button-container">
      <BaseButton variant="dark" @click="showAuth = true">Войти</BaseButton>
    </div>
    <div v-else>
      <BaseButton variant="dark" @click="auth.logout" class="login-button-container">
        Выйти
      </BaseButton>
    </div>
    <BaseModal :open="showAuth" @close="showAuth = false">
      <AuthForm @done="showAuth = false" />
    </BaseModal>
  </div>
</template>

<style scoped>
.layout { 
  display: flex; 
}

.main { 
  flex: 1; 
  padding: 32px; 
  display: grid; 
  gap: 28px; 
  align-content: start; 
  max-width: 860px; 
}

.login-button-container {
  position: absolute;
  top: 10px;
  right: 10px;
}
</style>
