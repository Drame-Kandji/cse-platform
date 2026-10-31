import { defineConfig, devices } from '@playwright/test'

/**
 * Tests de bout en bout des 3 scénarios critiques (spec §6.2) contre la pile qui TOURNE
 * (`make infra && make start && make front`). Aucun serveur n'est démarré par Playwright.
 */
export default defineConfig({
  testDir: './e2e/scenarios',
  // E2E_SKIP_LOGIN=1 : réutilise les sessions déjà présentes dans e2e/.auth (itérations locales).
  globalSetup: process.env.E2E_SKIP_LOGIN ? undefined : './e2e/global-setup.ts',
  outputDir: './test-results',
  timeout: 180_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    locale: 'fr-FR',
    timezoneId: 'Africa/Dakar',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
