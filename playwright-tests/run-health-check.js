const { chromium } = require('playwright');

async function runHealthCheck() {
  console.log('🏥 Pet Clinic Health Check Starting...\n');
  
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  const results = {
    timestamp: new Date().toISOString(),
    tests: [],
    summary: { passed: 0, failed: 0 }
  };

  // Test 1: Frontend Accessibility
  try {
    console.log('🌐 Testing Frontend Accessibility...');
    await page.goto('http://localhost:8081');
    await page.waitForTimeout(2000);
    
    const title = await page.title();
    const hasLoginForm = await page.locator('form').count() > 0;
    const hasInputs = await page.locator('input').count() >= 2;
    
    if (title.includes('Pet Clinic') && hasLoginForm && hasInputs) {
      console.log('✅ Frontend: HEALTHY');
      results.tests.push({ test: 'Frontend Access', status: 'PASS', details: title });
      results.summary.passed++;
    } else {
      console.log('❌ Frontend: ISSUES DETECTED');
      results.tests.push({ test: 'Frontend Access', status: 'FAIL', details: 'Missing elements' });
      results.summary.failed++;
    }
  } catch (error) {
    console.log('❌ Frontend: ERROR -', error.message);
    results.tests.push({ test: 'Frontend Access', status: 'FAIL', details: error.message });
    results.summary.failed++;
  }

  // Test 2: Backend Health
  try {
    console.log('🔧 Testing Backend Health...');
    const response = await page.request.get('http://localhost:9090/actuator/health');
    
    if (response.status() === 200) {
      const healthData = await response.json();
      if (healthData.status === 'UP') {
        console.log('✅ Backend: HEALTHY');
        results.tests.push({ test: 'Backend Health', status: 'PASS', details: 'Service UP' });
        results.summary.passed++;
      } else {
        console.log('⚠️ Backend: DEGRADED');
        results.tests.push({ test: 'Backend Health', status: 'WARN', details: healthData.status });
        results.summary.failed++;
      }
    } else {
      console.log('❌ Backend: UNHEALTHY');
      results.tests.push({ test: 'Backend Health', status: 'FAIL', details: `HTTP ${response.status()}` });
      results.summary.failed++;
    }
  } catch (error) {
    console.log('❌ Backend: ERROR -', error.message);
    results.tests.push({ test: 'Backend Health', status: 'FAIL', details: error.message });
    results.summary.failed++;
  }

  // Test 3: API Security
  try {
    console.log('🔒 Testing API Security...');
    const response = await page.request.get('http://localhost:9090/api/owners');
    
    if (response.status() === 403 || response.status() === 401) {
      console.log('✅ Security: PROPERLY PROTECTED');
      results.tests.push({ test: 'API Security', status: 'PASS', details: 'Endpoints protected' });
      results.summary.passed++;
    } else if (response.status() === 200) {
      console.log('⚠️ Security: ENDPOINTS OPEN');
      results.tests.push({ test: 'API Security', status: 'WARN', details: 'No authentication required' });
      results.summary.failed++;
    } else {
      console.log('❓ Security: UNEXPECTED RESPONSE');
      results.tests.push({ test: 'API Security', status: 'UNKNOWN', details: `HTTP ${response.status()}` });
      results.summary.failed++;
    }
  } catch (error) {
    console.log('❌ Security: ERROR -', error.message);
    results.tests.push({ test: 'API Security', status: 'FAIL', details: error.message });
    results.summary.failed++;
  }

  // Test 4: Validation Service
  try {
    console.log('✅ Testing Validation Service...');
    const response = await page.request.get('http://localhost:8081/api/validation/owners/health');
    
    if (response.status() === 200) {
      const validationData = await response.json();
      if (validationData.available === true) {
        console.log('✅ Validation: OPERATIONAL');
        results.tests.push({ test: 'Validation Service', status: 'PASS', details: 'Service available' });
        results.summary.passed++;
      } else {
        console.log('⚠️ Validation: UNAVAILABLE');
        results.tests.push({ test: 'Validation Service', status: 'WARN', details: 'Service unavailable' });
        results.summary.failed++;
      }
    } else {
      console.log('❌ Validation: ERROR');
      results.tests.push({ test: 'Validation Service', status: 'FAIL', details: `HTTP ${response.status()}` });
      results.summary.failed++;
    }
  } catch (error) {
    console.log('❌ Validation: ERROR -', error.message);
    results.tests.push({ test: 'Validation Service', status: 'FAIL', details: error.message });
    results.summary.failed++;
  }

  await browser.close();

  // Generate Summary
  const total = results.summary.passed + results.summary.failed;
  const successRate = Math.round((results.summary.passed / total) * 100);
  
  console.log('\n📊 HEALTH CHECK SUMMARY');
  console.log('========================');
  console.log(`Total Tests: ${total}`);
  console.log(`Passed: ${results.summary.passed}`);
  console.log(`Failed: ${results.summary.failed}`);
  console.log(`Success Rate: ${successRate}%`);
  
  if (successRate >= 75) {
    console.log('\n🎉 APPLICATION STATUS: HEALTHY');
  } else if (successRate >= 50) {
    console.log('\n⚠️ APPLICATION STATUS: DEGRADED');
  } else {
    console.log('\n🚨 APPLICATION STATUS: CRITICAL');
  }

  console.log('\nDetailed Results:');
  results.tests.forEach(test => {
    const icon = test.status === 'PASS' ? '✅' : test.status === 'WARN' ? '⚠️' : '❌';
    console.log(`${icon} ${test.test}: ${test.details}`);
  });

  return results;
}

// Run the health check
runHealthCheck().catch(console.error);