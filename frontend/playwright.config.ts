import { defineConfig, devices } from '@playwright/test'

const backendCommand = process.env.PLAYWRIGHT_BACKEND_COMMAND ?? 'cd ../backend && ./gradlew bootRun'
const frontendCommand = process.env.PLAYWRIGHT_FRONTEND_COMMAND ?? 'npm run dev -- --host localhost --port 5173'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chrome',
      use: {
        ...devices['Desktop Chrome'],
        channel: process.env.PLAYWRIGHT_CHANNEL ?? 'chrome',
      },
    },
  ],
  webServer: [
    {
      command: backendCommand,
      url: 'http://localhost:8080/actuator/health',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: frontendCommand,
      url: 'http://localhost:5173/login',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
})
