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
                      @Value("${app.seed.enabled:true}") boolean enabled,
                      @Value("${app.seed.admin-email:admin@demo.com}") String adminEmail,
                      @Value("${app.seed.admin-password:Admin#12345}") String adminPassword,
                      @Value("${app.seed.user-password:User#12345}") String userPassword) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidator = invalidator;
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
        // Always make sure an admin account exists.
        if (!userRepository.existsByEmail(adminEmail.toLowerCase())) {
            createUser("Admin User", adminEmail, adminPassword, Role.ROLE_ADMIN);
            log.info("Seeded admin account {}", adminEmail);
        }
        if (stationRepository.count() > 0) {
            return;
        }
        log.info("Empty database: seeding demo network");

        User user1 = createUser("Ahmad Ali", "ahmad@demo.com", userPassword, Role.ROLE_USER);
        User user2 = createUser("Samer Omar", "samer@demo.com", userPassword, Role.ROLE_USER);

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

        report(ReportType.CROWDING, user1.getEmail(), r1, sBaramkeh, null, "ازدحام شديد عند موقف البرامكة");
        report(ReportType.FARE_CHANGE, user2.getEmail(), r4, sQatanaCenter, new BigDecimal("2500.00"), "تقاضي أجرة زائدة 2500 ل.س");

        invalidator.invalidate();
    }

    private User createUser(String name, String email, String password, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
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
}
