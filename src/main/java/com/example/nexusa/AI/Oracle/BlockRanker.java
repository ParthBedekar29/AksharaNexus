package com.example.nexusa.AI.Oracle;

import com.example.nexusa.AI.Oracle.dto.QueryIntent;
import com.example.nexusa.AI.Oracle.dto.RankedBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BlockRanker {

    public List<RankedBlock> rank(List<CentralSearchService.ParsedEntry> entries,
                                  QueryIntent intent) {
        List<RankedBlock> ranked = new ArrayList<>();
        for (CentralSearchService.ParsedEntry entry : entries) {
            for (CentralSearchService.ParsedBlock block : entry.getBlocks()) {
                double score = scoreBlock(block.getBlockType(), block.getContent(), entry.getEntryTitle(), intent);
                ranked.add(new RankedBlock(
                        entry.getEntryTitle(),
                        entry.getVolumeTitle(),
                        block.getBlockType(),
                        block.getContent(),
                        block.getCitations(),
                        score
                ));
            }
        }
        ranked.sort(Comparator.comparingDouble(RankedBlock::getRelevanceScore).reversed());
        return ranked;
    }

    private double scoreBlock(String blockType, String content, String entryTitle, QueryIntent intent) {
        double score = 0;
        String contentLower = content.toLowerCase();
        String titleLower = entryTitle.toLowerCase();

        for (String kw : intent.getKeywords()) {
            long hits = countOccurrences(contentLower, kw);
            score += hits * 2.0;
        }

        for (String kw : intent.getKeywords()) {
            if (titleLower.contains(kw)) score += 5.0;
        }

        if (intent.getTopic() != null && contentLower.contains(intent.getTopic())) {
            score += 8.0;
        }

        // ✅ Fixed: compare blockType, not content
        if (intent.getTopic() != null) {
            if ("EVENT".equals(blockType) || "ASPECT".equals(blockType)) score += 3.0;
        }

        return score;
    }

    private long countOccurrences(String text, String keyword) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) { count++; idx++; }
        return count;
    }
}