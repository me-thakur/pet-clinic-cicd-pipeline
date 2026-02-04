package com.petclinic.backend.service.impl;

import com.petclinic.backend.dto.PetWithOwnerInfo;
import com.petclinic.backend.model.Pet;
import com.petclinic.backend.repository.PetRepository;
import com.petclinic.backend.service.EnhancedPetService;
import com.petclinic.backend.service.PetOwnerDataErrorHandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enhanced Pet Service implementation with owner information
 * Extends PetServiceImpl to provide methods that include joined owner data
 * Validates: Requirements 5.1, 5.2, 5.3, 5.5
 */
@Service("enhancedPetService")
@Primary
@Transactional
public class EnhancedPetServiceImpl extends PetServiceImpl implements EnhancedPetService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedPetServiceImpl.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private PetRepository petRepository;
    
    @Autowired
    private PetOwnerDataErrorHandlingService petOwnerDataErrorHandlingService;
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> findPetsWithOwnerInfo(Pageable pageable) {
        logger.debug("Finding pets with owner info, page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        PetOwnerDataErrorHandlingService.DataOperationContext context = 
            new PetOwnerDataErrorHandlingService.DataOperationContext();
        context.setPageable(pageable);
        context.setAllowPartialData(true);
        context.setRequestId("FIND_PETS_" + System.currentTimeMillis());
        
        return petOwnerDataErrorHandlingService.executeDataOperationWithRecovery(
            () -> {
                String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                        "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                        "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                        "o.address, o.city, o.state, o.zipCode" +
                        ") FROM Pet p LEFT JOIN p.owner o ORDER BY p.name ASC";
                
                Query query = entityManager.createQuery(jpql);
                query.setFirstResult((int) pageable.getOffset());
                query.setMaxResults(pageable.getPageSize());
                
                @SuppressWarnings("unchecked")
                List<PetWithOwnerInfo> results = query.getResultList();
                
                // Get total count
                String countJpql = "SELECT COUNT(p) FROM Pet p";
                Query countQuery = entityManager.createQuery(countJpql);
                Long total = (Long) countQuery.getSingleResult();
                
                logger.debug("Found {} pets with owner info out of {} total", results.size(), total);
                return new PageImpl<>(results, pageable, total);
            },
            "find_pets_with_owner_info",
            context
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PetWithOwnerInfo> findAllPetsWithOwnerInfo() {
        logger.debug("Finding all pets with owner info");
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        logger.debug("Found {} pets with owner info", results.size());
        return results;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PetWithOwnerInfo> findPetsWithOwnerInfoByOwner(Long ownerId) {
        logger.debug("Finding pets with owner info for owner ID: {}", ownerId);
        
        if (ownerId == null) {
            logger.warn("Owner ID is null, returning empty list");
            return List.of();
        }
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p JOIN p.owner o WHERE o.id = :ownerId ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("ownerId", ownerId);
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        logger.debug("Found {} pets with owner info for owner ID: {}", results.size(), ownerId);
        return results;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<PetWithOwnerInfo> findPetWithOwnerInfo(Long petId) {
        logger.debug("Finding pet with owner info for pet ID: {}", petId);
        
        if (petId == null) {
            logger.warn("Pet ID is null, returning empty optional");
            return Optional.empty();
        }
        
        PetOwnerDataErrorHandlingService.DataOperationContext context = 
            new PetOwnerDataErrorHandlingService.DataOperationContext();
        context.setPetId(petId);
        context.setAllowPartialData(true);
        context.setRequestId("FIND_PET_" + petId + "_" + System.currentTimeMillis());
        
        return petOwnerDataErrorHandlingService.executeDataOperationWithRecovery(
            () -> {
                String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                        "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                        "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                        "o.address, o.city, o.state, o.zipCode" +
                        ") FROM Pet p LEFT JOIN p.owner o WHERE p.id = :petId";
                
                Query query = entityManager.createQuery(jpql);
                query.setParameter("petId", petId);
                
                @SuppressWarnings("unchecked")
                List<PetWithOwnerInfo> results = query.getResultList();
                
                if (results.isEmpty()) {
                    logger.debug("No pet found with ID: {}", petId);
                    return Optional.empty();
                }
                
                PetWithOwnerInfo result = results.get(0);
                logger.debug("Found pet with owner info: {}", result);
                return Optional.of(result);
            },
            "find_pet_with_owner_info",
            context
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> searchPetsWithOwnerInfo(String searchTerm, Pageable pageable) {
        logger.debug("Searching pets with owner info for term: '{}', page: {}, size: {}", 
                    searchTerm, pageable.getPageNumber(), pageable.getPageSize());
        
        if (!StringUtils.hasText(searchTerm)) {
            return findPetsWithOwnerInfo(pageable);
        }
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o " +
                "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.species) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(CONCAT(o.firstName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("searchTerm", searchTerm);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Get total count for search
        String countJpql = "SELECT COUNT(p) FROM Pet p LEFT JOIN p.owner o " +
                "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.species) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(CONCAT(o.firstName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))";
        
        Query countQuery = entityManager.createQuery(countJpql);
        countQuery.setParameter("searchTerm", searchTerm);
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Found {} pets with owner info matching search term '{}' out of {} total", 
                    results.size(), searchTerm, total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> findPetsWithOwnerInfoBySpecies(String species, Pageable pageable) {
        logger.debug("Finding pets with owner info by species: '{}', page: {}, size: {}", 
                    species, pageable.getPageNumber(), pageable.getPageSize());
        
        if (!StringUtils.hasText(species)) {
            return findPetsWithOwnerInfo(pageable);
        }
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o WHERE LOWER(p.species) = LOWER(:species) ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("species", species);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Get total count
        String countJpql = "SELECT COUNT(p) FROM Pet p LEFT JOIN p.owner o WHERE LOWER(p.species) = LOWER(:species)";
        
        Query countQuery = entityManager.createQuery(countJpql);
        countQuery.setParameter("species", species);
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Found {} pets with owner info for species '{}' out of {} total", 
                    results.size(), species, total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> findPetsWithOwnerInfoByBreed(String breed, Pageable pageable) {
        logger.debug("Finding pets with owner info by breed: '{}', page: {}, size: {}", 
                    breed, pageable.getPageNumber(), pageable.getPageSize());
        
        if (!StringUtils.hasText(breed)) {
            return findPetsWithOwnerInfo(pageable);
        }
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o WHERE LOWER(p.breed) LIKE LOWER(CONCAT('%', :breed, '%')) ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("breed", breed);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Get total count
        String countJpql = "SELECT COUNT(p) FROM Pet p LEFT JOIN p.owner o WHERE LOWER(p.breed) LIKE LOWER(CONCAT('%', :breed, '%'))";
        
        Query countQuery = entityManager.createQuery(countJpql);
        countQuery.setParameter("breed", breed);
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Found {} pets with owner info for breed '{}' out of {} total", 
                    results.size(), breed, total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> findOrphanedPets(Pageable pageable) {
        logger.debug("Finding orphaned pets (pets without owners), page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "null, null, null, null, null, null, null, null, null, null" +
                ") FROM Pet p WHERE p.owner IS NULL ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Get total count
        String countJpql = "SELECT COUNT(p) FROM Pet p WHERE p.owner IS NULL";
        Query countQuery = entityManager.createQuery(countJpql);
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Found {} orphaned pets out of {} total", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> findPetsWithOwnerInfoByAgeRange(int minAge, int maxAge, Pageable pageable) {
        logger.debug("Finding pets with owner info by age range: {}-{}, page: {}, size: {}", 
                    minAge, maxAge, pageable.getPageNumber(), pageable.getPageSize());
        
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);
        
        String jpql = "SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o WHERE p.birthDate BETWEEN :minBirthDate AND :maxBirthDate ORDER BY p.name ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("minBirthDate", minBirthDate);
        query.setParameter("maxBirthDate", maxBirthDate);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Get total count
        String countJpql = "SELECT COUNT(p) FROM Pet p LEFT JOIN p.owner o WHERE p.birthDate BETWEEN :minBirthDate AND :maxBirthDate";
        
        Query countQuery = entityManager.createQuery(countJpql);
        countQuery.setParameter("minBirthDate", minBirthDate);
        countQuery.setParameter("maxBirthDate", maxBirthDate);
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Found {} pets with owner info in age range {}-{} out of {} total", 
                    results.size(), minAge, maxAge, total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PetWithOwnerInfo> advancedSearchPetsWithOwnerInfo(
            String petName, String petSpecies, String petBreed,
            String ownerFirstName, String ownerLastName, String ownerEmail,
            Pageable pageable) {
        
        logger.debug("Advanced search pets with owner info - petName: '{}', species: '{}', breed: '{}', " +
                    "ownerFirstName: '{}', ownerLastName: '{}', ownerEmail: '{}'", 
                    petName, petSpecies, petBreed, ownerFirstName, ownerLastName, ownerEmail);
        
        StringBuilder jpql = new StringBuilder("SELECT new com.petclinic.backend.dto.PetWithOwnerInfo(" +
                "p.id, p.name, p.species, p.breed, p.birthDate, p.medicalHistory, p.createdAt, p.updatedAt, " +
                "o.id, o.firstName, o.lastName, o.email, o.mobileNumber, o.telephone, " +
                "o.address, o.city, o.state, o.zipCode" +
                ") FROM Pet p LEFT JOIN p.owner o WHERE 1=1");
        
        if (StringUtils.hasText(petName)) {
            jpql.append(" AND LOWER(p.name) LIKE LOWER(CONCAT('%', :petName, '%'))");
        }
        if (StringUtils.hasText(petSpecies)) {
            jpql.append(" AND LOWER(p.species) = LOWER(:petSpecies)");
        }
        if (StringUtils.hasText(petBreed)) {
            jpql.append(" AND LOWER(p.breed) LIKE LOWER(CONCAT('%', :petBreed, '%'))");
        }
        if (StringUtils.hasText(ownerFirstName)) {
            jpql.append(" AND LOWER(o.firstName) LIKE LOWER(CONCAT('%', :ownerFirstName, '%'))");
        }
        if (StringUtils.hasText(ownerLastName)) {
            jpql.append(" AND LOWER(o.lastName) LIKE LOWER(CONCAT('%', :ownerLastName, '%'))");
        }
        if (StringUtils.hasText(ownerEmail)) {
            jpql.append(" AND LOWER(o.email) LIKE LOWER(CONCAT('%', :ownerEmail, '%'))");
        }
        
        jpql.append(" ORDER BY p.name ASC");
        
        Query query = entityManager.createQuery(jpql.toString());
        
        if (StringUtils.hasText(petName)) {
            query.setParameter("petName", petName);
        }
        if (StringUtils.hasText(petSpecies)) {
            query.setParameter("petSpecies", petSpecies);
        }
        if (StringUtils.hasText(petBreed)) {
            query.setParameter("petBreed", petBreed);
        }
        if (StringUtils.hasText(ownerFirstName)) {
            query.setParameter("ownerFirstName", ownerFirstName);
        }
        if (StringUtils.hasText(ownerLastName)) {
            query.setParameter("ownerLastName", ownerLastName);
        }
        if (StringUtils.hasText(ownerEmail)) {
            query.setParameter("ownerEmail", ownerEmail);
        }
        
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        @SuppressWarnings("unchecked")
        List<PetWithOwnerInfo> results = query.getResultList();
        
        // Build count query
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(p) FROM Pet p LEFT JOIN p.owner o WHERE 1=1");
        
        if (StringUtils.hasText(petName)) {
            countJpql.append(" AND LOWER(p.name) LIKE LOWER(CONCAT('%', :petName, '%'))");
        }
        if (StringUtils.hasText(petSpecies)) {
            countJpql.append(" AND LOWER(p.species) = LOWER(:petSpecies)");
        }
        if (StringUtils.hasText(petBreed)) {
            countJpql.append(" AND LOWER(p.breed) LIKE LOWER(CONCAT('%', :petBreed, '%'))");
        }
        if (StringUtils.hasText(ownerFirstName)) {
            countJpql.append(" AND LOWER(o.firstName) LIKE LOWER(CONCAT('%', :ownerFirstName, '%'))");
        }
        if (StringUtils.hasText(ownerLastName)) {
            countJpql.append(" AND LOWER(o.lastName) LIKE LOWER(CONCAT('%', :ownerLastName, '%'))");
        }
        if (StringUtils.hasText(ownerEmail)) {
            countJpql.append(" AND LOWER(o.email) LIKE LOWER(CONCAT('%', :ownerEmail, '%'))");
        }
        
        Query countQuery = entityManager.createQuery(countJpql.toString());
        
        if (StringUtils.hasText(petName)) {
            countQuery.setParameter("petName", petName);
        }
        if (StringUtils.hasText(petSpecies)) {
            countQuery.setParameter("petSpecies", petSpecies);
        }
        if (StringUtils.hasText(petBreed)) {
            countQuery.setParameter("petBreed", petBreed);
        }
        if (StringUtils.hasText(ownerFirstName)) {
            countQuery.setParameter("ownerFirstName", ownerFirstName);
        }
        if (StringUtils.hasText(ownerLastName)) {
            countQuery.setParameter("ownerLastName", ownerLastName);
        }
        if (StringUtils.hasText(ownerEmail)) {
            countQuery.setParameter("ownerEmail", ownerEmail);
        }
        
        Long total = (Long) countQuery.getSingleResult();
        
        logger.debug("Advanced search found {} pets with owner info out of {} total", results.size(), total);
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<PetWithOwnerInfo> refreshPetOwnerInfo(Long petId) {
        logger.debug("Refreshing pet-owner info for pet ID: {}", petId);
        
        // Use the error handling service for refresh with retry
        return petOwnerDataErrorHandlingService.refreshPetOwnerDataWithRetry(petId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long[] getPetOwnershipStatistics() {
        logger.debug("Getting pet ownership statistics");
        
        String jpql = "SELECT COUNT(CASE WHEN p.owner IS NOT NULL THEN 1 END) as petsWithOwners, " +
                "COUNT(CASE WHEN p.owner IS NULL THEN 1 END) as petsWithoutOwners FROM Pet p";
        
        Query query = entityManager.createQuery(jpql);
        Object[] result = (Object[]) query.getSingleResult();
        
        long petsWithOwners = result[0] != null ? ((Number) result[0]).longValue() : 0L;
        long petsWithoutOwners = result[1] != null ? ((Number) result[1]).longValue() : 0L;
        
        logger.debug("Pet ownership statistics - with owners: {}, without owners: {}", 
                    petsWithOwners, petsWithoutOwners);
        
        return new long[]{petsWithOwners, petsWithoutOwners};
    }
}