package com.example.demo.transit.service;

import com.example.demo.transit.entity.Station;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * FR-1.1 smart text search over station names and local aliases.
 * Arabic text is normalised (hamza forms, taa marbuta, alef maqsura, tashkeel, "ال") so that
 * "جسر الرئيس", "جسر الرءيس" and "الجسر" all match. Latin queries also match English names and,
 * by sound, Arabic names ("Baramkeh" → البرامكة, "Kadam" → القدم); the rider app's
 * utils/text.js uses the same rules offline.
 */
@Service
public class StationSearchService {

    private final TransitGraphFactory graphFactory;

    public StationSearchService(TransitGraphFactory graphFactory) {
        this.graphFactory = graphFactory;
    }

    public List<Station> search(String query, String governorate) {
        List<Station> stations = graphFactory.snapshot().stations();
        List<Station> all = (governorate == null || governorate.isBlank())
                ? stations
                : stations.stream().filter(s -> governorate.trim().equalsIgnoreCase(s.getGovernorate())).toList();
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = normalize(query);
        boolean latin = q.chars().anyMatch(c -> c >= 'a' && c <= 'z');
        List<String> qWords = latin ? soundWords(query) : List.of();
        String qKey = String.join("", qWords);
        List<Scored> scored = new ArrayList<>();
        for (Station station : all) {
            int score = score(station, q, qWords, qKey);
            if (score > 0) {
                scored.add(new Scored(station, score));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparingInt(s -> s.station().getName().length())
                .thenComparing(s -> s.station().getName()));
        return scored.stream().map(Scored::station).toList();
    }

    public Station best(String query, String governorate) {
        List<Station> matches = search(query, governorate);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private int score(Station station, String q, List<String> qWords, String qKey) {
        int best = 0;
        best = Math.max(best, scoreText(station.getName(), q, qWords, qKey, 100));
        best = Math.max(best, scoreText(station.getNameEn(), q, qWords, qKey, 100));
        if (station.getAliases() != null) {
            for (String alias : station.getAliases().split("[،,;/]")) {
                best = Math.max(best, scoreText(alias, q, qWords, qKey, 80));
            }
        }
        return best;
    }

    private static int scoreText(String text, String q, List<String> qWords, String qKey, int base) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int best = scoreTerm(normalize(text), q, base);
        if (qKey.length() >= 2) {
            best = Math.max(best, soundScore(soundWords(text), qWords, qKey, base));
        }
        return best;
    }

    private static int soundScore(List<String> words, List<String> qWords, String qKey, int base) {
        String key = String.join("", words);
        if (key.isEmpty()) {
            return 0;
        }
        int cut = base == 100 ? 30 : 40;
        if (key.equals(qKey)) {
            return base - cut;
        }
        if (words.containsAll(qWords)) {
            return base - cut - 3;
        }
        if (key.startsWith(qKey)) {
            return base - cut - 5;
        }
        if (qWords.stream().allMatch(w -> words.stream().anyMatch(tw -> tw.startsWith(w)))) {
            return base - cut - 10;
        }
        if (qKey.length() >= 4 && key.contains(qKey)) {
            return base - cut - 20;
        }
        return 0;
    }

    private static final Map<Character, String> AR_SOUNDS = Map.ofEntries(
            Map.entry('ب', "b"), Map.entry('پ', "b"), Map.entry('ت', "t"), Map.entry('ط', "t"),
            Map.entry('ث', "s"), Map.entry('س', "s"), Map.entry('ص', "s"), Map.entry('ش', "s"),
            Map.entry('ج', "j"), Map.entry('چ', "j"), Map.entry('ح', "h"), Map.entry('ه', "h"),
            Map.entry('خ', "k"), Map.entry('ق', "k"), Map.entry('ك', "k"), Map.entry('د', "d"),
            Map.entry('ض', "d"), Map.entry('ذ', "z"), Map.entry('ز', "z"), Map.entry('ظ', "z"),
            Map.entry('ر', "r"), Map.entry('غ', "g"), Map.entry('گ', "g"), Map.entry('ف', "f"),
            Map.entry('ڤ', "f"), Map.entry('ل', "l"), Map.entry('م', "m"), Map.entry('ن', "n"));
    private static final Pattern ARABIC = Pattern.compile("\\p{IsArabic}");
    private static final Pattern LATIN_ARTICLE = Pattern.compile("(^|\\s)(al|el|a[dnrstz]|ash)-");

    /** Consonant skeleton of each word, in Arabic or Latin script (see the class comment). */
    public static List<String> soundWords(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        if (ARABIC.matcher(text).find()) {
            for (String w : normalize(text).split(" ")) {
                StringBuilder b = new StringBuilder();
                String core = w.endsWith("ه") ? w.substring(0, w.length() - 1) : w;
                for (char c : core.toCharArray()) {
                    b.append(AR_SOUNDS.getOrDefault(c, ""));
                }
                String word = (w.startsWith("ا") || w.startsWith("ع") ? "'" : "") + collapse(b.toString());
                if (!word.isEmpty() && !word.equals("'")) {
                    out.add(word);
                }
            }
            return out;
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("['’`ʿʾ]", "");
        s = LATIN_ARTICLE.matcher(s).replaceAll("$1")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("([^aeiou\\s][ae])t(?= )", "$1")
                .trim();
        for (String w : s.split(" ")) {
            if (w.isEmpty() || w.equals("al") || w.equals("el")) {
                continue;
            }
            String c = w.replaceAll("sh|ch|th", "s").replace("dh", "z").replace("kh", "k").replace("gh", "g")
                    .replace("ph", "f").replaceAll("h$", "")
                    .replaceAll("[qc]", "k").replace("x", "ks").replace("v", "f").replace("p", "b")
                    .replaceAll("[aeiouyw]", "");
            String word = (w.matches("^[aeiou].*") ? "'" : "") + collapse(c);
            if (!word.isEmpty() && !word.equals("'")) {
                out.add(word);
            }
        }
        return out;
    }

    private static String collapse(String key) {
        return key.replaceAll("(.)\\1+", "$1");
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
