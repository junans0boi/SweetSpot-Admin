package com.hollywood.sweetspotadmin.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollywood.sweetspotadmin.place.model.Place;
import com.hollywood.sweetspotadmin.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class GeocodingBatchService {
    @Value("${vworld.api.key}")
    private String vworldApiKey;
    @Value("${vworld.api.url}")
    private String vworldApiUrl;
    private final PlaceRepository placeRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int GEOCODING_LIMIT_PER_DAY = 39000;
    private static final int BATCH_SIZE = 100;
    private static final long API_CALL_DELAY_MS = 100;
    @Async
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void processMissingCoordinates() {
        log.info("[GeocodingBatch] Starting geocoding for places with missing coordinates.");
        int processedCount = 0;
        int successCount = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        while (processedCount < GEOCODING_LIMIT_PER_DAY) {

            Page<Place> placesToProcess = placeRepository.findByGeomIsNullAndEpsg5174xIsNull(pageable);

            if (placesToProcess.isEmpty()) {

                log.info("[GeocodingBatch] No more places to process. Finishing task.");

                break;

            }

            for (Place place : placesToProcess.getContent()) {

                if (processedCount >= GEOCODING_LIMIT_PER_DAY) {

                    log.warn("[GeocodingBatch] Daily API limit ({}) reached. Stopping task.", GEOCODING_LIMIT_PER_DAY);

                    break;

                }

                String address = StringUtils.hasText(place.getRoadAddress())

                        ? place.getRoadAddress()

                        : place.getJibunAddress();

                Point point = callGeocodeApi(address);

                processedCount++;

                if (point != null) {

                    place.setGeom(point);

                    placeRepository.save(place);

                    successCount++;

                } else {

                    log.warn("[GeocodingBatch] Geocoding failed for Place ID: {}, Address: {}", place.getId(), address);

                }

                try {

                    Thread.sleep(API_CALL_DELAY_MS);

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    log.error("[GeocodingBatch] Interrupted during API call delay.", e);

                }

            }

            if (processedCount >= GEOCODING_LIMIT_PER_DAY || !placesToProcess.hasNext()) {

                break;

            }

        }

        log.info("[GeocodingBatch] Geocoding batch finished. Total API calls: {}, Successful geocodes: {}",
                processedCount, successCount);

    }

    private Point callGeocodeApi(String address) {

        if (!StringUtils.hasText(address)) {

            return null;

        }

        final int MAX_RETRIES = 3;

        long retryDelayMs = 1000; // Start with 1 second delay

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {

                String url = UriComponentsBuilder

                        .fromUriString(vworldApiUrl)

                        .queryParam("service", "address")

                        .queryParam("request", "getCoord")

                        .queryParam("format", "json")

                        .queryParam("type", "ROAD")

                        .queryParam("crs", "epsg:4326")

                        .queryParam("key", vworldApiKey)

                        .queryParam("address", address)

                        .toUriString();

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());

                if ("OK".equals(root.path("response").path("status").asText())) {

                    JsonNode pointNode = root.path("response").path("result").path("point");

                    double lon = pointNode.path("x").asDouble();

                    double lat = pointNode.path("y").asDouble();

                    return geometryFactory.createPoint(new Coordinate(lon, lat));

                }

                // If status is not OK, but no exception, treat as failure and don't retry

                log.warn("V-World API returned non-OK status for address: {}. Response: {}", address,
                        root.path("response").path("status").asText());

                return null;

            } catch (org.springframework.web.client.HttpServerErrorException
                    | org.springframework.web.client.ResourceAccessException e) {

                log.warn("V-World API call failed for address: {} (Attempt {}/{}) - {}. Retrying in {}ms...",

                        address, attempt, MAX_RETRIES, e.getMessage(), retryDelayMs);

                if (attempt < MAX_RETRIES) {

                    try {

                        Thread.sleep(retryDelayMs);

                    } catch (InterruptedException ie) {

                        Thread.currentThread().interrupt();

                        log.error("Retry delay interrupted.", ie);

                        return null; // Exit if interrupted

                    }

                    retryDelayMs *= 2; // Exponential backoff

                } else {

                    log.error("V-World API call failed after {} attempts for address: {}", MAX_RETRIES, address, e);

                    return null; // All retries failed

                }

            } catch (Exception e) {

                log.error("Exception during V-World API call for address: {}", address, e);

                return null; // Other unexpected exceptions, no retry

            }

        }

        return null; // Should not be reached

    }

}
