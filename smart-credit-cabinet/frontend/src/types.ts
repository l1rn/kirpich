// Типы ответов API — повторяют DTO бэкенда (backend/.../web/dto)

export type Role = 'DIRECTOR' | 'ACCOUNTANT' | 'UNDERWRITER';

export type Status =
  | 'DRAFT'
  | 'CHECKING'
  | 'NEEDS_INFO'
  | 'MANUAL_REVIEW'
  | 'OFFERED'
  | 'DECLINED'
  | 'ISSUED'
  | 'CANCELLED';

export interface Company {
  id: number;
  inn: string;
  name: string;
  legalForm: string;
  ogrn: string | null;
  okved: string | null;
  directorName: string | null;
  registeredAt: string | null;
}

export interface User {
  id: number;
  login: string;
  fullName: string;
  role: Role;
  company: Company | null;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  user: User;
}

export interface Option {
  code: string;
  label: string;
}

export interface Product {
  code: string;
  name: string;
  description: string;
  purposes: string[];
  minAmount: number;
  maxAmount: number;
  minTerm: number;
  maxTerm: number;
  baseRate: number;
  revenueRatio: number;
  autoApproveMax: number;
  requiredDocuments: string[];
  documentsRequiredAbove: number | null;
}

export interface Catalog {
  products: Product[];
  purposes: Option[];
  documents: Option[];
}

export interface Recommendation {
  product: Product;
  suitable: boolean;
  reasons: string[];
  estimatedMonthlyPayment: number | null;
}

export interface EgrulCompany {
  inn: string;
  ogrn: string;
  fullName: string;
  shortName: string;
  legalForm: string;
  status: string;
  registrationDate: string;
  address: string;
  okved: { code: string; name: string } | null;
  director: { fullName: string; inn: string; disqualified: boolean } | null;
}

export interface NextAction {
  actor: 'CLIENT' | 'BANK' | 'NONE';
  title: string;
  description: string;
}

export interface Summary {
  id: number;
  number: string;
  status: Status;
  statusLabel: string;
  productCode: string;
  productName: string;
  purpose: string;
  purposeLabel: string;
  amount: number;
  termMonths: number;
  createdAt: string;
  updatedAt: string;
  nextAction: NextAction;
}

export interface Check {
  step: string;
  label: string;
  status: 'PENDING' | 'RUNNING' | 'OK' | 'FAILED';
  attempts: number;
  error: string | null;
  updatedAt: string;
}

export interface Offer {
  id: number;
  kind: 'AS_REQUESTED' | 'COUNTER';
  productCode: string;
  productName: string;
  amount: number;
  termMonths: number;
  rate: number;
  monthlyPayment: number;
  status: 'ACTIVE' | 'ACCEPTED' | 'SIGNED' | 'REJECTED';
  createdAt: string;
  acceptedAt: string | null;
  signedAt: string | null;
}

export interface InfoItem {
  key: string;
  label: string;
  type: 'TEXT' | 'FILE';
}

export interface InfoRequest {
  id: number;
  requestedBy: 'SYSTEM' | 'UNDERWRITER';
  items: InfoItem[];
  note: string | null;
  status: 'OPEN' | 'ANSWERED';
  answers: Record<string, string> | null;
  createdAt: string;
  answeredAt: string | null;
}

export interface DocumentInfo {
  id: number;
  docType: string;
  docLabel: string;
  filename: string;
  sizeBytes: number;
  createdAt: string;
}

export interface AppEvent {
  id: number;
  type: string;
  fromStatus: Status | null;
  toStatus: Status | null;
  actorType: 'CLIENT' | 'SYSTEM' | 'UNDERWRITER';
  message: string;
  createdAt: string;
}

export interface Factor {
  code: string;
  label: string;
  value: string;
  points: number;
}

export interface ClientDecision {
  outcome: string;
  reasons: string[];
  score: number | null;
  grade: string | null;
  factors: Factor[];
}

export interface Detail {
  summary: Summary;
  stageIndex: number;
  stages: string[];
  revenue12m: number | null;
  existingMonthlyPayments: number;
  bkiConsentAt: string | null;
  submittedAt: string | null;
  decidedAt: string | null;
  checks: Check[];
  offer: Offer | null;
  infoRequest: InfoRequest | null;
  documents: DocumentInfo[];
  events: AppEvent[];
  decision: ClientDecision | null;
}

export interface AcceptOfferResponse {
  signingCode: string;
  message: string;
}

export interface BankRow {
  id: number;
  number: string;
  status: Status;
  statusLabel: string;
  companyName: string;
  companyInn: string;
  productCode: string;
  productName: string;
  amount: number;
  termMonths: number;
  score: number | null;
  grade: string | null;
  assignee: string | null;
  updatedAt: string;
}

export interface Source {
  step: string;
  label: string;
  status: Check['status'];
  attempts: number;
  error: string | null;
  data: Record<string, unknown> | null;
}

export interface Proposal {
  productCode: string;
  amount: number;
  termMonths: number;
  rate: number;
  monthlyPayment: number;
  kind: 'AS_REQUESTED' | 'COUNTER';
}

export interface DecisionFull {
  id: number;
  source: 'ENGINE' | 'UNDERWRITER';
  outcome: 'APPROVE' | 'COUNTER_OFFER' | 'MANUAL_REVIEW' | 'DECLINE';
  score: number | null;
  grade: string | null;
  factors: Factor[];
  stopFactors: { code: string; text: string }[];
  reasons: string[];
  proposal: Proposal | null;
  note: string | null;
  policyVersion: string | null;
  createdAt: string;
}

export interface BankDetail {
  application: Detail;
  company: Company;
  assignee: string | null;
  sources: Source[];
  decisions: DecisionFull[];
  infoRequests: InfoRequest[];
  version: number;
}

export interface ApplicationInput {
  productCode: string;
  purpose: string;
  amount: number;
  termMonths: number;
  revenue12m: number | null;
  existingMonthlyPayments: number;
}

export interface BankDecisionInput {
  outcome: 'APPROVE' | 'COUNTER_OFFER' | 'DECLINE';
  productCode?: string;
  amount?: number;
  termMonths?: number;
  rate?: number;
  reason?: string;
  note?: string;
}
