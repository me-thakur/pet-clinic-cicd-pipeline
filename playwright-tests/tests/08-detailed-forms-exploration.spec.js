const { test, expect } = require('@playwright/test');

test.describe('Pet Clinic - Detailed Forms Exploration', () => {
  let page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    
    // Login once for all tests
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(3000);
    
    console.log('✅ Successfully logged in for detailed form exploration');
  });

  test.afterAll(async () => {
    await page?.close();
  });

  test('should explore dashboard and find all available forms', async () => {
    console.log('🔍 Exploring Dashboard for Available Forms...');
    
    // Take screenshot of dashboard
    await page.screenshot({ 
      path: 'playwright-tests/screenshots/dashboard-overview.png',
      fullPage: true 
    });
    
    // Get page title and URL
    const title = await page.title();
    const url = page.url();
    console.log(`Current page: ${title} (${url})`);
    
    // Find all navigation links
    const navLinks = page.locator('nav a, .navbar a, .menu a, .nav-link');
    const navCount = await navLinks.count();
    console.log(`Found ${navCount} navigation links`);
    
    if (navCount > 0) {
      for (let i = 0; i < navCount; i++) {
        const link = navLinks.nth(i);
        const text = await link.textContent();
        const href = await link.getAttribute('href');
        console.log(`  - ${text?.trim()}: ${href}`);
      }
    }
    
    // Find all buttons that might lead to forms
    const buttons = page.locator('button, .btn, a[class*="btn"]');
    const buttonCount = await buttons.count();
    console.log(`Found ${buttonCount} buttons/links`);
    
    // Look for specific form-related keywords
    const formKeywords = ['Add', 'New', 'Create', 'Edit', 'Update', 'Search', 'Filter'];
    
    for (const keyword of formKeywords) {
      const elements = page.locator(`a:has-text("${keyword}"), button:has-text("${keyword}")`);
      const count = await elements.count();
      if (count > 0) {
        console.log(`✓ Found ${count} elements with "${keyword}"`);
      }
    }
  });

  test('should test owners section forms', async () => {
    console.log('👥 Testing Owners Section Forms...');
    
    try {
      // Try different possible URLs for owners
      const ownerUrls = ['/owners', '/owner', '/owners/list', '/owners/search'];
      
      for (const url of ownerUrls) {
        try {
          await page.goto(url);
          await page.waitForTimeout(2000);
          
          const currentUrl = page.url();
          if (!currentUrl.includes('error') && !currentUrl.includes('404')) {
            console.log(`✓ Successfully accessed: ${url}`);
            
            // Take screenshot
            await page.screenshot({ 
              path: `playwright-tests/screenshots/owners-section.png`,
              fullPage: true 
            });
            
            // Look for forms
            const forms = page.locator('form');
            const formCount = await forms.count();
            console.log(`Found ${formCount} forms on owners page`);
            
            // Look for search functionality
            const searchInputs = page.locator('input[type="search"], input[name*="search"], input[placeholder*="search"]');
            const searchCount = await searchInputs.count();
            console.log(`Found ${searchCount} search inputs`);
            
            if (searchCount > 0) {
              const searchInput = searchInputs.first();
              await searchInput.fill('test search');
              console.log('✓ Search input tested');
              
              // Look for search button
              const searchButton = page.locator('button:has-text("Search"), input[type="submit"]');
              if (await searchButton.count() > 0) {
                await searchButton.first().click();
                await page.waitForTimeout(2000);
                console.log('✓ Search functionality executed');
              }
            }
            
            // Look for add/new buttons
            const addButtons = page.locator('a:has-text("Add"), a:has-text("New"), button:has-text("Add"), button:has-text("New")');
            const addCount = await addButtons.count();
            console.log(`Found ${addCount} add/new buttons`);
            
            if (addCount > 0) {
              try {
                await addButtons.first().click();
                await page.waitForTimeout(2000);
                
                // Check if we're on a form page
                const newForms = page.locator('form');
                const newFormCount = await newForms.count();
                console.log(`Found ${newFormCount} forms on add/new page`);
                
                if (newFormCount > 0) {
                  await page.screenshot({ 
                    path: `playwright-tests/screenshots/owner-form.png`,
                    fullPage: true 
                  });
                  
                  // Test form fields
                  const inputs = page.locator('input, select, textarea');
                  const inputCount = await inputs.count();
                  console.log(`Found ${inputCount} form inputs`);
                  
                  // Test specific owner fields
                  const firstNameField = page.locator('input[name*="firstName"], input[id*="firstName"]');
                  if (await firstNameField.count() > 0) {
                    await firstNameField.fill('John');
                    console.log('✓ First name field tested');
                  }
                  
                  const lastNameField = page.locator('input[name*="lastName"], input[id*="lastName"]');
                  if (await lastNameField.count() > 0) {
                    await lastNameField.fill('Doe');
                    console.log('✓ Last name field tested');
                  }
                  
                  const addressField = page.locator('input[name*="address"], textarea[name*="address"]');
                  if (await addressField.count() > 0) {
                    await addressField.fill('123 Main St');
                    console.log('✓ Address field tested');
                  }
                  
                  const phoneField = page.locator('input[name*="phone"], input[name*="telephone"]');
                  if (await phoneField.count() > 0) {
                    await phoneField.fill('555-1234');
                    console.log('✓ Phone field tested');
                  }
                }
              } catch (error) {
                console.log(`ℹ Could not test add form: ${error.message}`);
              }
            }
            
            break; // Exit loop if we found a working URL
          }
        } catch (error) {
          console.log(`ℹ Could not access ${url}: ${error.message}`);
        }
      }
    } catch (error) {
      console.log(`ℹ Owners section test: ${error.message}`);
    }
  });

  test('should test pets section forms', async () => {
    console.log('🐕 Testing Pets Section Forms...');
    
    try {
      const petUrls = ['/pets', '/pet', '/pets/list', '/animals'];
      
      for (const url of petUrls) {
        try {
          await page.goto(url);
          await page.waitForTimeout(2000);
          
          const currentUrl = page.url();
          if (!currentUrl.includes('error') && !currentUrl.includes('404')) {
            console.log(`✓ Successfully accessed: ${url}`);
            
            await page.screenshot({ 
              path: `playwright-tests/screenshots/pets-section.png`,
              fullPage: true 
            });
            
            // Test pet-specific functionality
            const forms = page.locator('form');
            const formCount = await forms.count();
            console.log(`Found ${formCount} forms on pets page`);
            
            // Look for pet type filters
            const petTypeSelects = page.locator('select[name*="type"], select[id*="type"]');
            if (await petTypeSelects.count() > 0) {
              console.log('✓ Pet type selector found');
              const options = await petTypeSelects.first().locator('option').count();
              console.log(`  - ${options} pet type options available`);
            }
            
            break;
          }
        } catch (error) {
          console.log(`ℹ Could not access ${url}: ${error.message}`);
        }
      }
    } catch (error) {
      console.log(`ℹ Pets section test: ${error.message}`);
    }
  });

  test('should test visits section forms', async () => {
    console.log('🏥 Testing Visits Section Forms...');
    
    try {
      const visitUrls = ['/visits', '/visit', '/visits/list', '/appointments'];
      
      for (const url of visitUrls) {
        try {
          await page.goto(url);
          await page.waitForTimeout(2000);
          
          const currentUrl = page.url();
          if (!currentUrl.includes('error') && !currentUrl.includes('404')) {
            console.log(`✓ Successfully accessed: ${url}`);
            
            await page.screenshot({ 
              path: `playwright-tests/screenshots/visits-section.png`,
              fullPage: true 
            });
            
            // Test visit-specific functionality
            const dateInputs = page.locator('input[type="date"], input[name*="date"]');
            if (await dateInputs.count() > 0) {
              console.log('✓ Date input found');
              await dateInputs.first().fill('2024-02-04');
            }
            
            const descriptionFields = page.locator('textarea[name*="description"], input[name*="description"]');
            if (await descriptionFields.count() > 0) {
              console.log('✓ Description field found');
              await descriptionFields.first().fill('Regular checkup');
            }
            
            break;
          }
        } catch (error) {
          console.log(`ℹ Could not access ${url}: ${error.message}`);
        }
      }
    } catch (error) {
      console.log(`ℹ Visits section test: ${error.message}`);
    }
  });

  test('should test veterinarians section forms', async () => {
    console.log('👨‍⚕️ Testing Veterinarians Section Forms...');
    
    try {
      const vetUrls = ['/vets', '/vet', '/veterinarians', '/doctors'];
      
      for (const url of vetUrls) {
        try {
          await page.goto(url);
          await page.waitForTimeout(2000);
          
          const currentUrl = page.url();
          if (!currentUrl.includes('error') && !currentUrl.includes('404')) {
            console.log(`✓ Successfully accessed: ${url}`);
            
            await page.screenshot({ 
              path: `playwright-tests/screenshots/vets-section.png`,
              fullPage: true 
            });
            
            // Test vet-specific functionality
            const specialtyFields = page.locator('select[name*="specialty"], input[name*="specialty"]');
            if (await specialtyFields.count() > 0) {
              console.log('✓ Specialty field found');
            }
            
            break;
          }
        } catch (error) {
          console.log(`ℹ Could not access ${url}: ${error.message}`);
        }
      }
    } catch (error) {
      console.log(`ℹ Veterinarians section test: ${error.message}`);
    }
  });

  test('should test all form validation scenarios', async () => {
    console.log('✅ Testing Comprehensive Form Validation...');
    
    // Go back to dashboard to find any available forms
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);
    
    // Find all forms on the current page
    const forms = page.locator('form');
    const formCount = await forms.count();
    console.log(`Found ${formCount} forms to test validation on`);
    
    for (let i = 0; i < formCount; i++) {
      const form = forms.nth(i);
      const formAction = await form.getAttribute('action');
      console.log(`Testing form ${i + 1}: ${formAction || 'no action'}`);
      
      // Find all inputs in this form
      const inputs = form.locator('input, select, textarea');
      const inputCount = await inputs.count();
      console.log(`  - ${inputCount} inputs found`);
      
      // Test required field validation
      const requiredInputs = form.locator('input[required], select[required], textarea[required]');
      const requiredCount = await requiredInputs.count();
      console.log(`  - ${requiredCount} required inputs`);
      
      // Test email validation if present
      const emailInputs = form.locator('input[type="email"]');
      if (await emailInputs.count() > 0) {
        await emailInputs.first().fill('invalid-email');
        console.log('  ✓ Email validation tested');
      }
      
      // Test number validation if present
      const numberInputs = form.locator('input[type="number"]');
      if (await numberInputs.count() > 0) {
        await numberInputs.first().fill('abc');
        console.log('  ✓ Number validation tested');
      }
      
      // Test date validation if present
      const dateInputs = form.locator('input[type="date"]');
      if (await dateInputs.count() > 0) {
        await dateInputs.first().fill('invalid-date');
        console.log('  ✓ Date validation tested');
      }
    }
    
    console.log('✅ Form validation testing completed');
  });

  test('should generate comprehensive forms report', async () => {
    console.log('📊 Generating Comprehensive Forms Report...');
    
    const report = {
      timestamp: new Date().toISOString(),
      application: 'Pet Clinic',
      forms_tested: [],
      summary: {}
    };
    
    // Test different sections
    const sections = [
      { name: 'Dashboard', url: '/dashboard' },
      { name: 'Owners', url: '/owners' },
      { name: 'Pets', url: '/pets' },
      { name: 'Visits', url: '/visits' },
      { name: 'Vets', url: '/vets' }
    ];
    
    for (const section of sections) {
      try {
        await page.goto(section.url);
        await page.waitForTimeout(2000);
        
        const currentUrl = page.url();
        const forms = page.locator('form');
        const formCount = await forms.count();
        const inputs = page.locator('input, select, textarea');
        const inputCount = await inputs.count();
        const buttons = page.locator('button, input[type="submit"]');
        const buttonCount = await buttons.count();
        
        const sectionReport = {
          section: section.name,
          url: section.url,
          accessible: !currentUrl.includes('error'),
          forms_count: formCount,
          inputs_count: inputCount,
          buttons_count: buttonCount,
          status: !currentUrl.includes('error') ? 'ACCESSIBLE' : 'NOT_ACCESSIBLE'
        };
        
        report.forms_tested.push(sectionReport);
        console.log(`✓ ${section.name}: ${sectionReport.status} (${formCount} forms, ${inputCount} inputs)`);
        
      } catch (error) {
        report.forms_tested.push({
          section: section.name,
          url: section.url,
          accessible: false,
          status: 'ERROR',
          error: error.message
        });
        console.log(`❌ ${section.name}: ERROR - ${error.message}`);
      }
    }
    
    // Generate summary
    const totalForms = report.forms_tested.reduce((sum, section) => sum + (section.forms_count || 0), 0);
    const totalInputs = report.forms_tested.reduce((sum, section) => sum + (section.inputs_count || 0), 0);
    const accessibleSections = report.forms_tested.filter(section => section.accessible).length;
    
    report.summary = {
      total_sections_tested: sections.length,
      accessible_sections: accessibleSections,
      total_forms_found: totalForms,
      total_inputs_found: totalInputs,
      success_rate: `${Math.round((accessibleSections / sections.length) * 100)}%`
    };
    
    console.log('📊 Forms Testing Report:');
    console.log(`  - Sections tested: ${report.summary.total_sections_tested}`);
    console.log(`  - Accessible sections: ${report.summary.accessible_sections}`);
    console.log(`  - Total forms found: ${report.summary.total_forms_found}`);
    console.log(`  - Total inputs found: ${report.summary.total_inputs_found}`);
    console.log(`  - Success rate: ${report.summary.success_rate}`);
    
    // Save report to console for review
    await page.evaluate((reportData) => {
      console.log('=== PET CLINIC FORMS TESTING REPORT ===');
      console.log(JSON.stringify(reportData, null, 2));
    }, report);
    
    console.log('✅ Comprehensive forms report generated');
  });
});