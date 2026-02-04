package com.petclinic.frontend.service;

import com.petclinic.frontend.model.ReportFilter;
import com.petclinic.frontend.model.VeterinarianOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .bodyToMono(new ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {})
                .map(list -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    // If we get a list of reports, aggregate the visitsByVeterinarian data
                    if (!list.isEmpty()) {
                        // Assuming the first report contains the aggregated data
                        Map<String, Object> firstReport = list.get(0);
                        Object visitsByVet = firstReport.get("visitsByVeterinarian");
                        if (visitsByVet instanceof Map) {
                            result.putAll((Map<String, Object>) visitsByVet);
                        }
                    }
                    return result;
                })
                .onErrorReturn(new java.util.HashMap<>());
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
                .bodyToMono(new ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {})
                .map(list -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    // If we get a list of reports, aggregate the visitsBySpecies data
                    if (!list.isEmpty()) {
                        // Assuming the first report contains the aggregated data
                        Map<String, Object> firstReport = list.get(0);
                        Object visitsBySpecies = firstReport.get("visitsBySpecies");
                        if (visitsBySpecies instanceof Map) {
                            result.putAll((Map<String, Object>) visitsBySpecies);
                        }
                    }
                    return result;
                })
                .onErrorReturn(new java.util.HashMap<>());
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
                .bodyToMono(new ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {})
                .map(list -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    // Aggregate revenue data from the list of reports
                    for (Map<String, Object> report : list) {
                        Object revenueByVet = report.get("revenueByVeterinarian");
                        if (revenueByVet instanceof Map) {
                            result.putAll((Map<String, Object>) revenueByVet);
                        }
                    }
                    return result;
                })
                .onErrorReturn(new java.util.HashMap<>());
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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                         clientResponse -> {
                             return clientResponse.bodyToMono(String.class)
                                 .map(body -> new org.springframework.web.reactive.function.client.WebClientResponseException(
                                     clientResponse.statusCode().value(), 
                                     "Export failed: " + clientResponse.statusCode(), 
                                     null, body.getBytes(), null));
                         })
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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                         clientResponse -> {
                             return clientResponse.bodyToMono(String.class)
                                 .map(body -> new org.springframework.web.reactive.function.client.WebClientResponseException(
                                     clientResponse.statusCode().value(), 
                                     "Export failed: " + clientResponse.statusCode(), 
                                     null, body.getBytes(), null));
                         })
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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                         clientResponse -> {
                             return clientResponse.bodyToMono(String.class)
                                 .map(body -> new org.springframework.web.reactive.function.client.WebClientResponseException(
                                     clientResponse.statusCode().value(), 
                                     "Export failed: " + clientResponse.statusCode(), 
                                     null, body.getBytes(), null));
                         })
                .toEntity(byte[].class);
    }

    /**
     * Get available filter options
     */
    public Mono<ReportFilter> getAvailableFilterOptions() {
        return webClient.get()
                .uri("/reports/filter-options")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::convertToFrontendReportFilter);
    }
    
    /**
     * Convert backend response to frontend ReportFilter model
     */
    @SuppressWarnings("unchecked")
    private ReportFilter convertToFrontendReportFilter(Map<String, Object> backendResponse) {
        ReportFilter filter = new ReportFilter();
        
        // Convert available veterinarians
        List<Map<String, Object>> backendVets = (List<Map<String, Object>>) backendResponse.get("availableVeterinarians");
        if (backendVets != null) {
            List<VeterinarianOption> frontendVets = backendVets.stream()
                    .map(vetMap -> {
                        VeterinarianOption vet = new VeterinarianOption();
                        vet.setId(((Number) vetMap.get("id")).longValue());
                        vet.setFirstName((String) vetMap.get("firstName"));
                        vet.setLastName((String) vetMap.get("lastName"));
                        return vet;
                    })
                    .collect(Collectors.toList());
            filter.setAvailableVeterinarians(frontendVets);
        }
        
        // Convert species list
        List<String> species = (List<String>) backendResponse.get("species");
        if (species != null) {
            filter.setSpecies(species);
        }
        
        // Convert visit types
        List<String> visitTypes = (List<String>) backendResponse.get("visitTypes");
        if (visitTypes != null) {
            filter.setVisitTypes(visitTypes);
        }
        
        return filter;
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