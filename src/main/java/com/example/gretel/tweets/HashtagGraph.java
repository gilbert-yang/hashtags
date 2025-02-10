package com.example.gretel.tweets;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HashtagGraph {
    private final Map<String, Integer> nodesCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> edgeMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> tagsCount = new ConcurrentHashMap<>();
    private final AtomicInteger E = new AtomicInteger(0);
    private final AtomicInteger N = new AtomicInteger(0);

    public HashtagGraph() {
        // Default constructor
    }

    public HashtagGraph(String path) {
        loadGraphFromFile(path);
    }

    /**
     * Load graph from a file containing tweets in JSON format.
     * Parse hashtags directly from "text" field, avoid parsing the entire tweet text.
     * @param path
     */
    private void loadGraphFromFile(String path) {
        long start = System.currentTimeMillis();
        ObjectMapper objectMapper = new ObjectMapper();
        // Parallelize the processing of each line, by default using the number of available processors
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            lines.parallel().forEach(line -> {
                try (JsonParser parser = objectMapper.getFactory().createParser(line)) {
                    Set<String> hashtags = new HashSet<>();
                    // Parse hashtags directly from "text" field, avoid parsing the entire tweet text
                    while (parser.nextToken() != null) {
                        if (parser.currentToken() == JsonToken.FIELD_NAME) {
                            String fieldName = parser.getCurrentName();
                            
                            // Process only the main tweet's entities
                            if ("entities".equals(fieldName)) {
                                parser.nextToken(); // Enter "entities" object
                                while (parser.nextToken() != JsonToken.END_OBJECT) {
                                    if (parser.currentToken() == JsonToken.FIELD_NAME && 
                                        "hashtags".equals(parser.getCurrentName())) {
                                        
                                        parser.nextToken(); // Enter "hashtags" array
                                        while (parser.nextToken() != JsonToken.END_ARRAY) { 
                                            // Process each hashtag object
                                            String text = null;
                                            while (parser.nextToken() != JsonToken.END_OBJECT) {
                                                if (parser.currentToken() == JsonToken.FIELD_NAME && 
                                                    "text".equals(parser.getCurrentName())) {
                                                    
                                                    parser.nextToken(); // Move to VALUE_STRING
                                                    text = parser.getText().toLowerCase();
                                                }
                                            }
                                            if (text != null && !text.isEmpty()) {
                                                hashtags.add(text);
                                            }
                                        }
                                    }
                                }
                            } 
                            // Since quoted_status will contains hashtags too, we need to skip quoted_status entirely
                            else if ("quoted_status".equals(fieldName)) {
                                parser.nextToken(); // Move to quoted_status value (START_OBJECT)
                                parser.skipChildren(); // Skip the entire quoted_status object
                            } 
                            // Skip other fields
                            else {
                                parser.nextToken(); // Move to field value
                                parser.skipChildren(); // Skip objects/arrays
                            }
                        }
                    }
                    addTweet(hashtags);
                } catch (IOException e) {
                    System.out.println("Error reading line: " + e.getMessage());
                }
            });
            System.out.println("Graph initialized.");
            System.out.println("Time taken: " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a tweet to the graph.
     * @param hashtags
     */
    public synchronized void addTweet(Set<String> hashtags) {
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

    /**
     * Remove a tweet from the graph.
     * @param hashtags
     * @return
     */
    public synchronized boolean removeTweet(Set<String> hashtags) {
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
        return Collections.unmodifiableMap(nodesCount);
    }

    public Map<String, Integer> getEdgeMap() {
        return Collections.unmodifiableMap(edgeMap);
    }

    public Map<String, Integer> getTagsCount() {
        return Collections.unmodifiableMap(tagsCount);
    }
}
