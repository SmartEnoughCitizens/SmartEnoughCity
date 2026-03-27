package com.trinity.hermes.mv.facade;

import com.trinity.hermes.mv.dto.MvRefreshResult;
import com.trinity.hermes.mv.dto.MvRegistryDTO;
import com.trinity.hermes.mv.dto.UpsertMvRequest;
import com.trinity.hermes.mv.service.MaterializedViewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterializedViewFacade {

  private final MaterializedViewService materializedViewService;

  public MvRegistryDTO upsert(UpsertMvRequest request) {
    return materializedViewService.upsert(request);
  }

  public MvRefreshResult refresh(String name) {
    return materializedViewService.refresh(name);
  }

  public List<MvRefreshResult> refreshAll() {
    return materializedViewService.refreshAll();
  }

  public List<MvRegistryDTO> findAll() {
    return materializedViewService.findAll();
  }

  public MvRegistryDTO findByName(String name) {
    return materializedViewService.findByName(name);
  }

  public void drop(String name) {
    materializedViewService.drop(name);
  }

  public MvRegistryDTO toggle(String name) {
    return materializedViewService.toggle(name);
  }
}
