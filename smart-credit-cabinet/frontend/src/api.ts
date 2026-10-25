import type {
  AcceptOfferResponse,
  ApplicationInput,
  BankDecisionInput,
  BankDetail,
  BankRow,
  Catalog,
  Detail,
  EgrulCompany,
  InfoItem,
  Recommendation,
  Status,
  Summary,
  TokenResponse,
  User,
} from './types';

const TOKEN_KEY = 'scc.token';

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // Хранилище недоступно (приватный режим) — токен живёт до перезагрузки страницы
  }
}

/** Ошибка API: errors — сообщения из Problem Details, их можно показать пользователю как есть. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly errors: string[],
  ) {
    super(errors.join('; '));
  }
}

let onUnauthorized: () => void = () => {};

export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  let payload: BodyInit | undefined;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  const response = await fetch(`/api/v1${path}`, { method, headers, body: payload });
  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (response.status === 401 && path !== '/auth/login') {
    onUnauthorized();
  }
  if (!response.ok) {
    const problem = (data ?? {}) as { errors?: string[]; detail?: string; message?: string };
    const errors = problem.errors ?? [problem.detail ?? problem.message ?? `Ошибка ${response.status}`];
    throw new ApiError(response.status, errors);
  }
  return data as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

export const api = {
  login: (login: string, password: string) => request<TokenResponse>('POST', '/auth/login', { login, password }),
  me: () => request<User>('GET', '/auth/me'),

  catalog: () => request<Catalog>('GET', '/products'),
  recommend: (purpose: string, amount: number, urgent: boolean) =>
    request<Recommendation[]>('POST', '/products/recommendations', { purpose, amount, urgent }),
  lookupCompany: (inn: string) => request<EgrulCompany>('GET', `/companies/lookup?inn=${encodeURIComponent(inn)}`),

  applications: () => request<Summary[]>('GET', '/applications'),
  application: (id: number) => request<Detail>('GET', `/applications/${id}`),
  createApplication: (input: ApplicationInput) => request<Detail>('POST', '/applications', input),
  submit: (id: number) => request<Detail>('POST', `/applications/${id}/submit`, { bkiConsent: true }),
  cancel: (id: number) => request<Detail>('POST', `/applications/${id}/cancel`),
  upload: (id: number, docType: string, file: File) => {
    const form = new FormData();
    form.append('docType', docType);
    form.append('file', file);
    return request<Detail>('POST', `/applications/${id}/documents`, form);
  },
  respond: (id: number, requestId: number, answers: Record<string, string>) =>
    request<Detail>('POST', `/applications/${id}/info-requests/${requestId}/response`, { answers }),
  acceptOffer: (id: number) => request<AcceptOfferResponse>('POST', `/applications/${id}/offer/accept`),
  signOffer: (id: number, code: string) => request<Detail>('POST', `/applications/${id}/offer/sign`, { code }),
  rejectOffer: (id: number) => request<Detail>('POST', `/applications/${id}/offer/reject`),

  bankQueue: (status?: Status) => request<BankRow[]>('GET', `/bank/applications${status ? `?status=${status}` : ''}`),
  bankApplication: (id: number) => request<BankDetail>('GET', `/bank/applications/${id}`),
  assign: (id: number) => request<BankDetail>('POST', `/bank/applications/${id}/assign`),
  decide: (id: number, input: BankDecisionInput) => request<BankDetail>('POST', `/bank/applications/${id}/decision`, input),
  requestInfo: (id: number, items: Pick<InfoItem, 'label' | 'type'>[], note?: string) =>
    request<BankDetail>('POST', `/bank/applications/${id}/info-requests`, { items, note }),
};

/** Документ отдаётся только с токеном, поэтому скачиваем через fetch и открываем как blob. */
export async function downloadDocument(applicationId: number, documentId: number, filename: string): Promise<void> {
  const token = getToken();
  const response = await fetch(`/api/v1/applications/${applicationId}/documents/${documentId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    throw new ApiError(response.status, ['Не удалось скачать документ']);
  }
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
