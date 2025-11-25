package com.trinity.hermes.dataanalyzer.config;

import com.trinity.hermes.dataanalyzer.model.Indicator;
import com.trinity.hermes.dataanalyzer.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Initialize database with sample data for testing
 * Comment out @Component annotation in production
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final IndicatorRepository indicatorRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        if (indicatorRepository.count() == 0) {
            log.info("Initializing database with sample data...");

            List<Indicator> indicators = new ArrayList<>();

            // Generate sample bus data
            indicators.addAll(generateBusData());

            // Generate sample car data
            indicators.addAll(generateCarData());

            indicatorRepository.saveAll(indicators);

            log.info("Sample data initialization completed. Total records: {}", indicators.size());
        } else {
            log.info("Database already contains data. Skipping initialization.");
        }
    }

    private List<Indicator> generateBusData() {
        List<Indicator> busData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 20; i++) {
            Indicator indicator = new Indicator();
            indicator.setIndicatorType("bus");
            indicator.setMetricName("passenger_count");
            indicator.setValue(50.0 + random.nextDouble() * 100);
            indicator.setUnit("passengers");
            indicator.setLocation("Route-" + (1 + random.nextInt(5)));
            indicator.setTimestamp(now.minusHours(i));
            indicator.setMetadata("{\"route_id\": \"" + (1 + random.nextInt(5)) + "\", \"vehicle_id\": \"BUS-" + (100 + random.nextInt(50)) + "\"}");
            busData.add(indicator);

            // Add occupancy rate
            Indicator occupancy = new Indicator();
            occupancy.setIndicatorType("bus");
            occupancy.setMetricName("occupancy_rate");
            occupancy.setValue(40.0 + random.nextDouble() * 50);
            occupancy.setUnit("percentage");
            occupancy.setLocation("Route-" + (1 + random.nextInt(5)));
            occupancy.setTimestamp(now.minusHours(i));
            occupancy.setMetadata("{\"route_id\": \"" + (1 + random.nextInt(5)) + "\"}");
            busData.add(occupancy);
        }

        return busData;
    }

    private List<Indicator> generateCarData() {
        List<Indicator> carData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 20; i++) {
            Indicator indicator = new Indicator();
            indicator.setIndicatorType("car");
            indicator.setMetricName("traffic_volume");
            indicator.setValue(100.0 + random.nextDouble() * 200);
            indicator.setUnit("vehicles_per_hour");
            indicator.setLocation("Junction-" + (1 + random.nextInt(3)));
            indicator.setTimestamp(now.minusHours(i));
            indicator.setMetadata("{\"junction_id\": \"J-" + (1 + random.nextInt(3)) + "\", \"direction\": \"north\"}");
            carData.add(indicator);

            // Add average speed
            Indicator speed = new Indicator();
            speed.setIndicatorType("car");
            speed.setMetricName("average_speed");
            speed.setValue(30.0 + random.nextDouble() * 40);
            speed.setUnit("km/h");
            speed.setLocation("Junction-" + (1 + random.nextInt(3)));
            speed.setTimestamp(now.minusHours(i));
            speed.setMetadata("{\"junction_id\": \"J-" + (1 + random.nextInt(3)) + "\"}");
            carData.add(speed);
        }

        return carData;
    }
}