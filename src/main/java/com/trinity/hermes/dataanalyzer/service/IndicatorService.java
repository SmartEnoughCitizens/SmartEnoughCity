package com.trinity.hermes.dataanalyzer.service;

import com.trinity.hermes.dataanalyzer.dto.IndicatorDTO;
import com.trinity.hermes.dataanalyzer.dto.IndicatorResponse;
import com.trinity.hermes.dataanalyzer.dto.RecommendationEngineRequest;
import com.trinity.hermes.dataanalyzer.model.Indicator;
import com.trinity.hermes.dataanalyzer.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;

    /**
     * Get indicator data for dashboard
     * @param indicatorType Type of indicator (bus, car, etc.)
     * @return IndicatorResponse with data and statistics
     */
    @Transactional(readOnly = true)
    public IndicatorResponse getIndicatorDataForDashboard(String indicatorType) {
        log.info("Fetching indicator data for dashboard: {}", indicatorType);

        List<Indicator> indicators = indicatorRepository.findByIndicatorTypeOrderByTimestampDesc(indicatorType);

        List<IndicatorDTO> indicatorDTOs = indicators.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        IndicatorResponse.StatisticsSummary statistics = calculateStatistics(indicators);

        return new IndicatorResponse(
                indicatorType,
                indicatorDTOs.size(),
                indicatorDTOs,
                statistics
        );
    }

    /**
     * Get indicator data for recommendation engine
     * @param request Request containing indicator type and filters
     * @return List of IndicatorDTO
     */
    @Transactional(readOnly = true)
    public List<IndicatorDTO> getIndicatorDataForRecommendation(RecommendationEngineRequest request) {
        log.info("Fetching indicator data for recommendation engine: {}", request.getIndicatorType());

        List<Indicator> indicators;

        if (request.getStartDate() != null && request.getEndDate() != null) {
            indicators = indicatorRepository.findByIndicatorTypeAndDateRange(
                    request.getIndicatorType(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        } else {
            indicators = indicatorRepository.findByIndicatorTypeOrderByTimestampDesc(
                    request.getIndicatorType()
            );
        }

        // Apply limit if specified
        if (request.getLimit() != null && request.getLimit() > 0) {
            indicators = indicators.stream()
                    .limit(request.getLimit())
                    .collect(Collectors.toList());
        }

        return indicators.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Save indicator data (for data ingestion)
     * @param indicator Indicator entity to save
     * @return Saved indicator
     */
//    @Transactional
//    public Indicator saveIndicator(Indicator indicator) {
//        log.info("Saving indicator data: {} - {}", indicator.getIndicatorType(), indicator.getMetricName());
//        return indicatorRepository.save(indicator);
//    }

    /**
     * Convert Indicator entity to DTO
     */
    private IndicatorDTO convertToDTO(Indicator indicator) {
        return new IndicatorDTO(
                indicator.getId(),
                indicator.getIndicatorType(),
                indicator.getMetricName(),
                indicator.getValue(),
                indicator.getUnit(),
                indicator.getLocation(),
                indicator.getTimestamp(),
                indicator.getMetadata()
        );
    }

    /**
     * Calculate statistics for indicators
     */
    private IndicatorResponse.StatisticsSummary calculateStatistics(List<Indicator> indicators) {
        if (indicators.isEmpty()) {
            return new IndicatorResponse.StatisticsSummary(0.0, 0.0, 0.0, 0.0);
        }

        DoubleSummaryStatistics stats = indicators.stream()
                .mapToDouble(Indicator::getValue)
                .summaryStatistics();

        Double latest = indicators.isEmpty() ? 0.0 : indicators.get(0).getValue();

        return new IndicatorResponse.StatisticsSummary(
                stats.getAverage(),
                stats.getMin(),
                stats.getMax(),
                latest
        );
    }

    /**
     * Get latest indicator data by type
     */
    @Transactional(readOnly = true)
    public List<IndicatorDTO> getLatestIndicatorData(String indicatorType, int limit) {
        log.info("Fetching latest {} records for indicator: {}", limit, indicatorType);

        List<Indicator> indicators = indicatorRepository.findByIndicatorTypeOrderByTimestampDesc(indicatorType);

        return indicators.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}