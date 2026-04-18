package com.trinity.hermes.approval.controller;

import com.trinity.hermes.approval.dto.ApprovalRequestDTO;
import com.trinity.hermes.approval.dto.CreateApprovalRequestDTO;
import com.trinity.hermes.approval.dto.ReviewApprovalRequestDTO;
import com.trinity.hermes.approval.service.ApprovalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ApprovalController {

  private final ApprovalService approvalService;

  /** Submit a new approval request. Any indicator admin can call this. */
  @PostMapping
  public ResponseEntity<ApprovalRequestDTO> create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody CreateApprovalRequestDTO dto) {
    String userId = jwt.getClaimAsString("preferred_username");
    return ResponseEntity.ok(approvalService.create(userId, dto));
  }

  /**
   * Submit multiple approval requests (one per recommendation) with a single summary email to City
   * Managers.
   */
  @PostMapping("/batch")
  public ResponseEntity<List<ApprovalRequestDTO>> createBatch(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody List<CreateApprovalRequestDTO> dtos) {
    String userId = jwt.getClaimAsString("preferred_username");
    return ResponseEntity.ok(approvalService.createBatch(userId, dtos));
  }

  /**
   * List approval requests — filtered by the caller's role automatically. Optional ?indicator=train
   * query param further narrows results.
   */
  @GetMapping
  public ResponseEntity<List<ApprovalRequestDTO>> list(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String indicator) {
    String userId = jwt.getClaimAsString("preferred_username");
    List<String> roles = extractRoles(jwt);
    return ResponseEntity.ok(approvalService.list(userId, roles, indicator));
  }

  /** Approve or deny a request. Only City_Manager / Government_Admin. */
  @PatchMapping("/{id}/review")
  public ResponseEntity<ApprovalRequestDTO> review(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @RequestBody ReviewApprovalRequestDTO dto) {
    String userId = jwt.getClaimAsString("preferred_username");
    List<String> roles = extractRoles(jwt);
    return ResponseEntity.ok(approvalService.review(id, userId, roles, dto));
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRoles(Jwt jwt) {
    try {
      java.util.Map<String, Object> realmAccess =
          (java.util.Map<String, Object>) jwt.getClaims().get("realm_access");
      if (realmAccess == null) return List.of();
      Object rolesObj = realmAccess.get("roles");
      if (rolesObj instanceof List<?> list) {
        return list.stream().map(Object::toString).toList();
      }
    } catch (Exception e) {
      log.debug("Could not extract roles from JWT: {}", e.getMessage());
    }
    return List.of();
  }
}
