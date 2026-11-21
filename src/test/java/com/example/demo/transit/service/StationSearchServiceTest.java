package com.example.demo.transit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StationSearchServiceTest {

    @Test
    void normalisesArabicVariants() {
        assertEquals(StationSearchService.normalize("جسر الرئيس"), StationSearchService.normalize("جسر الرءيس"));
        assertEquals(StationSearchService.normalize("البرامكة"), StationSearchService.normalize("برامكه"));
        assertEquals("مزه فيلات", StationSearchService.normalize("  مَزّة  فيلات "));
        assertEquals(StationSearchService.normalize("أوتستراد"), StationSearchService.normalize("اوتستراد"));
        assertEquals("جسر ريس", StationSearchService.normalize("جسر الرئيس"));
    }

    @Test
    void reducesBothScriptsToTheSameSoundKey() {
        assertEquals(StationSearchService.soundWords("البرامكة"), StationSearchService.soundWords("Baramkeh"));
        assertEquals(StationSearchService.soundWords("القدم"), StationSearchService.soundWords("kadam"));
        assertEquals(StationSearchService.soundWords("المزة"), StationSearchService.soundWords("Mezzeh"));
        assertEquals(StationSearchService.soundWords("باب توما"), StationSearchService.soundWords("Bab Touma"));
        assertEquals(StationSearchService.soundWords("ساحة المرجة"), StationSearchService.soundWords("Sahat al-Marjeh"));
        assertEquals(StationSearchService.soundWords("العباسيين"), StationSearchService.soundWords("Abbasiyyin"));
        assertNotEquals(StationSearchService.soundWords("الحميدية"), StationSearchService.soundWords("Ahmadiyeh"));
    }
}
