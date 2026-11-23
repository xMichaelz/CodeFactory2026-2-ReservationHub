import { defineConfig } from '@playwright/test';

/**
 * Pruebas E2E contra el stack levantado (por defecto el de Docker Compose en :3001).
 *   E2E_BASE_URL=http://localhost:5173 npx playwright test   # contra `npm run dev`
 * Usa el Chrome instalado (channel) para no descargar navegadores.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3001',
    channel: process.env.E2E_BROWSER_CHANNEL ?? 'chrome',
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
  },
});
