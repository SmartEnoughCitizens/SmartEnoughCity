package com.trinity.hermes.approval.repository;

import com.trinity.hermes.approval.entity.ApprovalRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

  List<ApprovalRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);

  List<ApprovalRequest> findByIndicatorOrderByCreatedAtDesc(String indicator);

  List<ApprovalRequest> findByIndicatorAndRequestedByOrderByCreatedAtDesc(
      String indicator, String requestedBy);

  List<ApprovalRequest> findAllByOrderByStatusAscCreatedAtDesc();
}
