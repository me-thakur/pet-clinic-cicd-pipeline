const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - Corrected Functionality Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should successfully login with correct credentials', async ({ page }) => {
    console.log('🔐 Testing login with correct admin credentials...');
    
    // Fill in the correct admin credentials
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    
    // Submit the form
    await page.click('button[type="submit"]');
    
    // Wait for navigation
    await page.waitForTimeout(3000);
    
    const currentUrl = page.url();
    console.log(`Current URL after login: ${currentUrl}`);
    
    // Should not be on login page anymore
    expect(currentUrl).not.toContain('/login');
    
    // Take screenshot of successful login
    await page.screenshot({ 
      path: 'playwright-tests/screenshots/successful-admin-login.png', 
      fullPage: true 
    });
  });

  test('should test demo account auto-fill functionality', async ({ page }) => {
    console.log('🖱️ Testing demo account auto-fill...');
    
    // Click on the admin demo card
    await page.click('.card.bg-light:has-text("Admin")');
    
    // Wait for auto-fill
    await page.waitForTimeout(1000);
    
    // Check if fields are auto-filled
    const usernameValue = await page.inputValue('input[name="username"]');
    const passwordValue = await page.inputValue('input[name="password"]');
    
    expect(usernameValue).toBe('admin');
    expect(passwordValue).toBe('admin123');
    
    console.log('✓ Auto-fill functionality working correctly');
  });

  test('should test all demo accounts', async ({ page }) => {
    const demoAccounts = [
      { role: 'Admin', username: 'admin', password: 'admin123' },
      { role: 'Vet', username: 'vet1', password: 'vet123' },
      { role: 'Staff', username: 'staff1', password: 'staff123' },
      { role: 'Vet 2', username: 'vet2', password: 'vet123' },
      { role: 'Staff 2', username: 'staff2', password: 'staff123' }
    ];

    for (const account of demoAccounts) {
      console.log(`🔐 Testing ${account.role} login: ${account.username}/${account.password}`);
      
      await page.fill('input[name="username"]', account.username);
      await page.fill('input[name="password"]', account.password);
      await page.click('button[type="submit"]');
      
      await page.waitForTimeout(3000);
      
      const currentUrl = page.url();
      
      if (!currentUrl.includes('/login')) {
        console.log(`✅ ${account.role} login successful`);
        
        // Take screenshot
        await page.screenshot({ 
          path: `playwright-tests/screenshots/login-${account.username}.png`, 
          fullPage: true 
        });
        
        // Navigate back to login for next test (logout button is in hidden dropdown)
        await page.goto('/login');
        await page.waitForTimeout(1000);
      } else {
        console.log(`❌ ${account.role} login failed`);
      }
      
      // Clear fields for next test
      await page.fill('input[name="username"]', '');
      await page.fill('input[name="password"]', '');
    }
  });

  test('should validate login form accessibility', async ({ page }) => {
    console.log('♿ Testing login form accessibility...');
    
    // Check for proper labels
    const usernameLabel = page.locator('label[for="username"]');
    const passwordLabel = page.locator('label[for="password"]');
    
    await expect(usernameLabel).toBeVisible();
    await expect(passwordLabel).toBeVisible();
    
    // Check for required attributes
    const usernameInput = page.locator('input[name="username"]');
    const passwordInput = page.locator('input[name="password"]');
    
    await expect(usernameInput).toHaveAttribute('required');
    await expect(passwordInput).toHaveAttribute('required');
    
    // Check for autofocus
    await expect(usernameInput).toHaveAttribute('autofocus');
    
    console.log('✓ Login form accessibility checks passed');
  });

  test('should test responsive design on login page', async ({ page }) => {
    console.log('📱 Testing responsive design...');
    
    const viewports = [
      { width: 1920, height: 1080, name: 'Desktop' },
      { width: 768, height: 1024, name: 'Tablet' },
      { width: 375, height: 667, name: 'Mobile' }
    ];

    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.waitForTimeout(500);
      
      // Check if login form is still visible and functional
      await expect(page.locator('.login-card')).toBeVisible();
      await expect(page.locator('input[name="username"]')).toBeVisible();
      await expect(page.locator('input[name="password"]')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();
      
      // Take screenshot
      await page.screenshot({ 
        path: `playwright-tests/screenshots/responsive-${viewport.name.toLowerCase()}.png`,
        fullPage: true 
      });
      
      console.log(`✓ ${viewport.name} viewport test passed`);
    }
  });

  test('should test error handling for invalid credentials', async ({ page }) => {
    console.log('❌ Testing invalid credentials handling...');
    
    // Try with invalid credentials
    await page.fill('input[name="username"]', 'invalid_user');
    await page.fill('input[name="password"]', 'wrong_password');
    await page.click('button[type="submit"]');
    
    await page.waitForTimeout(2000);
    
    // Should stay on login page with error parameter
    const currentUrl = page.url();
    expect(currentUrl).toContain('login');
    expect(currentUrl).toContain('error=true');
    
    console.log('✓ Invalid credentials properly handled');
  });

  test('should verify page performance', async ({ page }) => {
    console.log('⚡ Testing page performance...');
    
    const startTime = Date.now();
    await page.goto('/login', { waitUntil: 'networkidle' });
    const loadTime = Date.now() - startTime;
    
    console.log(`Login page load time: ${loadTime}ms`);
    
    // Should load within 3 seconds
    expect(loadTime).toBeLessThan(3000);
    
    // Check for critical resources
    const responses = [];
    page.on('response', response => {
      if (response.status() >= 400) {
        responses.push({
          url: response.url(),
          status: response.status()
        });
      }
    });
    
    await page.reload();
    await page.waitForTimeout(2000);
    
    const criticalErrors = responses.filter(r => r.status >= 500);
    console.log(`Critical errors found: ${criticalErrors.length}`);
    
    if (criticalErrors.length > 0) {
      console.log('Critical errors:', criticalErrors);
    }
  });
});