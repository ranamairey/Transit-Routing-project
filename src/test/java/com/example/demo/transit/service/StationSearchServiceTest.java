package com.example.demo.transit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationSearchServiceTest {

    @Test
    void normalisesArabicVariants() {
        assertEquals(StationSearchService.normalize("جسر الرئيس"), StationSearchService.normalize("جسر الرءيس"));
        assertEquals(StationSearchService.normalize("البرامكة"), StationSearchService.normalize("برامكه"));
        assertEquals("مزه فيلات", StationSearchService.normalize("  مَزّة  فيلات "));
        assertEquals(StationSearchService.normalize("أوتستراد"), StationSearchService.normalize("اوتستراد"));
        assertEquals("جسر ريس", StationSearchService.normalize("جسر الرئيس"));
    }
}
