package com.example.demo.transit.service;

import com.example.demo.transit.entity.Station;
import com.example.demo.transit.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * FR-1.1 smart text search over station names and local aliases.
 * Arabic text is normalised (hamza forms, taa marbuta, alef maqsura, tashkeel, "ال") so that
 * "جسر الرئيس", "جسر الرءيس" and "الجسر" all match.
 */
@Service
public class StationSearchService {

    private final StationRepository stationRepository;

    public StationSearchService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<Station> search(String query, String governorate) {
        List<Station> all = (governorate == null || governorate.isBlank())
                ? stationRepository.findAll()
                : stationRepository.findByGovernorateIgnoreCase(governorate.trim());
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = normalize(query);
        List<Scored> scored = new ArrayList<>();
        for (Station station : all) {
            int score = score(station, q);
            if (score > 0) {
                scored.add(new Scored(station, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(s -> s.station().getName()));
        return scored.stream().map(Scored::station).toList();
    }

    public Station best(String query, String governorate) {
        List<Station> matches = search(query, governorate);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private int score(Station station, String q) {
        int best = 0;
        best = Math.max(best, scoreTerm(normalize(station.getName()), q, 100));
        if (station.getAliases() != null) {
            for (String alias : station.getAliases().split("[،,;/]")) {
                best = Math.max(best, scoreTerm(normalize(alias), q, 80));
            }
        }
        return best;
    }

    private static int scoreTerm(String term, String q, int base) {
        if (term.isEmpty()) {
            return 0;
        }
        if (term.equals(q)) {
            return base;
        }
        if (term.startsWith(q)) {
            return base - 10;
        }
        if (term.contains(q)) {
            return base - 20;
        }
        // all query words contained (any order)
        String[] words = q.split(" ");
        if (words.length > 1) {
            boolean all = true;
            for (String w : words) {
                if (!w.isEmpty() && !term.contains(w)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return base - 30;
            }
        }
        return 0;
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[\\u064B-\\u0652\\u0670\\u0640]", "");   // tashkeel + tatweel
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا');
        s = s.replace('ة', 'ه').replace('ى', 'ي').replace('ی', 'ي');
        s = s.replaceAll("[ءئؤ]", "");                               // hamza variants
        s = s.replaceAll("(^|\\s)ال", "$1");                         // definite article
        s = s.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    private record Scored(Station station, int score) {
    }
}
