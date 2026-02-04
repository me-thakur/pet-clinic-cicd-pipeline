const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - Comprehensive User Flow', () => {
  test('should perform complete application flow test', async ({ page }) => {
    console.log('🚀 Starting comprehensive Pet Clinic application test...');
    
    // Step 1: Navigate to application
    console.log('📍 Step 1: Navigating to application...');
    await page.goto('/');
    await page.waitForTimeout(2000);
    
    // Take initial screenshot
    await page.screenshot({ path: 'playwright-tests/screenshots/01-initial-load.png', fullPage: true });
    
    // Step 2: Check if redirected to login
    console.log('📍 Step 2: Checking login redirect...');
    const currentUrl = page.url();
    console.log(`Current URL: ${currentUrl}`);
    
    if (currentUrl.includes('/login')) {
      console.log('✓ Successfully redirected to login page');
      
      // Step 3: Analyze login form
      console.log('📍 Step 3: Analyzing login form...');
      await page.screenshot({ path: 'playwright-tests/screenshots/02-login-form.png', fullPage: true });
      
      const usernameField = page.locator('input[name="username"], input[type="text"], input[placeholder*="username" i]');
      const passwordField = page.locator('input[name="password"], input[type="password"]');
      const submitButton = page.locator('button[type="submit"], input[type="submit"], button:has-text("Login")');
      
      const hasUsername = await usernameField.count() > 0;
      const hasPassword = await passwordField.count() > 0;
      const hasSubmit = await submitButton.count() > 0;
      
      console.log(`Username field: ${hasUsername ? '✓' : '✗'}`);
      console.log(`Password field: ${hasPassword ? '✓' : '✗'}`);
      console.log(`Submit button: ${hasSubmit ? '✓' : '✗'}`);
      
      if (hasUsername && hasPassword && hasSubmit) {
        // Step 4: Attempt login with various credentials
        console.log('📍 Step 4: Testing authentication...');
        
        const testCredentials = [
          { username: 'admin', password: 'admin123', description: 'Admin user' },
          { username: 'vet1', password: 'vet123', description: 'Veterinarian 1' },
          { username: 'staff1', password: 'staff123', description: 'Staff member 1' },
          { username: 'vet2', password: 'vet123', description: 'Veterinarian 2' },
          { username: 'staff2', password: 'staff123', description: 'Staff member 2' }
        ];
        
        let successfulLogin = null;
        
        for (const cred of testCredentials) {
          console.log(`🔐 Testing ${cred.description}: ${cred.username}/${cred.password}`);
          
          await usernameField.first().fill(cred.username);
          await passwordField.first().fill(cred.password);
          await submitButton.first().click();
          
          await page.waitForTimeout(3000);
          
          const newUrl = page.url();
          if (!newUrl.includes('/login') && !newUrl.includes('/error')) {
            console.log(`✅ Login successful with ${cred.description}!`);
            successfulLogin = cred;
            await page.screenshot({ path: `playwright-tests/screenshots/03-successful-login-${cred.username}.png`, fullPage: true });
            break;
          } else {
            console.log(`❌ Login failed with ${cred.description}`);
            // Clear fields and try next
            await usernameField.first().fill('');
            await passwordField.first().fill('');
          }
        }
        
        if (successfulLogin) {
          // Step 5: Explore authenticated application
          console.log('📍 Step 5: Exploring authenticated application...');
          
          // Look for navigation elements
          const navLinks = page.locator('nav a, .navbar a, .menu a, header a');
          const navCount = await navLinks.count();
          console.log(`Found ${navCount} navigation links`);
          
          if (navCount > 0) {
            // Test navigation
            const links = await navLinks.all();
            for (let i = 0; i < Math.min(links.length, 5); i++) {
              const linkText = await links[i].textContent();
              const href = await links[i].getAttribute('href');
              
              if (href && !href.startsWith('#') && !href.includes('logout')) {
                console.log(`🔗 Testing navigation to: ${linkText} (${href})`);
                
                try {
                  await links[i].click();
                  await page.waitForTimeout(2000);
                  
                  const pageTitle = await page.title();
                  console.log(`  ✓ Navigated to: ${pageTitle}`);
                  
                  await page.screenshot({ 
                    path: `playwright-tests/screenshots/04-navigation-${i + 1}-${linkText?.replace(/[^a-zA-Z0-9]/g, '-') || 'unknown'}.png`, 
                    fullPage: true 
                  });
                  
                  // Go back to continue testing
                  await page.goBack();
                  await page.waitForTimeout(1000);
                } catch (error) {
                  console.log(`  ❌ Navigation failed: ${error.message}`);
                }
              }
            }
          }
          
          // Step 6: Test forms and functionality
          console.log('📍 Step 6: Testing forms and functionality...');
          
          // Look for forms
          const forms = page.locator('form');
          const formCount = await forms.count();
          console.log(`Found ${formCount} forms`);
          
          if (formCount > 0) {
            // Test first form
            const firstForm = forms.first();
            const inputs = firstForm.locator('input, select, textarea');
            const inputCount = await inputs.count();
            
            console.log(`First form has ${inputCount} input fields`);
            
            if (inputCount > 0) {
              await page.screenshot({ path: 'playwright-tests/screenshots/05-form-testing.png', fullPage: true });
            }
          }
          
          // Step 7: Test logout
          console.log('📍 Step 7: Testing logout...');
          const logoutLink = page.locator('a[href*="logout"], button:has-text("logout" i), .logout');
          
          if (await logoutLink.count() > 0) {
            await logoutLink.first().click();
            await page.waitForTimeout(2000);
            
            const finalUrl = page.url();
            if (finalUrl.includes('/login')) {
              console.log('✅ Logout successful - redirected to login');
            } else {
              console.log('ℹ Logout completed - current URL:', finalUrl);
            }
            
            await page.screenshot({ path: 'playwright-tests/screenshots/06-after-logout.png', fullPage: true });
          } else {
            console.log('ℹ No logout button found');
          }
          
        } else {
          console.log('❌ No successful login found with test credentials');
        }
      } else {
        console.log('❌ Login form elements not found or incomplete');
      }
    } else {
      console.log('ℹ Not redirected to login - checking current page content');
      await page.screenshot({ path: 'playwright-tests/screenshots/02-no-login-redirect.png', fullPage: true });
    }
    
    console.log('🏁 Comprehensive test completed!');
  });

  test('should test error handling', async ({ page }) => {
    console.log('🚀 Testing error handling...');
    
    // Test 404 page
    await page.goto('/nonexistent-page');
    await page.waitForTimeout(2000);
    
    const pageContent = await page.textContent('body');
    console.log('404 page response length:', pageContent.length);
    
    await page.screenshot({ path: 'playwright-tests/screenshots/07-404-page.png', fullPage: true });
    
    // Test invalid API endpoints
    const response = await page.request.get('http://localhost:9090/api/nonexistent');
    console.log(`Invalid API endpoint status: ${response.status()}`);
  });

  test('should generate comprehensive report', async ({ page }) => {
    console.log('📊 Generating comprehensive application report...');
    
    const report = {
      timestamp: new Date().toISOString(),
      application: 'Pet Clinic',
      frontend_url: 'http://localhost:8081',
      backend_url: 'http://localhost:9090',
      tests_performed: [],
      summary: {}
    };
    
    // Test frontend accessibility
    await page.goto('/');
    await page.waitForTimeout(2000);
    
    const title = await page.title();
    const hasTitle = title && title.length > 0;
    
    report.tests_performed.push({
      test: 'Page Title',
      status: hasTitle ? 'PASS' : 'FAIL',
      details: title
    });
    
    // Test backend health
    try {
      const healthResponse = await page.request.get('http://localhost:9090/actuator/health');
      const healthStatus = healthResponse.status() === 200 ? 'PASS' : 'FAIL';
      
      report.tests_performed.push({
        test: 'Backend Health',
        status: healthStatus,
        details: `HTTP ${healthResponse.status()}`
      });
    } catch (error) {
      report.tests_performed.push({
        test: 'Backend Health',
        status: 'FAIL',
        details: error.message
      });
    }
    
    // Test validation service
    try {
      const validationResponse = await page.request.get('http://localhost:8081/api/validation/owners/health');
      const validationStatus = validationResponse.status() === 200 ? 'PASS' : 'FAIL';
      
      report.tests_performed.push({
        test: 'Validation Service',
        status: validationStatus,
        details: `HTTP ${validationResponse.status()}`
      });
    } catch (error) {
      report.tests_performed.push({
        test: 'Validation Service',
        status: 'FAIL',
        details: error.message
      });
    }
    
    // Generate summary
    const passCount = report.tests_performed.filter(t => t.status === 'PASS').length;
    const failCount = report.tests_performed.filter(t => t.status === 'FAIL').length;
    
    report.summary = {
      total_tests: report.tests_performed.length,
      passed: passCount,
      failed: failCount,
      success_rate: `${Math.round((passCount / report.tests_performed.length) * 100)}%`
    };
    
    // Save report
    await page.evaluate((reportData) => {
      console.log('=== PET CLINIC TEST REPORT ===');
      console.log(JSON.stringify(reportData, null, 2));
    }, report);
    
    console.log('📊 Report generated successfully!');
  });
});