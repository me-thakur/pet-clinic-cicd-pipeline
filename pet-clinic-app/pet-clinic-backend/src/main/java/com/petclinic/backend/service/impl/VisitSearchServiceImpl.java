package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PagedResponse;
import com.petclinic.backend.model.Visit;
import com.petclinic.backend.service.VisitSearchService;
import com.petclinic.backend.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of VisitSearchService
 * Provides case-insensitive search functionality for visits with pagination support
 * 
 * Validates: Requirements 1.4, 1.5
 */
@Service
public class VisitSearchServiceImpl implements VisitSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VisitSearchServiceImpl.class);

    @Autowired
    private VisitService visitService;

    @Override
    public PagedResponse<Visit> searchByTreatment(String text, Pageable pageable) {
        logger.debug("Searching visits by treatment: '{}' with pagination: {}", text, pageable);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty search text provided for treatment search");
                return createEmptyPagedResponse(pageable);
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            // Filter visits by treatment
            List<Visit> filteredVisits = allVisits.stream()
                    .filter(visit -> visit.getTreatment() != null && 
                            visit.getTreatment().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            
            // Apply pagination
            Page<Visit> page = createPageFromList(filteredVisits, pageable);
            
            logger.debug("Found {} visits matching treatment '{}', returning page {} with {} results", 
                        filteredVisits.size(), text, pageable.getPageNumber(), page.getContent().size());
            
            return new PagedResponse<>(page);
            
        } catch (Exception e) {
            logger.error("Error searching visits by treatment '{}': {}", text, e.getMessage(), e);
            return createEmptyPagedResponse(pageable);
        }
    }

    @Override
    public PagedResponse<Visit> searchByDiagnosis(String text, Pageable pageable) {
        logger.debug("Searching visits by diagnosis: '{}' with pagination: {}", text, pageable);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty search text provided for diagnosis search");
                return createEmptyPagedResponse(pageable);
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            // Filter visits by diagnosis
            List<Visit> filteredVisits = allVisits.stream()
                    .filter(visit -> visit.getDiagnosis() != null && 
                            visit.getDiagnosis().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            
            // Apply pagination
            Page<Visit> page = createPageFromList(filteredVisits, pageable);
            
            logger.debug("Found {} visits matching diagnosis '{}', returning page {} with {} results", 
                        filteredVisits.size(), text, pageable.getPageNumber(), page.getContent().size());
            
            return new PagedResponse<>(page);
            
        } catch (Exception e) {
            logger.error("Error searching visits by diagnosis '{}': {}", text, e.getMessage(), e);
            return createEmptyPagedResponse(pageable);
        }
    }

    @Override
    public PagedResponse<Visit> searchByDescription(String text, Pageable pageable) {
        logger.debug("Searching visits by description: '{}' with pagination: {}", text, pageable);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty search text provided for description search");
                return createEmptyPagedResponse(pageable);
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            // Filter visits by notes (description field doesn't exist in Visit model)
            List<Visit> filteredVisits = allVisits.stream()
                    .filter(visit -> visit.getNotes() != null && 
                            visit.getNotes().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            
            // Apply pagination
            Page<Visit> page = createPageFromList(filteredVisits, pageable);
            
            logger.debug("Found {} visits matching description '{}', returning page {} with {} results", 
                        filteredVisits.size(), text, pageable.getPageNumber(), page.getContent().size());
            
            return new PagedResponse<>(page);
            
        } catch (Exception e) {
            logger.error("Error searching visits by description '{}': {}", text, e.getMessage(), e);
            return createEmptyPagedResponse(pageable);
        }
    }

    @Override
    public PagedResponse<Visit> searchByNotes(String text, Pageable pageable) {
        logger.debug("Searching visits by notes: '{}' with pagination: {}", text, pageable);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty search text provided for notes search");
                return createEmptyPagedResponse(pageable);
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            // Filter visits by notes
            List<Visit> filteredVisits = allVisits.stream()
                    .filter(visit -> visit.getNotes() != null && 
                            visit.getNotes().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            
            // Apply pagination
            Page<Visit> page = createPageFromList(filteredVisits, pageable);
            
            logger.debug("Found {} visits matching notes '{}', returning page {} with {} results", 
                        filteredVisits.size(), text, pageable.getPageNumber(), page.getContent().size());
            
            return new PagedResponse<>(page);
            
        } catch (Exception e) {
            logger.error("Error searching visits by notes '{}': {}", text, e.getMessage(), e);
            return createEmptyPagedResponse(pageable);
        }
    }

    @Override
    public long countByTreatment(String text) {
        logger.debug("Counting visits by treatment: '{}'", text);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            long count = allVisits.stream()
                    .filter(visit -> visit.getTreatment() != null && 
                            visit.getTreatment().toLowerCase().contains(searchText))
                    .count();
            
            logger.debug("Found {} visits matching treatment '{}'", count, text);
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting visits by treatment '{}': {}", text, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByDiagnosis(String text) {
        logger.debug("Counting visits by diagnosis: '{}'", text);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            long count = allVisits.stream()
                    .filter(visit -> visit.getDiagnosis() != null && 
                            visit.getDiagnosis().toLowerCase().contains(searchText))
                    .count();
            
            logger.debug("Found {} visits matching diagnosis '{}'", count, text);
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting visits by diagnosis '{}': {}", text, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByDescription(String text) {
        logger.debug("Counting visits by description: '{}'", text);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            long count = allVisits.stream()
                    .filter(visit -> visit.getNotes() != null && 
                            visit.getNotes().toLowerCase().contains(searchText))
                    .count();
            
            logger.debug("Found {} visits matching description '{}'", count, text);
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting visits by description '{}': {}", text, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public long countByNotes(String text) {
        logger.debug("Counting visits by notes: '{}'", text);
        
        try {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            
            String searchText = text.trim().toLowerCase();
            List<Visit> allVisits = visitService.findAll();
            
            long count = allVisits.stream()
                    .filter(visit -> visit.getNotes() != null && 
                            visit.getNotes().toLowerCase().contains(searchText))
                    .count();
            
            logger.debug("Found {} visits matching notes '{}'", count, text);
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting visits by notes '{}': {}", text, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Create a paginated page from a list of visits
     */
    private Page<Visit> createPageFromList(List<Visit> visits, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), visits.size());
        
        if (start >= visits.size()) {
            return new PageImpl<>(List.of(), pageable, visits.size());
        }
        
        List<Visit> pageContent = visits.subList(start, end);
        return new PageImpl<>(pageContent, pageable, visits.size());
    }

    /**
     * Create an empty paged response
     */
    private PagedResponse<Visit> createEmptyPagedResponse(Pageable pageable) {
        Page<Visit> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        return new PagedResponse<>(emptyPage);
    }
}