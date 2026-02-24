package com.choragi.eventmanager.service;

import com.choragi.eventmanager.model.GlobalTourState;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BigQueryLoggerService {

    private final BigQuery bigQuery;


    private static final String DATASET_NAME = "choragi_db";
    private static final String TABLE_NAME = "tour_states";

    public void saveTourState(GlobalTourState state) {

        Map<String, Object> rowContent = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();


        try {
            String jsonLogs = mapper.writeValueAsString(state.getExecutionLogs());

            rowContent.put("discovered_venues", state.getDiscoveredVenues());
            rowContent.put("logs", jsonLogs);
        } catch (Exception e) {
            log.error("Failed to parse logs to JSON", e);
            rowContent.put("logs", "[]");
        }


        try {
            TableId tableId = TableId.of(DATASET_NAME, TABLE_NAME);

            rowContent.put("tour_id", state.getTourId());
            rowContent.put("artist_id", state.getArtistId());
            rowContent.put("status", state.getStatus());

            InsertAllResponse response = bigQuery.insertAll(
                    InsertAllRequest.newBuilder(tableId)
                            .addRow(rowContent)
                            .build());

            if (response.hasErrors()) {
                response.getInsertErrors().forEach((index, errors) ->
                        log.error("Error inserting row {}: {}", index, errors));
            } else {
                log.info("Successfully logged tour state {} to BigQuery", state.getTourId());
            }
        } catch (Exception e) {
            log.error("Failed to log to BigQuery", e);
        }
    }
}