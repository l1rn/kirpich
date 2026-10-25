import CreditProductsPage from '@/pages/CreditProductsPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import DocumentsPage from '@/pages/DocumentsPage.vue'
import NewApplicationPage from '@/pages/NewApplicationPage.vue'
import ProfilePage from '@/pages/ProfilePage.vue'
import { useAuthStore } from '@/stores/authStore'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    { path: '/home', component: DashboardPage },
    { path: '/credit-products', component: CreditProductsPage},
    { path: '/applications/new', component: NewApplicationPage, meta: { requiresAuth: true } },
    { path: '/me', component: ProfilePage, meta: { requiresAuth: true } },
    { path: '/company', component: DocumentsPage, meta: { requiresAuth: true } }
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if(!auth.loaded) await auth.fetchMe();
  if(to.meta.requiresAuth && !auth.isLoggedIn) return '/';
})

export default router
