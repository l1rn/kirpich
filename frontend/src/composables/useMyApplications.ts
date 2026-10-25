import { computed, onMounted, ref } from 'vue'
import { http } from '@/api/api'
import type { Application } from '@/types/application'

export function useMyApplications(size: number) {
  const items = ref<Application[]>([])
  const page = ref(0)
  const totalPages = ref(0)
  const loading = ref(false)
  const error = ref('')
  const hasMore = computed(() => page.value + 1 < totalPages.value)

  async function load() {
    loading.value = true
    error.value = ''
    try {
      const { data } = await http.get('/applications/my-history', {
        params: { page: page.value, size, sort: 'createdAt,desc' },
      })
      items.value.push(...data.content)
      totalPages.value = data.totalPages
    } catch {
      error.value = 'Не удалось загрузить заявки'
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    page.value++
    await load()
  }

  onMounted(load)
  return { items, loading, error, hasMore, loadMore }
}