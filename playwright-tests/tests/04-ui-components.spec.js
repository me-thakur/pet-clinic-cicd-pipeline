const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - UI Components', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should check responsive design', async ({ page }) => {
    // Test different viewport sizes
    const viewports = [
      { width: 1920, height: 1080, name: 'Desktop Large' },
      { width: 1366, height: 768, name: 'Desktop Medium' },
      { width: 768, height: 1024, name: 'Tablet' },
      { width: 375, height: 667, name: 'Mobile' }
    ];

    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      await page.waitForTimeout(500);
      
      console.log(`Testing ${viewport.name} (${viewport.width}x${viewport.height})`);
      
      // Check if page is still functional
      await expect(page.locator('body')).toBeVisible();
      
      // Take screenshot for visual verification
      await page.screenshot({ 
        path: `playwright-tests/screenshots/${viewport.name.toLowerCase().replace(' ', '-')}.png`,
        fullPage: true 
      });
    }
  });

  test('should check navigation elements', async ({ page }) => {
    // Look for common navigation elements
    const navElements = [
      'nav',
      '.navbar',
      '.navigation',
      'header',
      '.menu',
      'a[href]'
    ];

    for (const selector of navElements) {
      const elements = page.locator(selector);
      const count = await elements.count();
      
      if (count > 0) {
        console.log(`✓ Found ${count} ${selector} elements`);
        
        // Check if navigation links are functional
        if (selector === 'a[href]') {
          const links = await elements.all();
          for (let i = 0; i < Math.min(links.length, 5); i++) {
            const href = await links[i].getAttribute('href');
            if (href && !href.startsWith('#') && !href.startsWith('javascript:')) {
              console.log(`  Link found: ${href}`);
            }
          }
        }
      }
    }
  });

  test('should check form elements and validation', async ({ page }) => {
    // Look for forms on the page
    const forms = page.locator('form');
    const formCount = await forms.count();
    
    console.log(`Found ${formCount} forms on the page`);
    
    if (formCount > 0) {
      // Check first form
      const firstForm = forms.first();
      
      // Look for input fields
      const inputs = firstForm.locator('input, select, textarea');
      const inputCount = await inputs.count();
      
      console.log(`Form has ${inputCount} input elements`);
      
      // Check for validation attributes
      const inputsWithValidation = firstForm.locator('input[required], input[pattern], input[minlength], input[maxlength]');
      const validationCount = await inputsWithValidation.count();
      
      if (validationCount > 0) {
        console.log(`✓ Found ${validationCount} inputs with validation attributes`);
      }
    }
  });

  test('should check JavaScript functionality', async ({ page }) => {
    // Check if JavaScript is working by looking for dynamic content
    await page.waitForTimeout(2000);
    
    // Check for common JavaScript indicators
    const jsIndicators = [
      '.js-enabled',
      '[data-toggle]',
      '[onclick]',
      '.dropdown',
      '.modal'
    ];

    for (const selector of jsIndicators) {
      const elements = page.locator(selector);
      const count = await elements.count();
      
      if (count > 0) {
        console.log(`✓ Found ${count} JavaScript-enabled elements: ${selector}`);
      }
    }

    // Check for JavaScript errors in console
    const logs = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        logs.push(msg.text());
      }
    });

    await page.reload();
    await page.waitForTimeout(3000);

    if (logs.length > 0) {
      console.log('JavaScript errors found:');
      logs.forEach(log => console.log(`  - ${log}`));
    } else {
      console.log('✓ No JavaScript errors detected');
    }
  });

  test('should check accessibility basics', async ({ page }) => {
    // Check for basic accessibility features
    const accessibilityChecks = [
      { selector: 'img[alt]', description: 'Images with alt text' },
      { selector: 'label[for]', description: 'Labels with for attributes' },
      { selector: 'input[aria-label], input[aria-labelledby]', description: 'Inputs with ARIA labels' },
      { selector: '[role]', description: 'Elements with ARIA roles' },
      { selector: 'h1, h2, h3, h4, h5, h6', description: 'Heading elements' }
    ];

    for (const check of accessibilityChecks) {
      const elements = page.locator(check.selector);
      const count = await elements.count();
      
      if (count > 0) {
        console.log(`✓ ${check.description}: ${count} found`);
      } else {
        console.log(`ℹ ${check.description}: none found`);
      }
    }
  });

  test('should check page performance', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('/', { waitUntil: 'networkidle' });
    
    const loadTime = Date.now() - startTime;
    console.log(`Page load time: ${loadTime}ms`);
    
    // Check if page loads within reasonable time (5 seconds)
    expect(loadTime).toBeLessThan(5000);
    
    // Check for large resources
    const responses = [];
    page.on('response', response => {
      responses.push({
        url: response.url(),
        status: response.status(),
        size: response.headers()['content-length']
      });
    });

    await page.reload();
    await page.waitForTimeout(2000);

    console.log(`Total HTTP requests: ${responses.length}`);
    
    const largeResources = responses.filter(r => r.size && parseInt(r.size) > 1000000);
    if (largeResources.length > 0) {
      console.log('Large resources (>1MB):');
      largeResources.forEach(r => console.log(`  - ${r.url}: ${r.size} bytes`));
    }
  });
});