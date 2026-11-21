package com.example.demo.transit.entity;

/**
 * A rider's saved place ("Home", "Work", ...), stored on the account. Either a station or a map point.
 *
 * @param kind HOME, WORK or OTHER
 */
public record SavedPlace(String id, String kind, String label, String name, String stationId, Double lat, Double lng) {
}
