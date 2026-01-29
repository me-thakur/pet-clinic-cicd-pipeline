package com.petclinic.backend.properties;

import com.petclinic.backend.config.ApiVersionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for API versioning consistency
 * **Validates: Requirements 7.5**
 */
@SpringBootTest
@ActiveProfiles("test")
public class ApiVersioningConsistencyProperties extends PropertyTestBase {
    
    /**
     * Property 18: API Versioning Consistency
     * For any versioned API endpoint, requests should be routed to the correct version and maintain backward compatibility
     */
    @Test
    void apiVersionShouldBeExtractedCorrectlyFromHeaders() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String version = "v" + (1 + random.nextInt(5)); // v1, v2, v3, v4, v5
            
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(ApiVersionConfig.API_VERSION_HEADER, version);
            
            String extractedVersion = ApiVersionConfig.getApiVersion(request);
            
            assertThat(extractedVersion).isEqualTo(version);
        });
    }
    
    /**
     * Property: Default API version should be returned when no header is present
     */
    @Test
    void defaultApiVersionShouldBeReturnedWhenNoHeaderIsPresent() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            MockHttpServletRequest request = new MockHttpServletRequest();
            // No version header added
            
            String extractedVersion = ApiVersionConfig.getApiVersion(request);
            
            assertThat(extractedVersion).isEqualTo(ApiVersionConfig.CURRENT_API_VERSION);
        });
    }
    
    /**
     * Property: Current API version should always be supported
     */
    @Test
    void currentApiVersionShouldAlwaysBeSupported() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            boolean isSupported = ApiVersionConfig.isSupportedVersion(ApiVersionConfig.CURRENT_API_VERSION);
            
            assertThat(isSupported).isTrue();
        });
    }
    
    /**
     * Property: Null version should be treated as current version and be supported
     */
    @Test
    void nullVersionShouldBeTreatedAsCurrentVersionAndBeSupported() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            boolean isSupported = ApiVersionConfig.isSupportedVersion(null);
            
            assertThat(isSupported).isTrue();
        });
    }
    
    /**
     * Property: Versioned paths should be constructed correctly
     */
    @Test
    void versionedPathsShouldBeConstructedCorrectly() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String basePath = "/api/pets/" + random.nextInt(1000);
            
            // Ensure basePath starts with /
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
            
            // Test with current version (should return base path)
            String currentVersionPath = ApiVersionConfig.getVersionedPath(basePath, ApiVersionConfig.CURRENT_API_VERSION);
            assertThat(currentVersionPath).isEqualTo(basePath);
            
            // Test with null version (should return base path)
            String nullVersionPath = ApiVersionConfig.getVersionedPath(basePath, null);
            assertThat(nullVersionPath).isEqualTo(basePath);
            
            // Test with different version
            String differentVersion = "v2";
            String versionedPath = ApiVersionConfig.getVersionedPath(basePath, differentVersion);
            assertThat(versionedPath).isEqualTo("/" + differentVersion + basePath);
        });
    }
    
    /**
     * Property: API version header name should be consistent
     */
    @Test
    void apiVersionHeaderNameShouldBeConsistent() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            assertThat(ApiVersionConfig.API_VERSION_HEADER).isEqualTo("X-API-Version");
            assertThat(ApiVersionConfig.API_VERSION_HEADER).isNotBlank();
        });
    }
    
    /**
     * Property: Current API version should be a valid version string
     */
    @Test
    void currentApiVersionShouldBeValidVersionString() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String currentVersion = ApiVersionConfig.CURRENT_API_VERSION;
            
            assertThat(currentVersion).isNotBlank();
            assertThat(currentVersion).matches("v\\d+"); // Should match pattern like v1, v2, etc.
        });
    }
    
    /**
     * Property: Default media type should be application/json
     */
    @Test
    void defaultMediaTypeShouldBeApplicationJson() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            assertThat(ApiVersionConfig.DEFAULT_MEDIA_TYPE).isEqualTo("application/json");
        });
    }
    
    /**
     * Property: Version support check should be case-sensitive
     */
    @Test
    void versionSupportCheckShouldBeCaseSensitive() {
        runPropertyTest(DEFAULT_ITERATIONS, () -> {
            String currentVersion = ApiVersionConfig.CURRENT_API_VERSION;
            String upperCaseVersion = currentVersion.toUpperCase();
            String lowerCaseVersion = currentVersion.toLowerCase();
            
            // Only exact match should be supported (assuming current version is lowercase)
            if (!currentVersion.equals(upperCaseVersion)) {
                assertThat(ApiVersionConfig.isSupportedVersion(upperCaseVersion)).isFalse();
            }
            
            if (!currentVersion.equals(lowerCaseVersion)) {
                assertThat(ApiVersionConfig.isSupportedVersion(lowerCaseVersion)).isFalse();
            }
            
            // Exact match should always be supported
            assertThat(ApiVersionConfig.isSupportedVersion(currentVersion)).isTrue();
        });
    }
}