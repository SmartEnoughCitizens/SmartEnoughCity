package com.trinity.hermes.mv.controller;

import com.trinity.hermes.mv.dto.MvRefreshResult;
import com.trinity.hermes.mv.dto.MvRegistryDTO;
import com.trinity.hermes.mv.dto.UpsertMvRequest;
import com.trinity.hermes.mv.facade.MaterializedViewFacade;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mv")
@RequiredArgsConstructor
@Slf4j
public class MaterializedViewController {

  private final MaterializedViewFacade materializedViewFacade;

  /** Register a new MV or update an existing one (upsert by name). */
  @PostMapping
  public ResponseEntity<MvRegistryDTO> upsert(@RequestBody @Valid UpsertMvRequest request) {
    log.info("POST /api/v1/mv — upsert '{}'", request.getName());
    return ResponseEntity.ok(materializedViewFacade.upsert(request));
  }

  /** List all registered MVs with their full metadata and stored query. */
  @GetMapping
  public ResponseEntity<List<MvRegistryDTO>> listAll() {
    log.info("GET /api/v1/mv");
    return ResponseEntity.ok(materializedViewFacade.findAll());
  }

  /** Get metadata and stored query for a single MV. */
  @GetMapping("/{name}")
  public ResponseEntity<MvRegistryDTO> getByName(@PathVariable String name) {
    log.info("GET /api/v1/mv/{}", name);
    return ResponseEntity.ok(materializedViewFacade.findByName(name));
  }

  /** Manually trigger a refresh for a named MV. */
  @PostMapping("/{name}/refresh")
  public ResponseEntity<MvRefreshResult> refresh(@PathVariable String name) {
    log.info("POST /api/v1/mv/{}/refresh", name);
    return ResponseEntity.ok(materializedViewFacade.refresh(name));
  }

  /** Refresh all enabled MVs. Returns per-MV results including any failures. */
  @PostMapping("/refresh-all")
  public ResponseEntity<List<MvRefreshResult>> refreshAll() {
    log.info("POST /api/v1/mv/refresh-all");
    return ResponseEntity.ok(materializedViewFacade.refreshAll());
  }

  /** Drop a MV and remove it from the registry. */
  @DeleteMapping("/{name}")
  public ResponseEntity<Void> drop(@PathVariable String name) {
    log.info("DELETE /api/v1/mv/{}", name);
    materializedViewFacade.drop(name);
    return ResponseEntity.noContent().build();
  }

  /** Toggle the enabled flag for a MV (pauses/resumes scheduled refresh). */
  @PatchMapping("/{name}/toggle")
  public ResponseEntity<MvRegistryDTO> toggle(@PathVariable String name) {
    log.info("PATCH /api/v1/mv/{}/toggle", name);
    return ResponseEntity.ok(materializedViewFacade.toggle(name));
  }

  /**
   * Get the last N refresh attempts for a MV (already embedded in GET /{name},
   * but exposed separately for quick debugging without the full metadata).
   */
  @GetMapping("/{name}/history")
  public ResponseEntity<List<MvRefreshResult>> history(@PathVariable String name) {
    log.info("GET /api/v1/mv/{}/history", name);
    return ResponseEntity.ok(materializedViewFacade.findByName(name).getRefreshHistory());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
