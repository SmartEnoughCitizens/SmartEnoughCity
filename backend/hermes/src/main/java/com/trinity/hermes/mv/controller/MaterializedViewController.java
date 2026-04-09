package com.trinity.hermes.mv.controller;

import com.trinity.hermes.common.logging.LogSanitizer;
import com.trinity.hermes.mv.dto.MvRefreshResult;
import com.trinity.hermes.mv.dto.MvRegistryDTO;
import com.trinity.hermes.mv.dto.UpsertMvRequest;
import com.trinity.hermes.mv.service.MaterializedViewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mv")
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewController {

  private final MaterializedViewService materializedViewService;

  /** Register a new MV or update an existing one (upsert by name). */
  @PostMapping
  public ResponseEntity<MvRegistryDTO> upsert(@RequestBody @Valid UpsertMvRequest request) {
    log.info("POST /api/v1/mv — upsert '{}'", LogSanitizer.sanitizeLog(request.getName()));
    return ResponseEntity.ok(materializedViewService.upsert(request));
  }

  /** List all registered MVs with their full metadata and stored query. */
  @GetMapping
  public ResponseEntity<List<MvRegistryDTO>> listAll() {
    log.info("GET /api/v1/mv");
    return ResponseEntity.ok(materializedViewService.findAll());
  }

  /** Get metadata and stored query for a single MV. */
  @GetMapping("/{name}")
  public ResponseEntity<MvRegistryDTO> getByName(@PathVariable String name) {
    log.info("GET /api/v1/mv/{}", LogSanitizer.sanitizeLog(name));
    return ResponseEntity.ok(materializedViewService.findByName(name));
  }

  /** Manually trigger a refresh for a named MV. */
  @PostMapping("/{name}/refresh")
  public ResponseEntity<MvRefreshResult> refresh(@PathVariable String name) {
    log.info("POST /api/v1/mv/{}/refresh", LogSanitizer.sanitizeLog(name));
    return ResponseEntity.ok(materializedViewService.refresh(name));
  }

  /** Refresh all enabled MVs. Returns per-MV results including any failures. */
  @PostMapping("/refresh-all")
  public ResponseEntity<List<MvRefreshResult>> refreshAll() {
    log.info("POST /api/v1/mv/refresh-all");
    return ResponseEntity.ok(materializedViewService.refreshAll());
  }

  /** Drop a MV and remove it from the registry. */
  @DeleteMapping("/{name}")
  public ResponseEntity<Void> drop(@PathVariable String name) {
    log.info("DELETE /api/v1/mv/{}", LogSanitizer.sanitizeLog(name));
    materializedViewService.drop(name);
    return ResponseEntity.noContent().build();
  }

  /** Toggle the enabled flag for a MV (pauses/resumes scheduled refresh). */
  @PatchMapping("/{name}/toggle")
  public ResponseEntity<MvRegistryDTO> toggle(@PathVariable String name) {
    log.info("PATCH /api/v1/mv/{}/toggle", LogSanitizer.sanitizeLog(name));
    return ResponseEntity.ok(materializedViewService.toggle(name));
  }

  /**
   * Get the last N refresh attempts for a MV (already embedded in GET /{name}, but exposed
   * separately for quick debugging without the full metadata).
   */
  @GetMapping("/{name}/history")
  public ResponseEntity<List<MvRefreshResult>> history(@PathVariable String name) {
    log.info("GET /api/v1/mv/{}/history", LogSanitizer.sanitizeLog(name));
    return ResponseEntity.ok(materializedViewService.findByName(name).getRefreshHistory());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage(), (a, b) -> a));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }
}
