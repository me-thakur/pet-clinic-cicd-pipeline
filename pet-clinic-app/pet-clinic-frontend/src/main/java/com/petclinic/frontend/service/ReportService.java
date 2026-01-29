package com.petclinic.frontend.service;

import com.petclinic.frontend.model.ReportFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

/**
 * Report Service
 * 
 * Service class for managing report operations through the backend REST API.
 * Provides methods for generating and exporting various reports.
 */
@Service
public class ReportService {

    private final WebClient webClient;

    @Autowired
    public ReportService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Generate visit statistics report
     */
    public Mono<Map<String, Object>> getVisitStatistics(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/visits")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Generate visit statistics report with filters
     */
    public Mono<Map<String, Object>> getVisitStatisticsWithFilter(ReportFilter filter) {
        return webClient.post()
                .uri("/reports/visits")
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Generate revenue report
     */
    public Mono<Map<String, Object>> getRevenueReport(LocalDate startDate, LocalDate endDate, boolean includeTrends) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/revenue")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("includeTrends", includeTrends)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Generate revenue report with filters
     */
    public Mono<Map<String, Object>> getRevenueReportWithFilter(ReportFilter filter) {
        return webClient.post()
                .uri("/reports/revenue")
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get visit statistics by veterinarian
     */
    public Mono<Map<String, Object>> getVisitStatisticsByVeterinarian(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/visits/by-veterinarian")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get visit statistics by species
     */
    public Mono<Map<String, Object>> getVisitStatisticsBySpecies(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/visits/by-species")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get revenue report by veterinarian
     */
    public Mono<Map<String, Object>> getRevenueReportByVeterinarian(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/revenue/by-veterinarian")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get monthly revenue trends
     */
    public Mono<Map<String, Object>> getMonthlyRevenueTrends(int months) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/revenue/trends")
                        .queryParam("months", months)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Export visit statistics report
     */
    public Mono<ResponseEntity<byte[]>> exportVisitStatistics(LocalDate startDate, LocalDate endDate, String format) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/export/visits")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("format", format)
                        .build())
                .retrieve()
                .toEntity(byte[].class);
    }

    /**
     * Export revenue report
     */
    public Mono<ResponseEntity<byte[]>> exportRevenueReport(LocalDate startDate, LocalDate endDate, String format, boolean includeTrends) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/export/revenue")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("format", format)
                        .queryParam("includeTrends", includeTrends)
                        .build())
                .retrieve()
                .toEntity(byte[].class);
    }

    /**
     * Export dashboard metrics
     */
    public Mono<ResponseEntity<byte[]>> exportDashboardMetrics(String format, LocalDate date) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/reports/export/dashboard")
                            .queryParam("format", format);
                    if (date != null) {
                        builder.queryParam("date", date);
                    }
                    return builder.build();
                })
                .retrieve()
                .toEntity(byte[].class);
    }

    /**
     * Get available filter options
     */
    public Mono<ReportFilter> getAvailableFilterOptions() {
        return webClient.get()
                .uri("/reports/filter-options")
                .retrieve()
                .bodyToMono(ReportFilter.class);
    }

    /**
     * Validate report filter
     */
    public Mono<Boolean> validateReportFilter(ReportFilter filter) {
        return webClient.post()
                .uri("/reports/validate-filter")
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}