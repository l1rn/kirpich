import { http } from "@/api/api";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

interface Me { email: string; phone: string | null; }

export const useAuthStore = defineStore('auth', () => {
    const user = ref<Me | null>(null);
    const loaded = ref(false);
    const isLoggedIn = computed(() => user.value !== null);

    const fetchMe = async() => {
        try{
            const response = await http.get<Me>('/users/me');
            user.value = response.data;
        } catch{
            user.value = null;
        } finally{
            loaded.value = true;
        }
    }

    const login = async(email: string, password: string) => {
        await http.post('/auth/login', { email, password });
        await fetchMe();
    }

    const register = async(email: string, password: string) => {
        await http.post('/auth/register', { email, password});
        await login(email, password);
    }

    const logout = async() => {
        await http.post('/auth/logout');
        user.value = null;
    }

    return { 
        user,
        loaded,
        isLoggedIn,
        fetchMe,
        register, 
        login,
        logout,
    };
})