export interface CreditProduct {
  id: number
  title: string
  minAmount: number
  maxAmount: number
  minTermMonths: number
  maxTermMonths: number
  interestRate: number
}