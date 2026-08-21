package com.example.demo.transit.repository;

import com.example.demo.transit.domain.ReportType;
import com.example.demo.transit.entity.CrowdsourceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CrowdsourceReportRepository extends JpaRepository<CrowdsourceReport, Long> {

    List<CrowdsourceReport> findByRoute_IdInAndSentAtAfterOrderBySentAtDesc(
            Collection<Long> routeIds, Instant since);

    @Query("""
            SELECT COUNT(DISTINCT r.reporterId) FROM CrowdsourceReport r
            WHERE r.route.id = :routeId
              AND r.reportType = :type
              AND r.applied = false
              AND r.sentAt >= :since
            """)
    long countDistinctReporters(
            @Param("routeId") Long routeId,
            @Param("type") ReportType type,
            @Param("since") Instant since);

    List<CrowdsourceReport> findByRoute_IdAndReportTypeAndAppliedFalseAndSentAtGreaterThanEqual(
            Long routeId, ReportType type, Instant since);
}
