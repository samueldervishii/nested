package com.nested.server.repository;

import com.nested.server.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    Page<Report> findBySubIdAndStatus(String subId, Report.ReportStatus status, Pageable pageable);

    Page<Report> findBySubId(String subId, Pageable pageable);

    List<Report> findByTargetIdAndTargetType(String targetId, Report.TargetType targetType);

    List<Report> findByReporterId(String reporterId);

    long countBySubIdAndStatus(String subId, Report.ReportStatus status);

    boolean existsByReporterIdAndTargetIdAndTargetType(String reporterId, String targetId, Report.TargetType targetType);
}
