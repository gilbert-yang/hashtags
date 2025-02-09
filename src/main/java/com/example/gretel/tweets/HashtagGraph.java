package com.example.gretel.tweets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashtagGraph {
    private final Map<String, Integer> nodesCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> edgeMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> tagsCount = new ConcurrentHashMap<>();
    private final AtomicInteger E = new AtomicInteger(0);
    private final AtomicInteger N = new AtomicInteger(0);

    public void addTweet(Set<String> hashtags) {
        List<String> sortedTags = new ArrayList<>(hashtags);
        Collections.sort(sortedTags);
        String hashtagsKey = String.join("|", sortedTags);

        // Track occurrences of this exact hashtag set
        tagsCount.compute(hashtagsKey, (k, v) -> (v == null) ? 1 : v + 1);

        // Update nodes and edges atomically
        for (String tag : hashtags) {
            nodesCount.compute(tag, (k, v) -> {
                if (v == null) {
                    N.incrementAndGet(); // Atomic increment
                    return 1;
                }
                return v + 1;
            });
        }

        for (int i = 0; i < sortedTags.size(); i++) {
            for (int j = i + 1; j < sortedTags.size(); j++) {
                String tag1 = sortedTags.get(i);
                String tag2 = sortedTags.get(j);
                String pairKey = tag1 + "|" + tag2;

                edgeMap.compute(pairKey, (k, v) -> {
                    if (v == null) {
                        E.incrementAndGet(); // Atomic increment
                        return 1;
                    }
                    return v + 1;
                });
            }
        }
    }

    public boolean removeTweet(Set<String> hashtags) {
        List<String> sortedTags = new ArrayList<>(hashtags);
        Collections.sort(sortedTags);
        String hashtagsKey = String.join("|", sortedTags);

        if (!tagsCount.containsKey(hashtagsKey)) {
            return false;
        }

        // Decrement or remove the hashtag set
        tagsCount.computeIfPresent(hashtagsKey, (k, v) -> (v == 1) ? null : v - 1);

        // Update nodes and edges atomically
        for (String tag : hashtags) {
            nodesCount.computeIfPresent(tag, (k, v) -> {
                if (v == 1) {
                    N.decrementAndGet(); // Atomic decrement
                    return null;
                }
                return v - 1;
            });
        }

        for (int i = 0; i < sortedTags.size(); i++) {
            for (int j = i + 1; j < sortedTags.size(); j++) {
                String tag1 = sortedTags.get(i);
                String tag2 = sortedTags.get(j);
                String pairKey = tag1 + "|" + tag2;

                edgeMap.computeIfPresent(pairKey, (k, v) -> {
                    if (v == 1) {
                        E.decrementAndGet(); // Atomic decrement
                        return null;
                    }
                    return v - 1;
                });
            }
        }
        return true;
    }

    
    // Default method calls the overloaded method with 3 decimals
    public double getAverageDegree() {
        return getAverageDegree(3);
    }

    // Overloaded method that allows specifying decimal places
    public double getAverageDegree(int decimals) {
        if (N.get() == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((2.0 * E.get()) / N.get())
                        .setScale(decimals, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    public Map<String, Integer> getNodesCount() {
        return nodesCount;
    }

    public Map<String, Integer> getEdgeMap() {
        return edgeMap;
    }
}
