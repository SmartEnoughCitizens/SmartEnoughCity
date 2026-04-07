package com.trinity.hermes.disruptionmanagement.dto;

/**
 * A nearby alternative transport option returned by AlternativeTransportRepository.findNearby().
 */
public record AlternativeTransportResult(
    String transportType, // "bus", "rail", "bike"
    String stopId,
    String stopName,
    Double lat,
    Double lon,
    Integer availableBikes, // null for bus/rail
    Integer capacity, // null for bus/rail
    Integer distanceM) {}
