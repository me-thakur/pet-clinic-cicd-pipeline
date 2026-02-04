package com.petclinic.backend.service;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for visit search functionality
 * Provides methods for searching visits by various criteria with pagination support
 * 
 * Validates: Requirements 1.4, 1.5
 */
public interface VisitSearchService {

    /**
     * Search visits by treatment text with case-insensitive partial matching
     * 
     * @param text Search text to match against treatment field
     * @param pageable Pagination parameters
     * @return PagedResponse containing matching visits
     */
    PagedResponse<Visit> searchByTreatment(String text, Pageable pageable);

    /**
     * Search visits by diagnosis text with case-insensitive partial matching
     * 
     * @param text Search text to match against diagnosis field
     * @param pageable Pagination parameters
     * @return PagedResponse containing matching visits
     */
    PagedResponse<Visit> searchByDiagnosis(String text, Pageable pageable);

    /**
     * Search visits by description/notes text with case-insensitive partial matching
     * 
     * @param text Search text to match against description and notes fields
     * @param pageable Pagination parameters
     * @return PagedResponse containing matching visits
     */
    PagedResponse<Visit> searchByDescription(String text, Pageable pageable);

    /**
     * Search visits by notes text with case-insensitive partial matching
     * 
     * @param text Search text to match against notes field
     * @param pageable Pagination parameters
     * @return PagedResponse containing matching visits
     */
    PagedResponse<Visit> searchByNotes(String text, Pageable pageable);

    /**
     * Get total count of visits matching treatment search criteria
     * 
     * @param text Search text to match against treatment field
     * @return Total count of matching visits
     */
    long countByTreatment(String text);

    /**
     * Get total count of visits matching diagnosis search criteria
     * 
     * @param text Search text to match against diagnosis field
     * @return Total count of matching visits
     */
    long countByDiagnosis(String text);

    /**
     * Get total count of visits matching description search criteria
     * 
     * @param text Search text to match against description and notes fields
     * @return Total count of matching visits
     */
    long countByDescription(String text);

    /**
     * Get total count of visits matching notes search criteria
     * 
     * @param text Search text to match against notes field
     * @return Total count of matching visits
     */
    long countByNotes(String text);
}