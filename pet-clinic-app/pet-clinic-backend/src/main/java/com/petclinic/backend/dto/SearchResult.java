package com.petclinic.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * DTO representing a single search result with highlighting
 * Validates: Requirements 4.1, 4.3, 4.4
 */
public class SearchResult {
    
    private String entityType;
    private Long entityId;
    private String title;
    private String description;
    private String highlightedTitle;
    private String highlightedDescription;
    private List<String> matchedFields;
    private Double relevanceScore;
    private LocalDateTime lastModified;
    private String url;
    
    // Constructors
    public SearchResult() {}
    
    public SearchResult(String entityType, Long entityId, String title, String description) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.title = title;
        this.description = description;
        this.relevanceScore = 1.0;
    }
    
    public SearchResult(String entityType, Long entityId, String title, String description, 
                       String highlightedTitle, String highlightedDescription, 
                       List<String> matchedFields, Double relevanceScore) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.title = title;
        this.description = description;
        this.highlightedTitle = highlightedTitle;
        this.highlightedDescription = highlightedDescription;
        this.matchedFields = matchedFields;
        this.relevanceScore = relevanceScore;
    }
    
    // Getters and Setters
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getHighlightedTitle() {
        return highlightedTitle != null ? highlightedTitle : title;
    }
    
    public void setHighlightedTitle(String highlightedTitle) {
        this.highlightedTitle = highlightedTitle;
    }
    
    public String getHighlightedDescription() {
        return highlightedDescription != null ? highlightedDescription : description;
    }
    
    public void setHighlightedDescription(String highlightedDescription) {
        this.highlightedDescription = highlightedDescription;
    }
    
    public List<String> getMatchedFields() {
        return matchedFields;
    }
    
    public void setMatchedFields(List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }
    
    public Double getRelevanceScore() {
        return relevanceScore != null ? relevanceScore : 1.0;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    // Business Methods
    public boolean hasHighlighting() {
        return highlightedTitle != null || highlightedDescription != null;
    }
    
    public boolean hasMatchedFields() {
        return matchedFields != null && !matchedFields.isEmpty();
    }
    
    public int getMatchedFieldCount() {
        return matchedFields != null ? matchedFields.size() : 0;
    }
    
    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(entityType, that.entityType) &&
               Objects.equals(entityId, that.entityId) &&
               Objects.equals(title, that.title);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(entityType, entityId, title);
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
                "entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", title='" + title + '\'' +
                ", relevanceScore=" + relevanceScore +
                ", matchedFields=" + getMatchedFieldCount() +
                '}';
    }
}