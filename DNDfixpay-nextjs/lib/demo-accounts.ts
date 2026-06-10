export interface DemoCredential {
  label: string
  identifier: string
  password: string
}

export const APP_DEMO_USERS: DemoCredential[] = [
  {
    label: 'Consumer Seed',
    identifier: 'seed.user@fixpay.test',
    password: 'Password123!',
  },
  {
    label: 'Alt Consumer Seed',
    identifier: '08010000002',
    password: 'Password123!',
  },
]

export const ADMIN_DEMO_USERS: DemoCredential[] = [
  {
    label: 'Admin Seed',
    identifier: 'seed.admin@fixpay.test',
    password: 'Password123!',
  },
]

export const PORTAL_DEMO_USERS: DemoCredential[] = [
  {
    label: 'Portal Seed',
    identifier: 'seed.portal@fixpay.test',
    password: 'Password123!',
  },
]
