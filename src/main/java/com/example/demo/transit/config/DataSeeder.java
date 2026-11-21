package com.example.demo.transit.config;

import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.domain.Role;
import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.domain.StationKind;
import com.example.demo.transit.domain.VehicleType;
import com.example.demo.transit.dto.GeoPointDto;
import com.example.demo.transit.entity.CrowdsourceReport;
import com.example.demo.transit.entity.Route;
import com.example.demo.transit.entity.RouteStop;
import com.example.demo.transit.entity.Station;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.CrowdsourceReportRepository;
import com.example.demo.transit.repository.RouteRepository;
import com.example.demo.transit.repository.StationRepository;
import com.example.demo.transit.repository.UserRepository;
import com.example.demo.transit.dto.NetworkImportRequest;
import com.example.demo.transit.dto.NetworkImportResult;
import com.example.demo.transit.service.NetworkImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a small Damascus / Rif Dimashq network on first start (empty database).
 * Disable with app.seed.enabled=false.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final CrowdsourceReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransitGraphInvalidator invalidator;
    private final NetworkImportService importService;
    private final com.example.demo.transit.repository.AppMetaRepository metaRepository;
    private final ObjectMapper objectMapper;
    private final String dataset;
    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String userPassword;

    public DataSeeder(StationRepository stationRepository,
                      RouteRepository routeRepository,
                      UserRepository userRepository,
                      CrowdsourceReportRepository reportRepository,
                      PasswordEncoder passwordEncoder,
                      TransitGraphInvalidator invalidator,
                      NetworkImportService importService,
                      com.example.demo.transit.repository.AppMetaRepository metaRepository,
                      ObjectMapper objectMapper,
                      @Value("${app.seed.dataset:damascus}") String dataset,
                      @Value("${app.seed.enabled:true}") boolean enabled,
                      @Value("${app.seed.admin-email:admin@demo.com}") String adminEmail,
                      @Value("${app.seed.admin-password:}") String adminPassword,
                      @Value("${app.seed.user-password:}") String userPassword) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidator = invalidator;
        this.importService = importService;
        this.metaRepository = metaRepository;
        this.objectMapper = objectMapper;
        this.dataset = dataset;
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        // Always make sure an admin account exists (password must come from the environment).
        if (!userRepository.existsByEmail(adminEmail.toLowerCase())) {
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("SEED_ADMIN_PASSWORD is required to create the first admin account (or set SEED_ENABLED=false)");
            }
            createUser("Admin User", adminEmail, adminPassword, Role.ROLE_ADMIN);
            log.info("Seeded admin account {}", adminEmail);
        }
        if (stationRepository.count() > 0) {
            if ("damascus".equalsIgnoreCase(dataset)) {
                syncDataset();
            }
            invalidator.warmUp();
            return;
        }
        if ("damascus".equalsIgnoreCase(dataset)) {
            seedDamascusDataset();
            return;
        }
        log.info("Empty database: seeding demo network");
        boolean demoUsers = userPassword != null && !userPassword.isBlank();
        User user1 = demoUsers ? createUser("Ahmad Ali", "ahmad@demo.com", userPassword, Role.ROLE_USER) : null;
        User user2 = demoUsers ? createUser("Samer Omar", "samer@demo.com", userPassword, Role.ROLE_USER) : null;

        // Hubs
        Station sPresidentBridge = station("جسر الرئيس", "الرئيس، تحت الجسر، الحافظ", StationKind.TRANSFER_HUB, "DAMASCUS", 33.5110, 36.2890);
        Station sBaramkeh = station("البرامكة", "كراجات البرامكة، سانا، كلية الحقوق", StationKind.HUB, "DAMASCUS", 33.5042, 36.2885);
        Station sSomaria = station("كراجات السومرية", "السومرية، مركز انطلاق الريف الجنوبي", StationKind.TERMINAL, "RIF_DIMASHQ", 33.4752, 36.2110);

        // Damascus & Rif Dimashq stops
        Station sMezzehSheikhSaad = station("المزة الشيخ سعد", "الشيخ سعد، المزة جبل", StationKind.STOP, "DAMASCUS", 33.5020, 36.2621);
        Station sMezzehAutostrad = station("المزة اوتستراد", "مشفى المواساة، الجلاء", StationKind.STOP, "DAMASCUS", 33.5000, 36.2700);
        Station sMezzehVillas = station("مزة فيلات", "مزة فيلات غربية، الفيلات", StationKind.STOP, "DAMASCUS", 33.5068, 36.2510);
        Station sDummarIsland1 = station("مشروع دمر - الجزيرة 1", "دمر البلد، الجزيرة الأولى", StationKind.STOP, "DAMASCUS", 33.5350, 36.2300);
        Station sDummarUp = station("مشروع دمر - التوسع", "دمر التوسع، الجزر الأخيرة", StationKind.STOP, "DAMASCUS", 33.5420, 36.2210);
        Station sMuhajireen = station("المهاجرين", "ساحة المهاجرين، جادة المهاجرين", StationKind.STOP, "DAMASCUS", 33.5230, 36.2880);
        Station sQatanaCenter = station("قطنا المركز", "مركز قطنا، الموقف العام", StationKind.TERMINAL, "RIF_DIMASHQ", 33.4361, 36.0825);
        Station sMuadamiyat = station("المعضمية", "مفرق المعضمية، الشارع العام", StationKind.STOP, "RIF_DIMASHQ", 33.4580, 36.1850);

        route("مزة جبل - جسر الرئيس", "R-MEZ-01", VehicleType.SERVIS, "DAMASCUS", "1000.00",
                List.of(sMezzehSheikhSaad, sBaramkeh, sPresidentBridge));
        route("مزة فيلات - جسر الرئيس", "R-MEZ-02", VehicleType.SERVIS, "DAMASCUS", "1000.00",
                List.of(sMezzehVillas, sMezzehAutostrad, sBaramkeh, sPresidentBridge));
        route("مشروع دمر - جسر الرئيس", "R-DUM-01", VehicleType.BUS, "DAMASCUS", "1500.00",
                List.of(sDummarUp, sDummarIsland1, sPresidentBridge));
        route("مشروع دمر - البرامكة", "R-DUM-02", VehicleType.SERVIS, "DAMASCUS", "1200.00",
                List.of(sDummarIsland1, sMezzehAutostrad, sBaramkeh));
        Route r4 = route("قطنا - السومرية", "R-QAT-01", VehicleType.SERVIS, "RIF_DIMASHQ", "2000.00",
                List.of(sQatanaCenter, sMuadamiyat, sSomaria));
        route("السومرية - البرامكة - جسر الرئيس", "R-SOM-01", VehicleType.BUS, "DAMASCUS", "1500.00",
                List.of(sSomaria, sMezzehAutostrad, sBaramkeh, sPresidentBridge));
        Route r1 = routeRepository.findByCode("R-MEZ-01").orElseThrow();
        route("جسر الرئيس - المهاجرين", "R-MUH-01", VehicleType.SERVIS, "DAMASCUS", "800.00",
                List.of(sPresidentBridge, sMuhajireen));

        if (demoUsers) {
            report(ReportType.CROWDING, user1.getEmail(), r1, sBaramkeh, null, "ازدحام شديد عند موقف البرامكة");
            report(ReportType.FARE_CHANGE, user2.getEmail(), r4, sQatanaCenter, new BigDecimal("2500.00"), "تقاضي أجرة زائدة 2500 ل.س");
        }

        invalidator.invalidate();
    }

    /** Real Damascus / Rif Dimashq network compiled from public sources (see docs/data/README.md). */
    private void seedDamascusDataset() {
        try (InputStream in = new ClassPathResource("data/damascus-network.json").getInputStream()) {
            NetworkImportRequest request = objectMapper.readValue(in, NetworkImportRequest.class);
            NetworkImportResult result = importService.importNetwork(request);
            log.info("Empty database: imported Damascus dataset ({} stations, {} routes, {} warnings)",
                    result.stationsCreated(), result.routesCreated(), result.warnings().size());
            result.warnings().forEach(w -> log.warn("Dataset warning: {}", w));
            rememberDataset(datasetHash());
            invalidator.warmUp();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read bundled Damascus dataset", e);
        }
    }

    private User createUser(String name, String email, String password, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Station station(String name, String aliases, StationKind kind, String gov, double lat, double lng) {
        Station station = new Station();
        station.setName(name);
        station.setAliases(aliases);
        station.setStationKind(kind);
        station.setGovernorate(gov);
        station.setLatitude(lat);
        station.setLongitude(lng);
        return stationRepository.save(station);
    }

    private Route route(String name, String code, VehicleType vehicleType, String gov, String fare, List<Station> stations) {
        Route route = new Route();
        route.setName(name);
        route.setCode(code);
        route.setVehicleType(vehicleType);
        route.setGovernorate(gov);
        route.setCurrentFare(new BigDecimal(fare));
        route.setStatus(RouteStatus.NORMAL);
        List<RouteStop> stops = new ArrayList<>();
        List<GeoPointDto> polyline = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            stops.add(new RouteStop(stations.get(i).getId(), i + 1));
            polyline.add(new GeoPointDto(stations.get(i).getLatitude(), stations.get(i).getLongitude()));
        }
        route.setStops(stops);
        route.setPolyline(polyline);
        return routeRepository.save(route);
    }

    private void report(ReportType type, String reporter, Route route, Station station, BigDecimal newFare, String desc) {
        CrowdsourceReport report = new CrowdsourceReport();
        report.setReportType(type);
        report.setReporterId(reporter);
        report.setRouteId(route.getId());
        report.setStationId(station.getId());
        report.setNewFare(newFare);
        report.setDescription(desc);
        report.setSentAt(Instant.now());
        report.setLatitude(station.getLatitude());
        report.setLongitude(station.getLongitude());
        reportRepository.save(report);
    }

    private static final String DATASET_META = "dataset-sha256";

    /**
     * When the bundled dataset changes (new places, English names), brings an existing database up to date
     * without touching anything an admin may have edited: stations missing from the database are added and
     * blank English names are filled in. Lines, fares and statuses are never changed here.
     */
    void syncDataset() {
        String hash = datasetHash();
        if (hash == null || metaRepository.findById(DATASET_META).map(m -> hash.equals(m.getValue())).orElse(false)) {
            return;
        }
        NetworkImportRequest request;
        try (InputStream in = new ClassPathResource("data/damascus-network.json").getInputStream()) {
            request = objectMapper.readValue(in, NetworkImportRequest.class);
        } catch (java.io.IOException e) {
            log.warn("Dataset sync skipped: {}", e.getMessage());
            return;
        }
        java.util.Map<String, Station> existing = new java.util.HashMap<>();
        for (Station st : stationRepository.findAll()) {
            existing.putIfAbsent(syncKey(st.getName(), st.getGovernorate()), st);
        }
        java.util.List<Station> changed = new java.util.ArrayList<>();
        int added = 0, named = 0;
        for (NetworkImportRequest.StationItem item : request.stations() == null ? java.util.List.<NetworkImportRequest.StationItem>of() : request.stations()) {
            String gov = item.governorate() == null || item.governorate().isBlank() ? "DAMASCUS" : item.governorate();
            Station current = existing.get(syncKey(item.name(), gov));
            if (current == null) {
                Station st = new Station();
                st.setName(item.name());
                st.setNameEn(item.nameEn());
                st.setAliases(item.aliases());
                st.setStationKind(item.stationKind() != null ? item.stationKind() : StationKind.POINT);
                st.setGovernorate(gov);
                st.setLatitude(item.lat());
                st.setLongitude(item.lng());
                changed.add(st);
                existing.put(syncKey(item.name(), gov), st);
                added++;
            } else if ((current.getNameEn() == null || current.getNameEn().isBlank())
                    && item.nameEn() != null && !item.nameEn().isBlank()) {
                current.setNameEn(item.nameEn());
                changed.add(current);
                named++;
            }
        }
        if (!changed.isEmpty()) {
            stationRepository.saveAll(changed);
            invalidator.invalidate();
        }
        rememberDataset(hash);
        log.info("Dataset sync: {} places added, {} English names filled", added, named);
    }

    private static String syncKey(String name, String governorate) {
        return com.example.demo.transit.service.StationSearchService.normalize(name) + "|"
                + (governorate == null ? "DAMASCUS" : governorate.toUpperCase(java.util.Locale.ROOT));
    }

    private String datasetHash() {
        try (InputStream in = new ClassPathResource("data/damascus-network.json").getInputStream()) {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(in.readAllBytes());
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return null;
        }
    }

    private void rememberDataset(String hash) {
        if (hash == null) {
            return;
        }
        com.example.demo.transit.entity.AppMeta meta = new com.example.demo.transit.entity.AppMeta();
        meta.setId(DATASET_META);
        meta.setValue(hash);
        meta.setUpdatedAt(java.time.Instant.now());
        metaRepository.save(meta);
    }
}
