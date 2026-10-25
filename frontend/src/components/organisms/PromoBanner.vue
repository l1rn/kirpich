<script setup lang="ts">
import { ref } from 'vue';
import BaseButton from '../atoms/BaseButton.vue'
import AuthForm from './AuthForm.vue';
import BaseModal from '../atoms/BaseModal.vue';
import { useAuthStore } from '@/stores/authStore.ts';
import { useRouter } from 'vue-router';

const auth = useAuthStore();
const showAuth = ref(false);
const router = useRouter();

const openAuth = () => {
  if(!auth.isLoggedIn){
    showAuth.value = true;
  }
  else{
    router.push('/applications/new');
  }
}
</script>

<template>
  <section class="banner">
    <h2>Нужен кредит для развития бизнеса?</h2>
    <p>Подберём подходящий продукт и поможем оформить заявку за несколько минут.</p>
    <BaseButton @click="openAuth">
      Подать заявку
    </BaseButton>
    <BaseModal :open="showAuth" @close="showAuth = false">
      <AuthForm @done="showAuth = false" />
    </BaseModal>
  </section>
</template>

<style scoped>
.banner { 
  background: linear-gradient(120deg, #eef0ff, #e1e3fb); 
  border-radius: var(--r-lg); 
  padding: 24px; 
  display: grid; 
  gap: 8px; 
  justify-items: start; 
}
h2 { font-size: 18px; }
p { font-size: 13px; color: var(--muted); max-width: 360px; margin-bottom: 8px; }
</style>
