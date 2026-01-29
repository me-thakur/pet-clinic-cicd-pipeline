package com.petclinic.backend.service;

import java.util.List;
import java.util.Optional;

/**
 * Base service interface providing common CRUD operations
 * @param <T> Entity type
 * @param <ID> ID type
 */
public interface BaseService<T, ID> {
    
    /**
     * Create a new entity
     * @param entity Entity to create
     * @return Created entity
     */
    T create(T entity);
    
    /**
     * Update an existing entity
     * @param id Entity ID
     * @param entity Updated entity data
     * @return Updated entity
     */
    T update(ID id, T entity);
    
    /**
     * Find entity by ID
     * @param id Entity ID
     * @return Entity if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Find all entities
     * @return List of all entities
     */
    List<T> findAll();
    
    /**
     * Delete entity by ID
     * @param id Entity ID
     */
    void deleteById(ID id);
    
    /**
     * Check if entity exists by ID
     * @param id Entity ID
     * @return true if exists, false otherwise
     */
    boolean existsById(ID id);
    
    /**
     * Count total number of entities
     * @return Total count
     */
    long count();
}