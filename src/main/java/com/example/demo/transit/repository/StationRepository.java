package com.example.demo.transit.repository;

import com.example.demo.transit.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StationRepository extends JpaRepository<Station, Long> {

    @Query("""
            SELECT s FROM Station s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(s.aliases) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<Station> searchByNameOrAlias(@Param("q") String query);

    List<Station> findByGovernorateIgnoreCase(String governorate);
}
