package com.trinity.hermes.buses.controller;

import com.trinity.hermes.buses.services.RecommendationSender;
import com.trinity.hermes.database.PostgresConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@RestController
public class BusController {

    @Autowired
    private PostgresConnection pgConnection;

    RecommendationSender recommendationSender = new RecommendationSender();

    @GetMapping("/buses")
    public String getBusesData() {

        try (Connection conn = pgConnection.getConnection()) {
            recommendationSender.sendAsync("bus data");
            return "bus data";
        }
        catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "Error";
    }

    @GetMapping("/data_for_recommendation")
    public String busDataForRecommendation() {

        try (Connection conn = pgConnection.getConnection()) {
            return "bus data needed for recommendation";
        }
        catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "Error";
    }
}