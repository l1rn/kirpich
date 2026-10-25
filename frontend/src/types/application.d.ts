export interface Application {
  id: number
  status: 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  productTitle: string
  requestedAmount: number
  termMonths: number
  bkiStatus: string
  egrulStatus: string
  reasons: string
}