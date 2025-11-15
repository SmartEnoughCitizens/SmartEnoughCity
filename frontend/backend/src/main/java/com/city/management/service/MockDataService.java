package com.city.management.service;

import com.city.management.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MockDataService {

    private final Map<Long, Simulation> simulations = new ConcurrentHashMap<>();
    private final Map<Long, Notification> notifications = new ConcurrentHashMap<>();
    private final AtomicLong simulationIdGenerator = new AtomicLong(1);
    private final AtomicLong notificationIdGenerator = new AtomicLong(1);
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public MockDataService() {
        initNotifications();
        initSimulations();
    }

    private void initNotifications() {
        createNotification("System Update", "System maintenance scheduled for tonight", "info", false);
        createNotification("Traffic Alert", "Heavy traffic detected on Main Street", "warning", false);
        createNotification("Simulation Complete", "Traffic simulation #123 has completed", "success", true);
    }

    private void initSimulations() {
        Map<String, Object> params = new HashMap<>();
        params.put("duration", 60);
        params.put("trafficDensity", "high");

        Map<String, Object> results = new HashMap<>();
        results.put("avgSpeed", 45.5);
        results.put("congestionLevel", 0.65);

        Simulation sim = new Simulation();
        sim.setId(1L);
        sim.setName("Morning Traffic Analysis");
        sim.setType("traffic");
        sim.setStatus("completed");
        sim.setParameters(params);
        sim.setResults(results);
        sim.setCreatedAt(LocalDateTime.now().minusHours(2));
        sim.setCompletedAt(LocalDateTime.now().minusHours(1));
        sim.setCreatedBy("admin");

        simulations.put(sim.getId(), sim);
    }

    private void createNotification(String title, String message, String type, boolean read) {
        Notification notification = new Notification();
        notification.setId(notificationIdGenerator.getAndIncrement());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUserId("admin");

        notifications.put(notification.getId(), notification);
    }

    public DashboardStats getDashboardStats() {
        List<DashboardStats.MetricData> metrics = Arrays.asList(
                new DashboardStats.MetricData(LocalDateTime.now().minusHours(2).format(formatter), "traffic", 75.5),
                new DashboardStats.MetricData(LocalDateTime.now().minusHours(1).format(formatter), "traffic", 82.3),
                new DashboardStats.MetricData(LocalDateTime.now().format(formatter), "traffic", 68.9)
        );

        return new DashboardStats(245, 12, 34, 98.5, metrics);
    }

    public DashboardOverview getDashboardOverview() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalVehicles", 15420);
        summary.put("avgSpeed", 45.2);
        summary.put("incidents", 8);
        summary.put("efficiency", 87.3);

        List<DashboardOverview.DataPoint> trafficData = Arrays.asList(
                new DashboardOverview.DataPoint("00:00", 120),
                new DashboardOverview.DataPoint("04:00", 80),
                new DashboardOverview.DataPoint("08:00", 450),
                new DashboardOverview.DataPoint("12:00", 380),
                new DashboardOverview.DataPoint("16:00", 520),
                new DashboardOverview.DataPoint("20:00", 290)
        );

        List<DashboardOverview.ChartData> charts = Arrays.asList(
                new DashboardOverview.ChartData("Traffic Volume", "line", trafficData)
        );

        List<DashboardOverview.Alert> alerts = Arrays.asList(
                new DashboardOverview.Alert("warning", "High traffic on Route 101", LocalDateTime.now().format(formatter)),
                new DashboardOverview.Alert("info", "Scheduled maintenance in Zone A", LocalDateTime.now().minusHours(1).format(formatter))
        );

        return new DashboardOverview(summary, charts, alerts);
    }

    public IndicatorData getIndicatorData(String mode) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("mode", mode);
        summary.put("totalCount", new Random().nextInt(1000) + 500);
        summary.put("avgSpeed", 30 + new Random().nextDouble() * 20);
        summary.put("efficiency", 70 + new Random().nextDouble() * 25);

        List<IndicatorData.TimeSeriesData> timeSeries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hour", i);
            metadata.put("conditions", "normal");

            timeSeries.add(new IndicatorData.TimeSeriesData(
                    LocalDateTime.now().minusHours(24 - i).format(formatter),
                    50 + new Random().nextDouble() * 50,
                    metadata
            ));
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("peakHour", "17:00");
        metrics.put("lowHour", "03:00");
        metrics.put("avgDailyCount", 12450);

        return new IndicatorData(mode, summary, timeSeries, metrics);
    }

    public List<Simulation> getAllSimulations() {
        return new ArrayList<>(simulations.values());
    }

    public Simulation runSimulation(Map<String, Object> params, String username) {
        Simulation simulation = new Simulation();
        simulation.setId(simulationIdGenerator.getAndIncrement());
        simulation.setName("Simulation " + simulation.getId());
        simulation.setType(params.getOrDefault("type", "traffic").toString());
        simulation.setStatus("running");
        simulation.setParameters(params);
        simulation.setCreatedAt(LocalDateTime.now());
        simulation.setCreatedBy(username);

        simulations.put(simulation.getId(), simulation);

        // Simulate completion after creation (in real app, this would be async)
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                simulation.setStatus("completed");
                simulation.setCompletedAt(LocalDateTime.now());

                Map<String, Object> results = new HashMap<>();
                results.put("avgSpeed", 40 + new Random().nextDouble() * 20);
                results.put("efficiency", 70 + new Random().nextDouble() * 25);
                results.put("incidents", new Random().nextInt(10));
                simulation.setResults(results);
            } catch (InterruptedException e) {
                simulation.setStatus("failed");
            }
        }).start();

        return simulation;
    }

    public boolean deleteSimulation(Long id) {
        return simulations.remove(id) != null;
    }

    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications.values());
    }

    public Optional<Notification> markNotificationAsRead(Long id) {
        Notification notification = notifications.get(id);
        if (notification != null) {
            notification.setRead(true);
            return Optional.of(notification);
        }
        return Optional.empty();
    }

    public void markAllNotificationsAsRead() {
        notifications.values().forEach(n -> n.setRead(true));
    }

    public SystemStatus getSystemStatus() {
        Map<String, SystemStatus.ServiceStatus> services = new HashMap<>();
        services.put("database", new SystemStatus.ServiceStatus("healthy", "Connected", 5L));
        services.put("cache", new SystemStatus.ServiceStatus("healthy", "Running", 2L));
        services.put("api", new SystemStatus.ServiceStatus("healthy", "Operational", 10L));
        services.put("monitoring", new SystemStatus.ServiceStatus("healthy", "Active", 15L));

        return new SystemStatus("healthy", "1.0.0", System.currentTimeMillis() / 1000, services);
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(formatter));
        health.put("uptime", System.currentTimeMillis() / 1000);

        Map<String, String> details = new HashMap<>();
        details.put("application", "Sustainable City Management Backend");
        details.put("version", "1.0.0");
        health.put("details", details);

        return health;
    }
}
