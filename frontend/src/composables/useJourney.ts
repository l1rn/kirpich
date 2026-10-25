import { computed, onMounted, ref, watch } from 'vue'
import { http } from '@/api/api'
import { useAuthStore } from '@/stores/authStore'
import type { Application } from '@/types/application'
import type { JourneyStep, StepState } from '@/types/journey'

export function useJourney() {
  const auth = useAuthStore()
  const hasCompany = ref(false)
  const latest = ref<Application | null>(null)

  async function load() {
    hasCompany.value = false
    latest.value = null
    if (!auth.isLoggedIn) return
    try {
      const [company, apps] = await Promise.all([
        http.get('/companies/my-company'),
        http.get('/applications/my-history', {
          params: { page: 0, size: 1, sort: 'createdAt,desc' },
        }),
      ])
      hasCompany.value = !!company.data          
      latest.value = apps.data.content[0] ?? null
    } catch {
    }
  }

  const progress = computed(() => {
    if (!auth.isLoggedIn) return 0
    if (!hasCompany.value) return 1
    if (!latest.value) return 2
    return 3
  })

  const steps = computed<JourneyStep[]>(() => {
    const p = progress.value
    const app = latest.value

    const base = (i: number): StepState => (i < p ? 'done' : i === p ? 'current' : 'todo')

    let verifyState: StepState = base(3)
    let verifyHint = 'Результат появится после заявки'
    let verifyLink: Pick<JourneyStep, 'to' | 'action'> = {}

    if (app?.status === 'APPROVED') {
      verifyState = 'done'
      verifyHint = 'Проверка пройдена'
    } else if (app?.status === 'REJECTED') {
      verifyState = 'failed'
      verifyHint = app.reasons || 'Проверка не пройдена'
      verifyLink = { to: '/company', action: 'Проверить данные компании' }
    } else if (app) {
      verifyHint = 'Проверяем данные'
    }

    return [
      {
        title: '👤 Аккаунт',
        hint: auth.isLoggedIn ? 'Вы вошли в кабинет' : 'Нажмите «Войти» или зарегистрируйтесь',
        state: base(0),
      },
      {
        title: '🏢 Компания',
        hint: p > 1 ? 'Данные компании сохранены' : 'Добавьте данные компании',
        state: base(1),
        ...(p === 1 && { to: '/company', action: 'Добавить компанию' }),
      },
      {
        title: '📝 Заявка',
        hint: p > 2 ? 'Заявка отправлена' : 'Выберите продукт и подайте заявку',
        state: base(2),
        ...(p === 2 && { to: '/credit-products', action: 'Выбрать продукт' }),
      },
      { title: '✅ Проверка', hint: verifyHint, state: verifyState, ...verifyLink },
    ]
  })

  onMounted(load)
  watch(() => auth.isLoggedIn, load)

  return { steps }
}