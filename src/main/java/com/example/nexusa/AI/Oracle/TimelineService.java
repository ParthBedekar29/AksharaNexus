package com.example.nexusa.AI.Oracle;

import com.example.nexusa.AI.Oracle.dto.TimelineEvent;
import com.example.nexusa.Model.Central.CentralEntry;
import com.example.nexusa.Repository.Central.CentralEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final CentralEntryRepository entryRepository;
    private final ObjectMapper objectMapper;

    public List<TimelineEvent> buildTimeline(String civilizationName) {
        List<CentralEntry> entries = entryRepository.findByCivName(civilizationName);
        List<TimelineEvent> events = new ArrayList<>();

        for (CentralEntry entry : entries) {
            try {
                JsonNode arr = objectMapper.readTree(entry.getSerializedBlocks());
                for (JsonNode node : arr) {
                    if (!"EVENT".equals(node.path("blockType").asText())) continue;
                    JsonNode data = node.path("data");

                    String title       = data.path("title").asText();
                    String date        = data.path("date").asText();
                    String location    = data.path("location").asText();
                    String description = data.path("description").asText();

                    // Best-effort year parsing for sorting
                    Long year = parseYear(date);

                    events.add(new TimelineEvent(title, date, location, description, year));
                }
            } catch (Exception ignored) {}
        }

        // Sort: known years first (ascending), then unknowns at end
        events.sort(Comparator.comparingLong(
                e -> e.getYear() != null ? e.getYear() : Long.MAX_VALUE));

        return events;
    }

    private Long parseYear(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            // Handle "2500 BCE", "323 BC", "1200 CE", plain "500"
            String d = date.trim().toUpperCase();
            boolean bce = d.contains("BCE") || d.contains("BC");
            String digits = d.replaceAll("[^0-9]", "");
            if (digits.isBlank()) return null;
            long y = Long.parseLong(digits);
            return bce ? -y : y;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}