const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - Basic Functionality', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto('/');
  });

  test('should load the homepage and redirect to login', async ({ page }) => {
    // Should redirect to login page
    await expect(page).toHaveURL(/.*\/login/);
    await expect(page).toHaveTitle(/Pet Clinic/);
  });

  test('should display login form', async ({ page }) => {
    // Check if login form elements are present
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should show validation errors for empty login', async ({ page }) => {
    // Try to submit empty form
    await page.click('button[type="submit"]');
    
    // Check for validation messages or error states
    // This will depend on your specific implementation
    await expect(page.locator('input[name="username"]')).toBeFocused();
  });

  test('should attempt login with demo credentials', async ({ page }) => {
    // Try logging in with correct demo credentials
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    
    // Wait for navigation or error message
    await page.waitForTimeout(2000);
    
    // Check if we're redirected to dashboard or get an error
    const currentUrl = page.url();
    console.log('Current URL after login attempt:', currentUrl);
  });

  test('should check backend API health', async ({ request }) => {
    // Test backend health endpoint
    const response = await request.get('http://localhost:9090/actuator/health');
    expect(response.status()).toBe(200);
    
    const healthData = await response.json();
    expect(healthData.status).toBe('UP');
  });

  test('should check validation service availability', async ({ request }) => {
    // Test validation service endpoint
    const response = await request.get('http://localhost:8081/api/validation/owners/health');
    expect(response.status()).toBe(200);
    
    const validationData = await response.json();
    expect(validationData.available).toBe(true);
  });
});