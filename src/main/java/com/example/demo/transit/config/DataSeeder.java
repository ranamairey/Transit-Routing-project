package com.example.demo.transit.config;

import com.example.demo.transit.domain.*;
import com.example.demo.transit.entity.*;
import com.example.demo.transit.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final CrowdsourceReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            StationRepository stationRepository,
            RouteRepository routeRepository,
            UserRepository userRepository,
            CrowdsourceReportRepository reportRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (stationRepository.count() > 0) {
            return;
        }

        // 1. المستخدمون
        User admin = createUser("Admin User", "admin@demo.com", "Admin#12345", Role.ROLE_ADMIN);
        User user1 = createUser("Ahmad Ali", "ahmad@demo.com", "User#12345", Role.ROLE_USER);
        User user2 = createUser("Samer Omar", "samer@demo.com", "User#12345", Role.ROLE_USER);

        // 2. عقد التبديل والمحطات المتصلة جغرافيتاً (تم ضبط الإحداثيات لتسمح بالتبديل)
        // Hubs الرئيسية
        Station sPresidentBridge = createStation("جسر الرئيس", "الرئيس، تحت الجسر، الحافظ", StationKind.TRANSFER_HUB, "DAMASCUS", 33.5110, 36.2890);
        Station sBaramkeh = createStation("البرامكة", "كراجات البرامكة، سانا، كلية الحقوق", StationKind.HUB, "DAMASCUS", 33.5042, 36.2885);
        Station sSomaria = createStation("كراجات السومرية", "السومرية، مركز انطلاق الريف الجنوبي", StationKind.TERMINAL, "RIF_DIMASHQ", 33.4752, 36.2110);

        // محطات الدمشقية والريف
        Station sMezzehSheikhSaad = createStation("المزة الشيخ سعد", "الشيخ سعد، المزة جبل", StationKind.STOP, "DAMASCUS", 33.5020, 36.2621);
        Station sMezzehAutostrad = createStation("المزة اوتستراد", "مشفى المواساة، الجلاء", StationKind.STOP, "DAMASCUS", 33.5000, 36.2700);
        Station sDummarIsland1 = createStation("مشروع دمر - الجزيرة 1", "دمر البلد، الجزيرة الأولى", StationKind.STOP, "DAMASCUS", 33.5350, 36.2300);
        Station sDummarUp = createStation("مشروع دمر - التوسع", "دمر التوسع، الجزر الأخيرة", StationKind.STOP, "DAMASCUS", 33.5420, 36.2210);
        Station sQatanaCenter = createStation("قطنا المركز", "مركز قطنا، الموقف العام", StationKind.TERMINAL, "RIF_DIMASHQ", 33.4361, 36.0825);
        Station sMuadamiyat = createStation("المعضمية", "مفرق المعضمية، الشارع العام", StationKind.STOP, "RIF_DIMASHQ", 33.4580, 36.1850);

        // 3. إنشاء خطوط النقل المتقاطعة لتوفير خيارات متعددة

        // خط 1: مزة جبل - البرامكة - جسر الرئيس (مباشر)
        Route r1 = createRoute("مزة جبل - جسر الرئيس", "R-MEZ-01", VehicleType.SERVIS, "DAMASCUS", new BigDecimal("1000.00"));
        addStopsToRoute(r1, List.of(sMezzehSheikhSaad, sBaramkeh, sPresidentBridge));
        routeRepository.save(r1);

        // خط 2: مشروع دمر - جسر الرئيس (مباشر عبر الاوتستراد)
        Route r2 = createRoute("مشروع دمر - جسر الرئيس", "R-DUM-01", VehicleType.BUS, "DAMASCUS", new BigDecimal("1500.00"));
        addStopsToRoute(r2, List.of(sDummarUp, sDummarIsland1, sPresidentBridge));
        routeRepository.save(r2);

        // خط 3: مشروع دمر - البرامكة (خط تبديلي للوصول للجسر)
        Route r3 = createRoute("مشروع دمر - البرامكة", "R-DUM-02", VehicleType.SERVIS, "DAMASCUS", new BigDecimal("1200.00"));
        addStopsToRoute(r3, List.of(sDummarIsland1, sMezzehAutostrad, sBaramkeh));
        routeRepository.save(r3);

        // خط 4: قطنا - السومرية
        Route r4 = createRoute("قطنا - السومرية", "R-QAT-01", VehicleType.SERVIS, "RIF_DIMASHQ", new BigDecimal("2000.00"));
        addStopsToRoute(r4, List.of(sQatanaCenter, sMuadamiyat, sSomaria));
        routeRepository.save(r4);

        // خط 5: السومرية - البرامكة - جسر الرئيس (خط يربط الريف بمركز المدينة للتبديل)
        Route r5 = createRoute("السومرية - البرامكة - جسر الرئيس", "R-SOM-01", VehicleType.BUS, "DAMASCUS", new BigDecimal("1500.00"));
        addStopsToRoute(r5, List.of(sSomaria, sMezzehAutostrad, sBaramkeh, sPresidentBridge));
        routeRepository.save(r5);

        // 4. بلاغات تجريبية
        createReport(ReportType.CROWDING, user1.getEmail(), r1, sBaramkeh, null, "ازدحام شديد عند موقف البرامكة");
        createReport(ReportType.FARE_CHANGE, user2.getEmail(), r4, sQatanaCenter, new BigDecimal("2500.00"), "تقاضي أجرة زائدة 2500 ل.س");
    }

    private User createUser(String name, String email, String password, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Station createStation(String name, String aliases, StationKind kind, String gov, double lat, double lng) {
        Station station = new Station();
        station.setName(name);
        station.setAliases(aliases);
        station.setStationKind(kind);
        station.setGovernorate(gov);
        station.setLatitude(lat);
        station.setLongitude(lng);
        return stationRepository.save(station);
    }

    private Route createRoute(String name, String code, VehicleType vehicleType, String gov, BigDecimal fare) {
        Route route = new Route();
        route.setName(name);
        route.setCode(code);
        route.setVehicleType(vehicleType);
        route.setGovernorate(gov);
        route.setCurrentFare(fare);
        route.setStatus(RouteStatus.NORMAL);
        return route;
    }

    private void addStopsToRoute(Route route, List<Station> stations) {
        for (int i = 0; i < stations.size(); i++) {
            RouteStation rs = new RouteStation();
            rs.setRoute(route);
            rs.setStation(stations.get(i));
            rs.setSequenceNo(i + 1);
            route.getStops().add(rs);
        }
    }

    private void createReport(ReportType type, String reporterEmail, Route route, Station station, BigDecimal newFare, String desc) {
        CrowdsourceReport report = new CrowdsourceReport();
        report.setReportType(type);
        report.setReporterId(reporterEmail);
        report.setRoute(route);
        report.setStation(station);
        report.setNewFare(newFare);
        report.setDescription(desc);
        report.setSentAt(Instant.now());
        report.setApplied(false);
        if (station != null) {
            report.setLatitude(station.getLatitude());
            report.setLongitude(station.getLongitude());
        }
        reportRepository.save(report);
    }
}