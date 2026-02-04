const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login page correctly', async ({ page }) => {
    await expect(page).toHaveTitle(/Pet Clinic/);
    // Check for the actual heading structure on the login page
    await expect(page.locator('h3')).toContainText(/Pet Clinic/i);
    await expect(page.locator('p')).toContainText(/Management System/i);
  });

  test('should validate required fields', async ({ page }) => {
    // Submit form without filling fields
    await page.click('button[type="submit"]');
    
    // Check for HTML5 validation or custom validation
    const usernameField = page.locator('input[name="username"]');
    const passwordField = page.locator('input[name="password"]');
    
    await expect(usernameField).toHaveAttribute('required');
    await expect(passwordField).toHaveAttribute('required');
  });

  test('should attempt login with various credential combinations', async ({ page }) => {
    const credentials = [
      { username: 'admin', password: 'admin123' },
      { username: 'vet1', password: 'vet123' },
      { username: 'staff1', password: 'staff123' },
      { username: 'vet2', password: 'vet123' },
      { username: 'staff2', password: 'staff123' }
    ];

    for (const cred of credentials) {
      console.log(`Testing credentials: ${cred.username}/${cred.password}`);
      
      await page.fill('input[name="username"]', cred.username);
      await page.fill('input[name="password"]', cred.password);
      await page.click('button[type="submit"]');
      
      // Wait for response
      await page.waitForTimeout(2000);
      
      const currentUrl = page.url();
      console.log(`URL after ${cred.username} login:`, currentUrl);
      
      // If successful login (not on login page), test logout
      if (!currentUrl.includes('/login')) {
        console.log(`✓ Login successful for ${cred.username}`);
        
        // Try to find logout button/link
        const logoutButton = page.locator('a[href*="logout"], button:has-text("logout"), .logout');
        if (await logoutButton.count() > 0) {
          await logoutButton.first().click();
          await page.waitForTimeout(1000);
        } else {
          // Navigate back to login manually
          await page.goto('/login');
        }
      } else {
        console.log(`✗ Login failed for ${cred.username}`);
      }
      
      // Clear fields for next attempt
      await page.fill('input[name="username"]', '');
      await page.fill('input[name="password"]', '');
    }
  });

  test('should handle invalid credentials gracefully', async ({ page }) => {
    await page.fill('input[name="username"]', 'invalid_user');
    await page.fill('input[name="password"]', 'wrong_password');
    await page.click('button[type="submit"]');
    
    await page.waitForTimeout(2000);
    
    // Should stay on login page or show error
    const currentUrl = page.url();
    expect(currentUrl).toContain('login');
  });

  test('should check CSRF protection', async ({ page }) => {
    // Check if CSRF token is present in the form
    const csrfToken = page.locator('input[name="_token"], input[name="csrf_token"], input[name="_csrf"]');
    
    if (await csrfToken.count() > 0) {
      console.log('✓ CSRF protection detected');
      await expect(csrfToken.first()).toHaveAttribute('value');
    } else {
      console.log('ℹ No CSRF token found in login form');
    }
  });
});