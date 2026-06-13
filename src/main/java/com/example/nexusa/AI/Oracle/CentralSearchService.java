package com.example.nexusa.AI.Oracle;

import com.example.nexusa.AI.Oracle.dto.QueryIntent;
import com.example.nexusa.Model.Central.CentralEntry;
import com.example.nexusa.Repository.Central.CentralEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentralSearchService {

    private final CentralEntryRepository entryRepository;
    private final ObjectMapper objectMapper;

    public List<ParsedEntry> fetchAndParseEntries(QueryIntent intent) {
        List<CentralEntry> entries = fetchEntries(intent);
        return entries.stream()
                .filter(e -> e.getVolume().getCivilization().getTitle()
                        .equalsIgnoreCase(intent.getCivilizationName()))
                .map(this::parseEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CentralEntry> fetchEntries(QueryIntent intent) {
        if (intent.getCivilizationName() == null) return List.of();

        if (intent.getStartYear() != null || intent.getEndYear() != null) {
            return entryRepository.findByCivAndTimeRange(
                    intent.getCivilizationName(), intent.getStartYear(), intent.getEndYear());
        }
        return entryRepository.findByCivName(intent.getCivilizationName());
    }
    // In CentralSearchService
    public List<ParsedEntry> fetchForTwoCivilizations(String civ1, String civ2) {
        List<ParsedEntry> results = new ArrayList<>();
        QueryIntent i1 = new QueryIntent();
        i1.setCivilizationName(civ1);
        QueryIntent i2 = new QueryIntent();
        i2.setCivilizationName(civ2);
        results.addAll(fetchAndParseEntries(i1));
        results.addAll(fetchAndParseEntries(i2));
        return results;
    }
    public ParsedEntry parseEntry(CentralEntry entry) {
        try {
            List<ParsedBlock> blocks = new ArrayList<>();
            JsonNode arr = objectMapper.readTree(entry.getSerializedBlocks());

            for (JsonNode node : arr) {
                String blockType = node.path("blockType").asText();
                JsonNode data = node.path("data");
                JsonNode citations = node.path("citations");

                String content = switch (blockType) {
                    case "TEXT" -> stripHtml(data.path("html").asText());

                    case "EVENT" -> "%s (%s, %s): %s".formatted(
                            data.path("title").asText(),
                            data.path("date").asText(),
                            data.path("location").asText(),
                            data.path("description").asText()
                    );

                    case "ASPECT" -> "[%s] %s — Tags: %s".formatted(
                            data.path("title").asText(),
                            data.path("description").asText(),
                            data.path("tags").toString()
                    );

                    default -> data.toString();
                };

                List<String> citationSummaries = new ArrayList<>();
                for (JsonNode c : citations) {
                    if (!c.path("source").asText().isBlank()) {
                        citationSummaries.add("%s by %s (%d)".formatted(
                                c.path("source").asText(),
                                c.path("author").asText(),
                                c.path("year").asInt()
                        ));
                    }
                }

                blocks.add(new ParsedBlock(blockType, content, citationSummaries));
            }

            return new ParsedEntry(entry.getTitle(), entry.getVolume().getTitle(),
                    entry.getStartYear(), entry.getEndYear(), blocks);

        } catch (Exception e) {
            return null;
        }
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "").trim();
    }

    // Inner DTOs — keep them here, no extra files needed
    @Data
    @AllArgsConstructor
    public static class ParsedEntry {
        String entryTitle, volumeTitle;
        Long startYear, endYear;
        List<ParsedBlock> blocks;
    }

    @Data @AllArgsConstructor
    public static class ParsedBlock {
        String blockType, content;
        List<String> citations;
    }
}