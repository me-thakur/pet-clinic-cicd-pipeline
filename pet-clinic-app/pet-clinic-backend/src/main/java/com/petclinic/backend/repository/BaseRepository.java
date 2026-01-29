package com.petclinic.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base repository interface providing common query methods
 * @param <T> Entity type
 * @param <ID> ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    
    /**
     * Find entities created after a specific date
     * @param date Date to search from
     * @return List of entities created after the date
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt > :date")
    List<T> findCreatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Find entities updated after a specific date
     * @param date Date to search from
     * @return List of entities updated after the date
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt > :date")
    List<T> findUpdatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Find entities created between two dates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of entities created in the date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<T> findCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find entities with pagination, ordered by creation date (newest first)
     * @param pageable Pagination information
     * @return Page of entities ordered by creation date
     */
    @Query("SELECT e FROM #{#entityName} e ORDER BY e.createdAt DESC")
    Page<T> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find entities with pagination, ordered by update date (newest first)
     * @param pageable Pagination information
     * @return Page of entities ordered by update date
     */
    @Query("SELECT e FROM #{#entityName} e ORDER BY e.updatedAt DESC")
    Page<T> findAllOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * Count entities created after a specific date
     * @param date Date to count from
     * @return Number of entities created after the date
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdAt > :date")
    long countCreatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Count entities updated after a specific date
     * @param date Date to count from
     * @return Number of entities updated after the date
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.updatedAt > :date")
    long countUpdatedAfter(@Param("date") LocalDateTime date);
}