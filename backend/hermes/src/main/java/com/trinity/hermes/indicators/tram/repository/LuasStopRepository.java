package com.trinity.hermes.indicators.tram.repository;

import com.trinity.hermes.indicators.tram.entity.LuasStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LuasStopRepository extends JpaRepository<LuasStop, String> {
    List<LuasStop> findByLine(String line);
}
