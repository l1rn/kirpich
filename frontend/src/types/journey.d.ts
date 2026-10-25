export type StepState = 'done' | 'current' | 'failed' | 'todo'

export interface JourneyStep {
  title: string
  hint: string
  state: StepState
  to?: string        
  action?: string
}