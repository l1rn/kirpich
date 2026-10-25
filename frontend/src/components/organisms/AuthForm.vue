<template>
    <form class="form" @submit.prevent="submit">
        <div class="tabs">
            <button type="button" :class="{ on: mode === 'login' }" @click="mode = 'login'">Вход</button>
            <button type="button" :class="{ on: mode === 'register' }" @click="mode = 'register'">Регистрация</button>
        </div>
        <FormField label="Email">
            <BaseInput v-model="email" type="email" placeholder="example@example.com" required />
        </FormField>

        <FormField label="Пароль">
            <BaseInput v-model="password" type="password" placeholder="asdPaSSw0r$d" required />
        </FormField>
        <p v-if="error" class="error">{{ error }}</p>
        
        <BaseButton :disabled="loading">
            {{ mode === 'login' ? 'Войти' : 'Зарегистрироваться' }}
        </BaseButton>
    </form>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import FormField from '../molecules/FormField.vue';
import BaseInput from '../atoms/BaseInput.vue';
import BaseButton from '../atoms/BaseButton.vue';
import { useAuthStore } from '@/stores/authStore.ts';

const error = ref('');
const mode = ref<'login' | 'register'>('login');
const email = ref('');
const password = ref('');
const loading = ref(false);

const auth = useAuthStore();

const emit = defineEmits<{ done: [] }>();

const submit = async() => {
    loading.value = true;
    error.value = '';
    try{
        if(mode.value === 'register') await auth.register(email.value, password.value);
        else await auth.login(email.value, password.value);
        password.value = '';
        emit('done');
    }
    catch(e: any){
        const msg = e?.response?.data;
        error.value = typeof msg === 'string' && msg ? msg : 'Не удалось выполнить запрос';
    }
    finally{
        loading.value = false;
    }
}
</script>

<style lang="scss" scoped>
.form { 
    display: grid; 
    gap: 16px;
}
.tabs { 
    display: grid; 
    grid-template-columns: 1fr 1fr; 
    background: var(--bg); 
    border-radius: var(--r-sm); 
    padding: 4px; 
}

.tabs button { 
    border: 0; 
    background: none; 
    padding: 8px; 
    border-radius: 8px; 
    font-weight: 600; 
    font-size: 13px; 
    color: var(--muted); 
    cursor: pointer; 
}

.tabs .on { 
    background: var(--surface); 
    color: var(--text); 
}

.error { 
    font-size: 13px; 
    color: #d33; 
}
</style>