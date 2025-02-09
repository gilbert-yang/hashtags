package com.example.gretel.tweets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

class HashtagGraphTest {
    private static final Logger logger = LoggerFactory.getLogger(HashtagGraphTest.class);
    private HashtagGraph graph;

    @BeforeEach
    void setUp() {
        graph = new HashtagGraph();
    }

    @Test
    void addTweet_shouldHandleEmptyHashtags() {
        graph.addTweet(Set.of());
        assertEquals(0, graph.getAverageDegree());
    }

    @Test
    void addTweet_shouldCreateNodesAndEdges() {
        graph.addTweet(Set.of("data", "gretel"));
        
        assertAll(
            () -> assertEquals(2, graph.getNodesCount().size()),
            () -> assertEquals(1, graph.getEdgeMap().size()),
            () -> assertEquals(1.0, graph.getAverageDegree())
        );
    }

    @Test
    void removeTweet_shouldDecrementCounts() {
        // Add then remove
        Set<String> tags = Set.of("data", "privacy");
        graph.addTweet(tags);
        graph.removeTweet(tags);
        
        assertAll(
            () -> assertEquals(0, graph.getNodesCount().size()),
            () -> assertEquals(0, graph.getEdgeMap().size()),
            () -> assertEquals(0.0, graph.getAverageDegree())
        );
    }

    @Test
    void averageDegree_shouldHandleMultipleEdges() {
        graph.addTweet(Set.of("a", "b", "c"));
        
        // Expected edges: a-b, a-c, b-c (3 edges)
        // Average degree = (2*3)/3 = 2.0
        assertEquals(2.0, graph.getAverageDegree());
    }

    @Test
    void edgeCreation_shouldBeOrderAgnostic() {
        graph.addTweet(Set.of("b", "a"));
        assertTrue(graph.getEdgeMap().containsKey("a|b"));
    }

    @Test
    void testAverageDegreeForGivenTweets() {
        HashtagGraph graph = new HashtagGraph();

        // Add tweets
        graph.addTweet(Set.of("gretel", "data"));          // Tweet 1
        graph.addTweet(Set.of("data", "startup", "privacy")); // Tweet 2
        graph.addTweet(Set.of("data"));                   // Tweet 3
        graph.addTweet(Set.of("gretel"));                 // Tweet 4
        graph.addTweet(Set.of("rocketship", "gretel"));   // Tweet 5
        graph.addTweet(Set.of("cats"));   // Tweet 6

        // Verify nodes
        assertEquals(6, graph.getNodesCount().size(), "Number of nodes should be 6");

        // Verify edges
        assertEquals(5, graph.getEdgeMap().size(), "Number of unique edges should be 5");

        // Verify average degree
        double expectedAverageDegree = (2.0 * 5) / 6; // (2 * E) / N
        assertEquals(expectedAverageDegree, graph.getAverageDegree(), 0.01, "Average degree should be ~1.67");
    }
}