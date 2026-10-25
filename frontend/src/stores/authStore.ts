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
            const response = await http.post<Me>('/users/me');
            user.value = response.data;
        } catch{
            user.value = null;
        } finally{
            loaded.value = true;
        }
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
        logout
    };
})