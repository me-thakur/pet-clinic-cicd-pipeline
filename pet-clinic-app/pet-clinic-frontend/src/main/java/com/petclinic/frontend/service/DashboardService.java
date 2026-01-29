package com.petclinic.frontend.service;

import com.petclinic.frontend.model.DashboardMetrics;
import com.petclinic.frontend.model.ReportFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service
 * 
 * Service class for managing dashboard operations through the backend REST API.
 * Provides methods for retrieving dashboard metrics and real-time analytics.
 */
@Service
public class DashboardService {

    private final WebClient webClient;

    @Autowired
    public DashboardService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get real-time dashboard metrics
     */
    public Mono<DashboardMetrics> getRealTimeDashboardMetrics() {
        return webClient.get()
                .uri("/dashboard/metrics")
                .retrieve()
                .bodyToMono(DashboardMetrics.class);
    }

    /**
     * Get dashboard metrics for specific date
     */
    public Mono<DashboardMetrics> getDashboardMetricsForDate(LocalDate date) {
        return webClient.get()
                .uri("/dashboard/metrics/{date}", date)
                .retrieve()
                .bodyToMono(DashboardMetrics.class);
    }

    /**
     * Get dashboard metrics for date range
     */
    public Mono<DashboardMetrics> getDashboardMetricsForRange(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/metrics/range")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(DashboardMetrics.class);
    }

    /**
     * Get dashboard metrics with filters
     */
    public Mono<DashboardMetrics> getDashboardMetricsWithFilter(ReportFilter filter) {
        return webClient.post()
                .uri("/dashboard/metrics/filtered")
                .bodyValue(filter)
                .retrieve()
                .bodyToMono(DashboardMetrics.class);
    }

    /**
     * Get daily appointment metrics
     */
    public Mono<Map<String, Long>> getDailyAppointmentMetrics(LocalDate date) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/appointments/daily")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {});
    }

    /**
     * Get active pets count
     */
    public Mono<Long> getActivePetsCount() {
        return webClient.get()
                .uri("/dashboard/pets/count")
                .retrieve()
                .bodyToMono(Long.class);
    }

    /**
     * Get active pets by species
     */
    public Mono<Map<String, Long>> getActivePetsBySpecies() {
        return webClient.get()
                .uri("/dashboard/pets/by-species")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {});
    }

    /**
     * Get veterinarian utilization
     */
    public Mono<Map<String, Object>> getVeterinarianUtilization(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/utilization")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get recent activities
     */
    public Mono<List<DashboardMetrics.RecentActivity>> getRecentActivities(int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/activities/recent")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToFlux(DashboardMetrics.RecentActivity.class)
                .collectList();
    }

    /**
     * Get upcoming appointments
     */
    public Mono<List<DashboardMetrics.UpcomingAppointment>> getUpcomingAppointments(int days, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/appointments/upcoming")
                        .queryParam("days", days)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToFlux(DashboardMetrics.UpcomingAppointment.class)
                .collectList();
    }

    /**
     * Get system alerts
     */
    public Mono<List<DashboardMetrics.Alert>> getSystemAlerts() {
        return webClient.get()
                .uri("/dashboard/alerts")
                .retrieve()
                .bodyToFlux(DashboardMetrics.Alert.class)
                .collectList();
    }

    /**
     * Get performance indicators
     */
    public Mono<Map<String, Object>> getPerformanceIndicators(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/performance")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get revenue metrics
     */
    public Mono<Map<String, Object>> getRevenueMetrics(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/revenue")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Get top veterinarians by visits
     */
    public Mono<Map<String, Long>> getTopVeterinariansByVisits(LocalDate startDate, LocalDate endDate, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/top-veterinarians/visits")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {});
    }

    /**
     * Get monthly trends
     */
    public Mono<Map<String, Object>> getMonthlyTrends(int months) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dashboard/trends/monthly")
                        .queryParam("months", months)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Refresh dashboard cache
     */
    public Mono<Map<String, String>> refreshDashboardCache() {
        return webClient.post()
                .uri("/dashboard/refresh")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }
}