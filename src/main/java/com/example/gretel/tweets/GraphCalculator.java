package com.example.gretel.tweets;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

public class GraphCalculator {
    private static HashtagGraph graph;

    public static void initGraphCmd(Scanner scanner) {
    
        System.out.println("Enter the path to the file, if you input nothing, it will use the default input in src/main/java/com/example/gretel/static/tweets.txt:");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = "src/main/java/com/example/gretel/static/tweets.txt";
            System.out.println("You did not enter a path. Using default path: " + path);
        }
        // src/main/java/com/example/gretel/static/tweets.txt
        // In GraphCalculator.java - Parse hashtags directly from "text" field
        // Avoid parsing the entire tweet text
        long start = System.currentTimeMillis();
        graph = new HashtagGraph();
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
                    graph.addTweet(hashtags);
                } catch (IOException e) {
                    System.out.println("Error reading line: " + e.getMessage());
                }
            });
            System.out.println("Graph initialized.");
            System.out.println("Time taken: " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static List<String> parseHashtags(String input) {
        List<String> hashtags = new ArrayList<>();
        for (String tag : input.replaceAll("\\s+","").split(",")) {
            hashtags.add(tag.replaceAll("\\#", "").trim().toLowerCase());
        }
        return hashtags;
    }

    private static void addTweetCmd(Scanner scanner) {
        System.out.println("Enter hashtags, for example #data,#gretel :");
        String input = scanner.nextLine().trim();
        Set<String> hashtags = new HashSet<>();
        for (String tag : parseHashtags(input)) {
            hashtags.add(tag.trim().toLowerCase());
        }
        graph.addTweet(hashtags);
        System.out.println("Tweet added.");
    }

    private static void removeTweetCmd(Scanner scanner) {
        System.out.println("Enter hashtags to remove, for example #data,#gretel:");
        String input = scanner.nextLine().trim();
        Set<String> hashtags = new HashSet<>();
        for (String tag : parseHashtags(input)) {
            hashtags.add(tag.trim().toLowerCase());
        }
        if (hashtags.size() > 0 && graph.removeTweet(hashtags)) {
            System.out.println("Tweet removed.");
        } else {
            System.out.printf("Tweet %s not found. Please try again.", input);
        }
    }

    private static void listGraphCmd(Scanner scanner) {
        System.out.println("Enter lines you want to show, defaul 100:");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            input = "100";
        }
        // System.out.println("Nodes (Hashtags):");

        // graph.getNodesCount().forEach((tag, count) ->
        //         System.out.println(tag + " : " + count + " occurrences"));
        int lines = Integer.parseInt(input);
        System.out.println("\nEdges (Co-occurring Hashtags):");
        graph.getEdgeMap().entrySet()
            .stream()
            .limit(lines)
            .forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue()));

    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nEnter a command (init, add, remove, list-all, avg, help, exit), if you don't know how to use, type help:");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "init":
                    initGraphCmd(scanner);
                    // default path: src/main/java/com/example/gretel/static/tweets.txt
                    break;
                case "add":
                    addTweetCmd(scanner);
                    break;
                case "remove":
                    removeTweetCmd(scanner);
                    break;
                case "list-all":
                    listGraphCmd(scanner);
                    break;
                case "avg":
                    System.out.printf("Average Degree: %.3f%n", graph.getAverageDegree());
                    break;
                case "help":
                    System.out.println("\nAvailable Commands:");
                    System.out.println("--------------------------------------------------");
                    System.out.println("init\t\t\t\t\t\t\t\t- Initialize the graph (default path: src/main/java/com/example/gretel/static/tweets.txt)");
                    System.out.println("add <tweet_tags>\t\t\t\t\t\t\t- Add a new tweet to the graph");
                    System.out.println("remove <tweet_tags>\t\t\t\t\t- Remove a tweet from the graph using its ID");
                    System.out.println("list-all\t\t\t\t\t\t\t- List all tweets currently stored in the graph");
                    System.out.println("avg\t\t\t\t\t\t\t\t\t- Calculate and display the average degree of the graph");
                    System.out.println("exit\t\t\t\t\t\t\t\t- Exit the application");
                    System.out.println("\nExample usage:");
                    System.out.println("- To initialize the graph: init");
                    System.out.println("- To add a tweet tags: 1.add 2.#data,#gretel");
                    System.out.println("- To remove a tweet tags : 1.remove 2.#data,#gretel");
                    System.out.println("- To list all tweets: list-all");
                    System.out.println("- To calculate the average degree: avg");
                    System.out.println("- To exit: exit");
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid command. Try again.");
            }
        }
    }
}