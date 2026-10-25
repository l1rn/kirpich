<template>
    <DashboardTemplate>
        <header>
            <h1>
                <span>
                    <User width="36" height="36" />Профиль
                </span>
            </h1>
            <div class="email-container">
                <span>
                    <Mail width="24" height="24" />
                </span> 
                <div>
                    {{ auth.user?.email }}
                </div>
            </div>
            <div class="phone-container">
                <span><Phone width="24" height="24" /></span>
                <div>{{ auth.user?.phone ?? "Не указан"}}</div>
            </div>
        </header>
        <body>
            <ApplicationHistory />
        </body>
    </DashboardTemplate>
</template>

<script setup lang="ts">
import ApplicationHistory from '@/components/organisms/ApplicationHistory.vue';
import DashboardTemplate from '@/components/templates/DashboardTemplate.vue';
import { useAuthStore } from '@/stores/authStore';
import { Mail, Phone, User } from '@lucide/vue';

const auth = useAuthStore();
</script>

<style lang="scss" scoped>
@use "../assets/styles/static/mixins" as *;

header {
    display: grid;
    gap: 8px;
    h1 {
        margin-bottom: 1rem;

        span{
            display: flex;
            gap: 12px;
        }
    }
    .email-container, .phone-container {
        display: flex;
        gap: 12px;
        span{
            @include flex-center();
        }
        div{
            @include flex-center();
            font-size: 20px;
        }
    }
}
</style>