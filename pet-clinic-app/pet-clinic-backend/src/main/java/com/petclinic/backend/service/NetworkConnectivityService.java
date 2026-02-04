package com.petclinic.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Network connectivity detection and retry mechanisms
 * Requirements: 15.1, 15.2, 15.3
 */
@Service
public class NetworkConnectivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkConnectivityService.class);
    
    private final AtomicBoolean networkAvailable = new AtomicBoolean(true);
    private final AtomicReference<LocalDateTime> lastConnectivityCheck = new AtomicReference<>(LocalDateTime.now());
    private final AtomicReference<LocalDateTime> lastConnectivityLoss = new AtomicReference<>();
    
    // Configuration
    private static final int CONNECTIVITY_TIMEOUT = 5000; // 5 seconds
    private static final String[] TEST_HOSTS = {
        "8.8.8.8",      // Google DNS
        "1.1.1.1",      // Cloudflare DNS
        "208.67.222.222" // OpenDNS
    };
    private static final int[] TEST_PORTS = { 53, 80, 443 };
    
    /**
     * Check if network connectivity is available
     */
    public boolean isNetworkAvailable() {
        return networkAvailable.get();
    }
    
    /**
     * Get last connectivity check time
     */
    public LocalDateTime getLastConnectivityCheck() {
        return lastConnectivityCheck.get();
    }
    
    /**
     * Get last connectivity loss time
     */
    public LocalDateTime getLastConnectivityLoss() {
        return lastConnectivityLoss.get();
    }
    
    /**
     * Perform immediate connectivity check
     */
    public boolean checkConnectivity() {
        boolean isConnected = performConnectivityTest();
        updateConnectivityStatus(isConnected);
        return isConnected;
    }
    
    /**
     * Perform connectivity test with multiple methods
     */
    private boolean performConnectivityTest() {
        // Test 1: DNS resolution
        if (testDnsResolution()) {
            logger.debug("DNS resolution test passed");
            return true;
        }
        
        // Test 2: Socket connection to known hosts
        if (testSocketConnections()) {
            logger.debug("Socket connection test passed");
            return true;
        }
        
        // Test 3: Ping test (if available)
        if (testPingConnectivity()) {
            logger.debug("Ping connectivity test passed");
            return true;
        }
        
        logger.debug("All connectivity tests failed");
        return false;
    }
    
    /**
     * Test DNS resolution
     */
    private boolean testDnsResolution() {
        try {
            InetAddress.getByName("google.com");
            return true;
        } catch (UnknownHostException e) {
            logger.debug("DNS resolution failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Test socket connections to known hosts
     */
    private boolean testSocketConnections() {
        for (String host : TEST_HOSTS) {
            for (int port : TEST_PORTS) {
                if (testSocketConnection(host, port)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Test socket connection to specific host and port
     */
    private boolean testSocketConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTIVITY_TIMEOUT);
            return true;
        } catch (IOException e) {
            logger.debug("Socket connection to {}:{} failed: {}", host, port, e.getMessage());
            return false;
        }
    }
    
    /**
     * Test ping connectivity (platform dependent)
     */
    private boolean testPingConnectivity() {
        try {
            for (String host : TEST_HOSTS) {
                InetAddress address = InetAddress.getByName(host);
                if (address.isReachable(CONNECTIVITY_TIMEOUT)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Ping connectivity test failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Update connectivity status
     */
    private void updateConnectivityStatus(boolean isConnected) {
        boolean wasConnected = networkAvailable.get();
        networkAvailable.set(isConnected);
        lastConnectivityCheck.set(LocalDateTime.now());
        
        if (wasConnected && !isConnected) {
            lastConnectivityLoss.set(LocalDateTime.now());
            logger.warn("Network connectivity lost");
        } else if (!wasConnected && isConnected) {
            logger.info("Network connectivity restored");
        }
    }
    
    /**
     * Scheduled connectivity monitoring
     */
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void monitorConnectivity() {
        checkConnectivity();
    }
    
    /**
     * Wait for network connectivity with timeout
     */
    public boolean waitForConnectivity(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (checkConnectivity()) {
                return true;
            }
            
            try {
                Thread.sleep(1000); // Wait 1 second between checks
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Async connectivity check
     */
    public CompletableFuture<Boolean> checkConnectivityAsync() {
        return CompletableFuture.supplyAsync(this::performConnectivityTest)
            .orTimeout(CONNECTIVITY_TIMEOUT * 2, TimeUnit.MILLISECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.debug("Async connectivity check failed: {}", throwable.getMessage());
                    updateConnectivityStatus(false);
                } else {
                    updateConnectivityStatus(result);
                }
            });
    }
    
    /**
     * Get connectivity statistics
     */
    public ConnectivityStats getConnectivityStats() {
        return new ConnectivityStats(
            networkAvailable.get(),
            lastConnectivityCheck.get(),
            lastConnectivityLoss.get(),
            calculateUptime()
        );
    }
    
    /**
     * Calculate network uptime percentage
     */
    private double calculateUptime() {
        LocalDateTime lossTime = lastConnectivityLoss.get();
        if (lossTime == null) {
            return 100.0; // No recorded loss
        }
        
        LocalDateTime now = LocalDateTime.now();
        long totalMinutes = java.time.Duration.between(lossTime.minusHours(24), now).toMinutes();
        long downMinutes = networkAvailable.get() ? 0 : java.time.Duration.between(lossTime, now).toMinutes();
        
        if (totalMinutes == 0) return 100.0;
        return Math.max(0.0, 100.0 - ((double) downMinutes / totalMinutes * 100.0));
    }
    
    /**
     * Connectivity statistics
     */
    public static class ConnectivityStats {
        private final boolean available;
        private final LocalDateTime lastCheck;
        private final LocalDateTime lastLoss;
        private final double uptimePercentage;
        
        public ConnectivityStats(boolean available, LocalDateTime lastCheck, 
                               LocalDateTime lastLoss, double uptimePercentage) {
            this.available = available;
            this.lastCheck = lastCheck;
            this.lastLoss = lastLoss;
            this.uptimePercentage = uptimePercentage;
        }
        
        public boolean isAvailable() { return available; }
        public LocalDateTime getLastCheck() { return lastCheck; }
        public LocalDateTime getLastLoss() { return lastLoss; }
        public double getUptimePercentage() { return uptimePercentage; }
    }
    
    /**
     * Test network latency to specific host
     */
    public long testLatency(String host, int port) {
        long startTime = System.currentTimeMillis();
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTIVITY_TIMEOUT);
            return System.currentTimeMillis() - startTime;
        } catch (IOException e) {
            logger.debug("Latency test to {}:{} failed: {}", host, port, e.getMessage());
            return -1; // Indicates failure
        }
    }
    
    /**
     * Get average latency to test hosts
     */
    public double getAverageLatency() {
        long totalLatency = 0;
        int successfulTests = 0;
        
        for (String host : TEST_HOSTS) {
            for (int port : TEST_PORTS) {
                long latency = testLatency(host, port);
                if (latency > 0) {
                    totalLatency += latency;
                    successfulTests++;
                }
            }
        }
        
        return successfulTests > 0 ? (double) totalLatency / successfulTests : -1;
    }
}