import { expect, Page, test } from '@playwright/test'

const csrfBody = {
  success: true,
  data: { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'test-csrf-token' },
  error: null,
}

async function mockCsrf(page: Page) {
  await page.route('**/api/v1/auth/csrf', async (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(csrfBody),
  }))
}

test('회원가입을 완료하면 가입 완료 안내를 보여준다', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/v1/auth/register', async (route) => route.fulfill({
    status: 201,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { emailVerified: true }, error: null }),
  }))

  await page.goto('/signup')
  await page.getByLabel('이메일').fill('new@example.com')
  await page.getByLabel('닉네임').fill('new_user')
  await page.getByLabel('비밀번호', { exact: true }).fill('password123')
  await page.getByLabel('비밀번호 확인').fill('password123')
  await page.getByRole('button', { name: /회원가입/ }).click()

  await expect(page.getByText('가입이 완료됐어요')).toBeVisible()
  await expect(page.getByText('이제 바로 로그인해서 오늘의 시장을 확인할 수 있어요.')).toBeVisible()
})

test('로그인하면 대시보드로 이동하고 사용자 정보를 보여준다', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/v1/auth/login', async (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: {}, error: null }),
  }))
  await page.route('**/api/v1/users/me', async (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      success: true,
      data: { email: 'user@example.com', nickname: 'investor_1', role: 'USER', status: 'ACTIVE', emailVerified: true, createdAt: '2026-01-01T00:00:00Z' },
      error: null,
    }),
  }))

  await page.goto('/')
  await page.getByLabel('이메일').fill('user@example.com')
  await page.getByLabel('비밀번호').fill('password123')
  await page.getByRole('button', { name: /로그인/ }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByText('investor_1님, 반가워요.')).toBeVisible()
  await expect(page.getByText('user@example.com')).toBeVisible()
})

test('비밀번호 찾기와 재설정 화면을 사용할 수 있다', async ({ page }) => {
  await mockCsrf(page)
  await page.route('**/api/v1/auth/password-resets', async (route) => route.fulfill({
    status: 202,
    body: '',
  }))
  await page.route('**/api/v1/auth/password-resets/confirm', async (route) => route.fulfill({
    status: 204,
    body: '',
  }))

  await page.goto('/forgot-password')
  await page.getByLabel('이메일').fill('user@example.com')
  await page.getByRole('button', { name: /재설정 안내 받기/ }).click()
  await expect(page.getByText('요청을 접수했어요')).toBeVisible()

  await page.goto('/reset-password?token=test-token')
  await page.getByLabel('새 비밀번호', { exact: true }).fill('new-password123')
  await page.getByLabel('새 비밀번호 확인').fill('new-password123')
  await page.getByRole('button', { name: /비밀번호 변경/ }).click()
  await expect(page.getByText('비밀번호가 변경됐어요')).toBeVisible()
})
