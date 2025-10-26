import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Main application class for the Bangkok Transit shortest path finder.
 * This class handles graph initialization, data loading, user input,
 * Dijkstra's pathfinding (minimizing Time or Transfers), and formatted route output.
 */
public class Main {

    // --- Core Data Structures & Configuration ---

    // The core graph structure implemented using an AdjacencyMap.
    private Graph<String, String> TransitGraph;

    // Map to provide O(1) lookup of a Vertex object given its String name (station name).
    private Map<String, Vertex<String>> stations;

    // Weights (time in seconds) applied to edges for time-based Dijkstra's.
    private int stationtime = 3; // Time cost for travel between two regular stations(minute).
    private int interchangetime = 10; // Time cost (penalty) for transferring between two different lines(minute).

    // Map used to shorten long station names for better display formatting (e.g., QSNCC).
    private static final Map<String, String> ABBREVIATION_MAP = new HashMap<>();

    static {
        // Initialize abbreviations here to be applied before displaying station list.
        ABBREVIATION_MAP.put("Queen Sirikit National Convention Centre", "QSNCC");
        // Add any other long station names here
    }

    /**
     * Constructor: Initializes the graph as undirected and sets up the station map.
     */
    public Main() {
        this.TransitGraph = new AdjacencyMapGraph<>(false);
        this.stations = new HashMap<>();
    }

    /**
     * Utility method: Applies defined abbreviations to a station name for output clarity.
     * @param originalName The full station name.
     * @return The abbreviated name if a mapping exists, otherwise the original name.
     */
    private String applyAbbreviations(String originalName) {
        return ABBREVIATION_MAP.getOrDefault(originalName, originalName);
    }

    // Reverse map for the disclaimer: Maps abbreviated name -> original full name.
    private static final Map<String, String> DISCLOSURE_MAP = new HashMap<>();
    static {
        for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
            DISCLOSURE_MAP.put(entry.getValue(), entry.getKey());
        }
    }

    // --- Configuration and Data Loading Methods ---

    public void setstationtime(int stationtime){
        this.stationtime = stationtime;
    }

    public void setinterchangetime(int interchangetime){
        this.interchangetime = interchangetime;
    }

    /**
     * Loads graph data from a CSV file, inserting Vertices for stations
     * and Edges for connections (labeled by line name).
     * @param connectionPATH The file path of the connection data.
     */
    public void loadConnections(String connectionPATH) {
        try (BufferedReader br = new BufferedReader(new FileReader(connectionPATH))) {
            String line;
            br.readLine(); // Skip header line

            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                if (values.length < 3) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                String stationNameA = values[0].trim();
                String stationNameB = values[1].trim();
                String railwayLine = values[2].trim();

                // Ensure both station vertices exist.
                Vertex<String> vertexA = stations.get(stationNameA);
                if (vertexA == null) {
                    vertexA = TransitGraph.insertVertex(stationNameA);
                    stations.put(stationNameA, vertexA);
                }

                Vertex<String> vertexB = stations.get(stationNameB);
                if (vertexB == null) {
                    vertexB = TransitGraph.insertVertex(stationNameB);
                    stations.put(stationNameB, vertexB);
                }

                // Insert the connection (Edge) if not already present.
                if (TransitGraph.getEdge(vertexA, vertexB) == null) {
                    TransitGraph.insertEdge(vertexA, vertexB, railwayLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the connections file: " + e.getMessage());
            e.printStackTrace();
        }

    }

    // --- Pathfinding Implementation (Dijkstra's) ---

    /**
     * Finds the **Shortest Path** minimizing **Total Travel Time** using Dijkstra's algorithm.
     * Weights are derived from 'stationtime' and 'interchangetime'.
     * @param startStationName The starting station name.
     * @param endStationName The destination station name.
     * @return A List of Vertices representing the time-optimized path.
     */
    public List<Vertex<String>> findShortestPath(String startStationName, String endStationName) {
        Vertex<String> startVertex = stations.get(startStationName);
        Vertex<String> endVertex = stations.get(endStationName);

        if (startVertex == null || endVertex == null) {
            System.err.println("Invalid start or end station.");
            return null;
        }

        // Standard Dijkstra setup: Priority Queue (PQ), Distance Map, Predecessor Map.
        PriorityQueue<Integer, Vertex<String>> pq = new Heap<>();
        Map<Vertex<String>, Integer> dist = new HashMap<>();
        Map<Vertex<String>, Vertex<String>> predecessor = new HashMap<>();
        Map<Vertex<String>, Entry<Integer, Vertex<String>>> pqEntries = new HashMap<>();

        for (Vertex<String> v : TransitGraph.vertices()) {
            dist.put(v, Integer.MAX_VALUE);
        }

        dist.put(startVertex, 0);
        Entry<Integer, Vertex<String>> startEntry = new Entry<>(0, startVertex);
        pq.insert(startEntry.getKey(), startEntry.getValue());
        pqEntries.put(startVertex, startEntry);

        while (!pq.isEmpty()) {
            Entry<Integer, Vertex<String>> entry = pq.removeMin();
            Vertex<String> u = entry.getValue();

            if (u.equals(endVertex)) {
                break;
            }

            // Relaxation Step: Check all outgoing neighbors.
            for (Edge<String> e : TransitGraph.outgoingEdges(u)) {
                Vertex<String> v = TransitGraph.opposite(u, e);

                // Weight is either 'interchangetime' or 'stationtime'.
                int weight = e.getElement().equals("Interchange") ? interchangetime : stationtime;
                int newDist = dist.get(u) + weight;

                if (newDist > 5000) { // Safety check against excessively long/infinite paths.
                    continue;
                }

                // If a shorter path is found.
                if (newDist < dist.get(v)) {
                    dist.put(v, newDist);
                    predecessor.put(v, u);

                    // Update the PQ with the newly found shorter path.
                    Entry<Integer, Vertex<String>> newEntry = new Entry<>(newDist, v);
                    pq.insert(newEntry.getKey(), newEntry.getValue());
                    pqEntries.put(v, newEntry);
                }
            }
        }

        return reconstructPath(predecessor, startVertex, endVertex);
    }


    /**
     * Nested class representing a multi-criteria key: Transfers (primary) and Time (secondary).
     */
    private static class DistancePair implements Comparable<DistancePair> {
        int transfers;
        int time;

        DistancePair(int transfers, int time) {
            this.transfers = transfers;
            this.time = time;
        }

        @Override
        public int compareTo(DistancePair o) {
            // Lexicographic comparison: Primary key is transfers.
            if (this.transfers != o.transfers) return Integer.compare(this.transfers, o.transfers);
            // Secondary key is time.
            return Integer.compare(this.time, o.time);
        }
    }


    /**
     * Finds the path minimizing **Transfers** (primary key) and **Time** (secondary key)
     * by using a specialized DistancePair weight in Dijkstra's.
     * @param startStationName The starting station name.
     * @param endStationName The destination station name.
     * @return A List of Vertices representing the transfer-optimized path.
     */
    public List<Vertex<String>> findPathFewestTransfers(String startStationName, String endStationName) {
        Vertex<String> startVertex = stations.get(startStationName);
        Vertex<String> endVertex = stations.get(endStationName);

        if (startVertex == null || endVertex == null) {
            System.err.println("Invalid start or end station.");
            return null;
        }

        PriorityQueue<DistancePair, Vertex<String>> pq = new Heap<>();
        Map<Vertex<String>, DistancePair> dist = new HashMap<>();
        Map<Vertex<String>, Vertex<String>> predecessor = new HashMap<>();

        for (Vertex<String> v : TransitGraph.vertices()) {
            dist.put(v, new DistancePair(Integer.MAX_VALUE/2, Integer.MAX_VALUE/2));
        }

        DistancePair startD = new DistancePair(0, 0);
        dist.put(startVertex, startD);
        pq.insert(startD, startVertex);

        while (!pq.isEmpty()) {
            Entry<DistancePair, Vertex<String>> entry = pq.removeMin();
            Vertex<String> u = entry.getValue();
            DistancePair du = entry.getKey();

            if (u.equals(endVertex)) break;

            for (Edge<String> e : TransitGraph.outgoingEdges(u)) {
                Vertex<String> v = TransitGraph.opposite(u, e);

                // Calculate the weights based on the edge type.
                int weight = e.getElement().equals("Interchange") ? interchangetime : stationtime;
                int transferInc = e.getElement().equals("Interchange") ? 1 : 0; // Transfer penalty is 1.

                DistancePair candidate = new DistancePair(du.transfers + transferInc, du.time + weight);
                DistancePair current = dist.get(v);

                // If the new path is lexicographically better (fewer transfers OR same transfers and less time).
                if (candidate.compareTo(current) < 0) {
                    dist.put(v, candidate);
                    predecessor.put(v, u);
                    pq.insert(candidate, v);
                }
            }
        }

        return reconstructPath(predecessor, startVertex, endVertex);
    }

    /**
     * Reconstructs the path from destination to start using the predecessor map.
     * @param predecessor Map storing the path taken to reach each vertex.
     * @param start The starting vertex.
     * @param end The destination vertex.
     * @return The path as a forward-ordered list of vertices.
     */
    private List<Vertex<String>> reconstructPath(Map<Vertex<String>, Vertex<String>> predecessor, Vertex<String> start, Vertex<String> end) {
        List<Vertex<String>> path = new ArrayList<>();
        Vertex<String> current = end;
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break; // Path found
            current = predecessor.get(current);
        }

        // Validation: Path found must end at the starting station.
        if (path.isEmpty() || !Objects.equals(path.get(path.size() - 1),(start))) return null;

        Collections.reverse(path);
        return path;
    }


    /**
     * Calculates the estimated total travel time in seconds by summing up all edge weights along the path.
     * @param path The list of vertices representing the path.
     * @return The total time in seconds.
     */
    public int CalculateTotalTime(List<Vertex<String>> path) {
        if (path == null || path.size() <= 1) {
            return 0;
        }
        int totalTime = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Vertex<String> prev = path.get(i);
            Vertex<String> next = path.get(i + 1);
            Edge<String> edge = TransitGraph.getEdge(prev, next);
            if (edge == null) continue;
            totalTime += edge.getElement().equals("Interchange") ? interchangetime : stationtime;
        }
        return totalTime;
    }

    /**
     * Validates if a station name (original or abbreviated) exists in the graph.
     * @param stationName The name provided by the user.
     * @return true if the station is found, false otherwise (prints "Invalid station").
     */
    public boolean checkStationAvailable(String stationName) {
        if (stationName == null || stationName.trim().isEmpty()) {
            System.out.println("Station name is empty.");
            return false;
        }

        String query = stationName.trim();

        // 1. Check against original names first
        for (String key : stations.keySet()) {
            if (key.equals(query)) {
                return true;
            }
        }

        // 2. Check against abbreviated names
        for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
            if (entry.getValue().equals(query)) {
                return true;
            }
        }

        // Failure: print basic error message.
        System.out.println("Invalid station: '" + stationName + "'");
        return false;
    }

    // --- Main Execution Block ---

    public static void main (String[] args) {
        Main bkkRailwayApp = new Main();

        // Load graph data from CSV and print basic statistics for setup verification.
        bkkRailwayApp.loadConnections("src/BTS/connections.csv");

        System.out.println("--------------Bangkok Transit---------------------  ");

        // 1. Prepare and print the station list in a formatted, multi-column display.
        List<String> stationNames = new ArrayList<>();
        for (Vertex<String> v : bkkRailwayApp.TransitGraph.vertices()) {
            // Apply abbreviation for display
            stationNames.add(bkkRailwayApp.applyAbbreviations(v.getElement()));
        }

        int totalStations = stationNames.size();
        int numColumns = 3;
        int stationsPerColumn = (int) Math.ceil((double) totalStations / numColumns);

        StringBuilder sb = new StringBuilder();
        sb.append("Station operating List (Total: " + totalStations + ")\n\n");

        for (int i = 0; i < stationsPerColumn; i++) {
            // Column 1
            if (i < totalStations) {
                sb.append(String.format("%-30s", stationNames.get(i)));
            }

            // Column 2
            int indexCol2 = i + stationsPerColumn;
            if (indexCol2 < totalStations) {
                sb.append(String.format("%-30s", stationNames.get(indexCol2)));
            } else {
                sb.append(String.format("%-30s", ""));
            }

            // Column 3
            int indexCol3 = i + (2 * stationsPerColumn);
            if (indexCol3 < totalStations) {
                sb.append(String.format("%s", stationNames.get(indexCol3)));
            }

            sb.append("\n");
        }

        System.out.println(sb.toString());

        // 3. Print the disclaimer for abbreviated stations
        if (!ABBREVIATION_MAP.isEmpty()) {
            System.out.println("--- Abbreviation Disclosure ---");
            for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
                System.out.println("* " + entry.getValue() + " is " + entry.getKey());
            }
            System.out.println("-----------------------------\n");
        }


        System.out.println("\n--- Welcome to Finding Shortest Path of Bangkok Transit :) ---");

        java.io.Console console = System.console();
        java.util.Scanner scanner = null;
        if (console == null) {
            scanner = new java.util.Scanner(System.in);
            System.out.println("No console available. Falling back to standard input. Type 'exit' to quit.");
        }

        // --- User Input and Validation ---

        String start = null;
        // Prompt for starting station until valid input is received.
        while (true) {
            String input;
            if (console != null) {
                input = console.readLine("Enter starting station (or 'exit' to quit): ");
            } else {
                System.out.print("Enter starting station (or 'exit' to quit): ");
                input = scanner.hasNextLine() ? scanner.nextLine() : null;
            }
            if (input == null || input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting.");
                if (scanner != null) scanner.close();
                return;
            }
            if (bkkRailwayApp.checkStationAvailable(input)) {
                // Convert input (which might be an abbreviation) back to the official full name for pathfinding.
                start = ABBREVIATION_MAP.entrySet().stream()
                        .filter(e -> e.getValue().equals(input.trim()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(input.trim());
                break;
            }
        }

        String end = null;
        // Prompt for destination station until valid input is received.
        while (true) {
            String input;
            if (console != null) {
                input = console.readLine("Enter destination station (or 'exit' to quit): ");
            } else {
                System.out.print("Enter destination station (or 'exit' to quit): ");
                input = scanner.hasNextLine() ? scanner.nextLine() : null;
            }
            if (input == null || input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting.");
                if (scanner != null) scanner.close();
                return;
            }
            if (bkkRailwayApp.checkStationAvailable(input)) {
                // Convert input (which might be an abbreviation) back to the official full name for pathfinding.
                end = ABBREVIATION_MAP.entrySet().stream()
                        .filter(e -> e.getValue().equals(input.trim()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(input.trim());
                break;
            }
        }

        // --- Path Calculation Selection ---

        // Dummy check for maintenance arguments (functionality removed, but argument check remains).
        for (String a : args) {
            if (a.equalsIgnoreCase("-e") || a.equalsIgnoreCase("--exclude-maintenance")) {
                System.out.println("NOTE: Maintenance exclusion feature is no longer supported.");
                break;
            }
        }

        // Ask user for routing preference (minimum time or minimum transfers).
        boolean minimizeTransfers = false;
        String choice = null;
        if (console != null) {
            choice = console.readLine("Would you like to minimize transfers? (Y/N): ");
        } else {
            System.out.print("Would you like to minimize transfers? (Y/N): ");
            choice = scanner.hasNextLine() ? scanner.nextLine() : null;
        }
        if (choice != null && (choice.equalsIgnoreCase("y") || choice.equalsIgnoreCase("yes"))) minimizeTransfers = true;

        List<Vertex<String>> path;
        if (minimizeTransfers) {
            // Use specialized Dijkstra's with weights (Transfers and then Time).
            path = bkkRailwayApp.findPathFewestTransfers(start, end);
        } else {
            // Use standard Dijkstra's minimizing total travel time.
            path = bkkRailwayApp.findShortestPath(start, end);
        }

        // --- Output and Visualization ---

        if (path != null) {
            System.out.println((minimizeTransfers ? "Route (minimized transfers)" : "Shortest path") + " from " + start + " to " + end + ":");

            String currentLine = "";
            int stepCounter = 1;
            int stopsOnLine = 0;

            // Print the starting station outside the loop
            System.out.println(stepCounter + ". " + bkkRailwayApp.applyAbbreviations(path.get(0).getElement()) + " (Start)");
            stepCounter++;

            for (int i = 0; i < path.size() - 1; i++) {
                Vertex<String> u = path.get(i);
                Vertex<String> v = path.get(i + 1);
                Edge<String> edge = bkkRailwayApp.TransitGraph.getEdge(u, v);
                String nextLine = (edge == null) ? "Unknown" : edge.getElement();

                // Get the abbreviated name for printing
                String abbreviatedV = bkkRailwayApp.applyAbbreviations(v.getElement());

                if (nextLine.equals("Interchange")) {
                    // Handle the moment of transfer (Interchange Edge)
                    if (!currentLine.isEmpty() && !currentLine.equals("Interchange")) {
                        // End the previous segment and print the summary/transfer action
                        System.out.println("   --> FINISHED Segment (" + stopsOnLine + " stop" + (stopsOnLine != 1 ? "s" : "") + " on " + currentLine + ")");
                        System.out.println("[TRANSFER] Change lines at " + abbreviatedV);
                    }
                    currentLine = "Interchange";
                    stopsOnLine = 0;

                } else if (!nextLine.equals(currentLine)) {
                    // Handle the start of a new line segment (or first non-Interchange segment)
                    if (!currentLine.isEmpty() && !currentLine.equals("Interchange")) {
                        // End the previous segment and print the transfer/boarding action
                        System.out.println("   --> FINISHED Segment (" + stopsOnLine + " stop" + (stopsOnLine != 1 ? "s" : "") + " on " + currentLine + ")");
                        System.out.println("[TRANSFER] Board " + nextLine + " at " + abbreviatedV);
                    } else if (currentLine.equals("Interchange")) {
                        // Just finished a transfer, now boarding the next line
                        System.out.println("   --> BOARD " + nextLine + " from " + bkkRailwayApp.applyAbbreviations(u.getElement()));
                    }
                    currentLine = nextLine;
                    stopsOnLine = 1;

                } else {
                    // Continue on the same line segment
                    stopsOnLine++;
                }

                // Print the current station (unless it's the destination, which is handled after the loop)
                if (i < path.size() - 2) {
                    if (!nextLine.equals("Interchange")) {
                        System.out.println(stepCounter + ". " + abbreviatedV);
                        stepCounter++;
                    }
                }
            }

            // Print the FINAL destination station
            System.out.println(stepCounter + ". " + bkkRailwayApp.applyAbbreviations(path.get(path.size() - 1).getElement()) + " (Destination)");

            // Print summary for the final segment
            if (!currentLine.equals("Interchange")) {
                System.out.println("   --> FINAL Segment (" + stopsOnLine + " stop" + (stopsOnLine != 1 ? "s" : "") + " on " + currentLine + ")");
            }

            // Print final summary statistics.
            int totalTimeMinute = bkkRailwayApp.CalculateTotalTime(path); // This value is in seconds

            int totalHour = totalTimeMinute / 60; // Total number of hours ( 1 hr = 60 minutes)
            int totalHours = 0;
            int Minutes;

            if (totalTimeMinute >= 60) { //In case it take more than 60 minute
                totalHours = totalTimeMinute / 60; // Calculate full hours
                Minutes = totalTimeMinute % 60; // Calculate remaining minutes
            } else {
                // If less than an hour, totalHours is 0
                Minutes = totalTimeMinute; // All minutes are "remaining"
            }


            System.out.println("\n--- Summary ---");
            System.out.println("Total stops: " + (path.size() - 1));
            System.out.println("Estimated Total Time: " + totalHours + " hours(hr) and " + Minutes + " minutes(m)");
        } else {
            System.out.println("No path found from " + start + " to " + end);
        }
    }
}