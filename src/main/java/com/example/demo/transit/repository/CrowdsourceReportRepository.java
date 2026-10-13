package com.example.demo.transit.repository;

import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.entity.CrowdsourceReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CrowdsourceReportRepository extends MongoRepository<CrowdsourceReport, String> {

    List<CrowdsourceReport> findByRouteIdInAndSentAtAfterAndDismissedFalseOrderBySentAtDesc(
            Collection<String> routeIds, Instant since);

    List<CrowdsourceReport> findByRouteIdAndSentAtAfterAndDismissedFalseOrderBySentAtDesc(
            String routeId, Instant since);

    List<CrowdsourceReport> findByRouteIdAndReportTypeAndAppliedFalseAndDismissedFalseAndSentAtGreaterThanEqual(
            String routeId, ReportType type, Instant since);

    List<CrowdsourceReport> findBySentAtAfterOrderBySentAtDesc(Instant since);
}
