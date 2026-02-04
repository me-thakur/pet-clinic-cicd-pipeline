package com.petclinic.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to verify SearchResultConsistencyService can be injected
 */
@SpringBootTest
@ActiveProfiles("test")
public class SearchResultConsistencyServiceTest {

    @Autowired
    private SearchResultConsistencyService searchResultConsistencyService;

    @Autowired
    private VisitSearchService visitSearchService;

    @Test
    public void testServiceInjection() {
        assertThat(searchResultConsistencyService).isNotNull();
        assertThat(visitSearchService).isNotNull();
    }
}