import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Main {

    //Graph Interface
    private Graph<String, String> railwayGraph;

    private Map<String, Vertex<String>> stations; //MapADT for Dijkstra to detect the passed station
    private int stationtime = 3; //second (Weight)
    private int interchangetime = 10; //second (Weight)

    public List<String> MatainanceList = new ArrayList<>(); //The list of station that currently unavaliable

    // MAP for Abbreviations: Stores long name -> short name
    private static final Map<String, String> ABBREVIATION_MAP = new HashMap<>();

    static {
        // Initialize abbreviations here
        ABBREVIATION_MAP.put("Queen Sirikit National Convention Centre", "QSNCC");
        // Add any other long station names you want to abbreviate here
    }

    public Main() {
        // Initialize as an undirected graph
        this.railwayGraph = new AdjacencyMapGraph<>(false);
        this.stations = new HashMap<>();
    }

    /**
     * Helper method to apply abbreviations to a station name.
     * @param originalName The full station name.
     * @return The abbreviated name if found, otherwise the original name.
     */
    private String applyAbbreviations(String originalName) {
        return ABBREVIATION_MAP.getOrDefault(originalName, originalName);
    }

    // Reverse map for the disclaimer
    private static final Map<String, String> DISCLOSURE_MAP = new HashMap<>();
    static {
        for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
            DISCLOSURE_MAP.put(entry.getValue(), entry.getKey());
        }
    }


    public void addMaintenanceList(List<String> station){
        MatainanceList.addAll(station);
    }

    public void removeMaintenanceList(String station){
        MatainanceList.remove(station);
    }

    public void setstationtime(int stationtime){
        this.stationtime = stationtime;
    }

    public void setinterchangetime(int interchangetime){
        this.interchangetime = interchangetime;
    }

    /**load all of the connected lines and stations to the Graph Structure using AdjacentMapGraph
     * using java.io.file
     * @param connectionPATH // the Path of the all connection of the Railway
     */
    public void loadConnections(String connectionPATH) {
        try (BufferedReader br = new BufferedReader(new FileReader(connectionPATH))) {
            String line;
            br.readLine(); // Skip header line

            //read the line and skip the comma and place in 3 variable
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

                // Get or create vertex for station A
                Vertex<String> vertexA = stations.get(stationNameA);
                if (vertexA == null) {
                    vertexA = railwayGraph.insertVertex(stationNameA);
                    stations.put(stationNameA, vertexA);
                }

                // Get or create vertex for station B
                Vertex<String> vertexB = stations.get(stationNameB);
                if (vertexB == null) {
                    vertexB = railwayGraph.insertVertex(stationNameB);
                    stations.put(stationNameB, vertexB);
                }

                // Insert an edge between the two stations, if it doesn't exist yet
                if (railwayGraph.getEdge(vertexA, vertexB) == null) {
                    railwayGraph.insertEdge(vertexA, vertexB, railwayLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the connections file: " + e.getMessage());
            e.printStackTrace();
        }

    }


    public List<Vertex<String>> findShortestPath(String startStationName, String endStationName) {
        Vertex<String> startVertex = stations.get(startStationName);
        Vertex<String> endVertex = stations.get(endStationName);

        if (startVertex == null || endVertex == null) {
            System.err.println("Invalid start or end station.");
            return null;
        }

        // Priority queue stores entries of <Distance, Vertex>
        PriorityQueue<Integer, Vertex<String>> pq = new Heap<>();
        // Maps to store distances, predecessors (for path reconstruction), and entries in the PQ
        Map<Vertex<String>, Integer> dist = new HashMap<>();
        Map<Vertex<String>, Vertex<String>> predecessor = new HashMap<>();
        Map<Vertex<String>, Entry<Integer, Vertex<String>>> pqEntries = new HashMap<>();

        // Initialize all distances to infinity and add to the distance map
        for (Vertex<String> v : railwayGraph.vertices()) {
            dist.put(v, Integer.MAX_VALUE);
        }

        // Set distance for the start vertex to 0 and add to PQ
        dist.put(startVertex, 0);
        Entry<Integer, Vertex<String>> startEntry = new Entry<>(0, startVertex);
        pq.insert(startEntry.getKey(), startEntry.getValue());
        pqEntries.put(startVertex, startEntry);

        while (!pq.isEmpty()) {
            Entry<Integer, Vertex<String>> entry = pq.removeMin();
            Vertex<String> u = entry.getValue();

            // If we've reached the destination, we can stop
            if (u.equals(endVertex)) {
                break;
            }

            // For each neighbor of the current vertex

            for (Edge<String> e : railwayGraph.outgoingEdges(u)) {
                Vertex<String> v = railwayGraph.opposite(u, e);

                // define the weight between interchange and regular (Time)
                int weight = e.getElement().equals("Interchange") ? interchangetime : stationtime;

                int newDist = dist.get(u) + weight;

                // If the route exceeds the time limit, do not consider it further
                if (newDist > 5000) {
                    continue;
                }

                // If we found a shorter path to v
                if (newDist < dist.get(v)) {
                    // Update distance and predecessor
                    dist.put(v, newDist);
                    predecessor.put(v, u);

                    // re-insert. if PQ doesn't have key
                    Entry<Integer, Vertex<String>> newEntry = new Entry<>(newDist, v);
                    pq.insert(newEntry.getKey(), newEntry.getValue());
                    pqEntries.put(v, newEntry);
                }
            }
        }

        return reconstructPath(predecessor, startVertex, endVertex);
    }


    private static class DistancePair implements Comparable<DistancePair> {
        int transfers;
        int time;

        DistancePair(int transfers, int time) {
            this.transfers = transfers;
            this.time = time;
        }

        @Override
        public int compareTo(DistancePair o) {
            if (this.transfers != o.transfers) return Integer.compare(this.transfers, o.transfers);
            return Integer.compare(this.time, o.time);
        }
    }


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

        for (Vertex<String> v : railwayGraph.vertices()) {
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

            for (Edge<String> e : railwayGraph.outgoingEdges(u)) {
                Vertex<String> v = railwayGraph.opposite(u, e);

                int weight = e.getElement().equals("Interchange") ? interchangetime : stationtime;
                int transferInc = e.getElement().equals("Interchange") ? 1 : 0;

                DistancePair candidate = new DistancePair(du.transfers + transferInc, du.time + weight);
                DistancePair current = dist.get(v);
                if (candidate.compareTo(current) < 0) {
                    dist.put(v, candidate);
                    predecessor.put(v, u);
                    pq.insert(candidate, v);
                }
            }
        }

        return reconstructPath(predecessor, startVertex, endVertex);
    }

    private List<Vertex<String>> reconstructPath(Map<Vertex<String>, Vertex<String>> predecessor, Vertex<String> start, Vertex<String> end) {
        List<Vertex<String>> path = new ArrayList<>();
        Vertex<String> current = end;
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break; // Path found
            current = predecessor.get(current);
        }

        if (path.isEmpty() || !Objects.equals(path.get(path.size() - 1),(start))) return null; // No path found

        Collections.reverse(path);
        return path;
    }

    public class linePassinfo{
        String name = null;
        int numberofStation = 0;

        public linePassinfo(String station, int numberofStation){
            this.name = station;
            this.numberofStation = numberofStation;
        }
    }

    //Summing up the total weight from each edge to calculate for totaltime cost
    public int CalculateTotalTime(List<Vertex<String>> path) {
        if (path == null || path.size() <= 1) {
            return 0;
        }
        int totalTime = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Vertex<String> prev = path.get(i);
            Vertex<String> next = path.get(i + 1);
            Edge<String> edge = railwayGraph.getEdge(prev, next);
            if (edge == null) continue;
            totalTime += edge.getElement().equals("Interchange") ? interchangetime : stationtime;
        }
        return totalTime;
    }

    // Validate station name
    public boolean checkStationAvailable(String stationName) {
        if (stationName == null || stationName.trim().isEmpty()) {
            System.out.println("Station name is empty.");
            return false;
        }

        String query = stationName.trim();

        // Check against original names first
        for (String key : stations.keySet()) {
            if (key.equals(query)) {
                return true;
            }
        }

        // Check against abbreviated names
        for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
            if (entry.getValue().equals(query)) {
                return true;
            }
        }

        // Failure: print basic error message.
        System.out.println("Invalid station: '" + stationName + "'");
        return false;
    }

    public static void main (String[] args) {
        Main bkkRailwayApp = new Main();

        // Station List received and showed
        bkkRailwayApp.loadConnections("src/BTS/connections.csv");
        System.out.println();

        // 1. Get all station names into a simple list and apply abbreviations
        List<String> stationNames = new ArrayList<>();
        for (Vertex<String> v : bkkRailwayApp.railwayGraph.vertices()) {
            // Apply abbreviation for display
            stationNames.add(bkkRailwayApp.applyAbbreviations(v.getElement()));
        }

        int totalStations = stationNames.size();
        int numColumns = 3;
        // Calculate the number of stations in each column to ensure even distribution
        int stationsPerColumn = (int) Math.ceil((double) totalStations / numColumns);

        // Use a StringBuilder for efficient string concatenation
        StringBuilder sb = new StringBuilder();

        // 2. Format and print the list in 3 columns (54 rows each)
        sb.append("Station List (Total: " + totalStations + ")\n");
        for (int i = 0; i < stationsPerColumn; i++) {
            // Column 1
            if (i < totalStations) {
                sb.append(String.format("%-30s", stationNames.get(i)));
            }

            // Column 2 (Starts after the first column ends)
            int indexCol2 = i + stationsPerColumn;
            if (indexCol2 < totalStations) {
                sb.append(String.format("%-30s", stationNames.get(indexCol2)));
            } else {
                sb.append(String.format("%-30s", "")); // Print empty space if no data
            }

            // Column 3 (Starts after the second column ends)
            int indexCol3 = i + (2 * stationsPerColumn);
            if (indexCol3 < totalStations) {
                sb.append(String.format("%s", stationNames.get(indexCol3)));
            }

            sb.append("\n"); // Newline for the next row
        }

        System.out.println(sb.toString()); // Print the formatted table

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
            // Fallback for IDEs where System.console() is null
            scanner = new java.util.Scanner(System.in);
            System.out.println("No console available. Falling back to standard input. Type 'exit' to quit.");
        }

        String start = null;
        // Prompt until a valid station is entered or user types 'exit'
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
            // Check station available using original or abbreviated name
            if (bkkRailwayApp.checkStationAvailable(input)) {
                // Ensure the input is converted back to the official name for pathfinding if an abbreviation was used
                start = ABBREVIATION_MAP.entrySet().stream()
                        .filter(e -> e.getValue().equals(input.trim()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(input.trim());
                break;
            }
            // else loop and let checkStationAvailable print suggestions
        }

        String end = null;
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
                // Ensure the input is converted back to the official name for pathfinding if an abbreviation was used
                end = ABBREVIATION_MAP.entrySet().stream()
                        .filter(e -> e.getValue().equals(input.trim()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(input.trim());
                break;
            }
        }
        // Do not close scanner yet; we'll use it for additional prompts below in IDE fallback

        // Check for exclude-maintenance flag in CLI args
        for (String a : args) {
            if (a.equalsIgnoreCase("-e") || a.equalsIgnoreCase("--exclude-maintenance")) {
                // Maintenance logic has been removed from the class.
                System.out.println("NOTE: Maintenance exclusion feature is no longer supported.");
                break;
            }
        }

        // Ask whether user wants a route that minimizes transfers
        boolean minimizeTransfers = false;
        String choice = null;
        if (console != null) {
            choice = console.readLine("Would you like to minimize transfers? (y/N): ");
        } else {
            System.out.print("Would you like to minimize transfers? (y/N): ");
            choice = scanner.hasNextLine() ? scanner.nextLine() : null;
        }
        if (choice != null && (choice.equalsIgnoreCase("y") || choice.equalsIgnoreCase("yes"))) minimizeTransfers = true;

        List<Vertex<String>> path;
        if (minimizeTransfers) {
            path = bkkRailwayApp.findPathFewestTransfers(start, end);
        } else {
            path = bkkRailwayApp.findShortestPath(start, end);
        }

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
                Edge<String> edge = bkkRailwayApp.railwayGraph.getEdge(u, v);
                String nextLine = (edge == null) ? "Unknown" : edge.getElement();

                // Get the abbreviated name for printing
                String abbreviatedV = bkkRailwayApp.applyAbbreviations(v.getElement());

                if (nextLine.equals("Interchange")) {
                    // This is a transfer edge: print instruction, reset line, and print next station
                    if (!currentLine.isEmpty() && !currentLine.equals("Interchange")) {
                        // Print summary for the line segment just completed
                        System.out.println("   --> FINISHED Segment (" + stopsOnLine + " stop" + (stopsOnLine != 1 ? "s" : "") + " on " + currentLine + ")");
                        System.out.println("[TRANSFER] Change lines at " + abbreviatedV);
                    }
                    currentLine = "Interchange";
                    stopsOnLine = 0;

                } else if (!nextLine.equals(currentLine)) {
                    // This is the start of a new line segment (not Interchange)
                    if (!currentLine.isEmpty() && !currentLine.equals("Interchange")) {
                        // Print summary for the line segment just completed
                        System.out.println("   --> FINISHED Segment (" + stopsOnLine + " stop" + (stopsOnLine != 1 ? "s" : "") + " on " + currentLine + ")");
                        System.out.println("[TRANSFER] Board " + nextLine + " at " + abbreviatedV);
                    } else if (currentLine.equals("Interchange")) {
                        // Just completed a transfer/walk, now boarding a new line
                        System.out.println("   --> BOARD " + nextLine + " from " + bkkRailwayApp.applyAbbreviations(u.getElement()));
                    }
                    currentLine = nextLine;
                    stopsOnLine = 1; // Count the next station now

                } else {
                    // Continue on the same line segment
                    stopsOnLine++;
                }

                // Print the station being arrived at, only if it's not the last one in the loop (which is printed in the summary)
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

            int totalTimeSeconds = bkkRailwayApp.CalculateTotalTime(path);
            int totalMinutes = totalTimeSeconds / 60;
            int remainingSeconds = totalTimeSeconds % 60;

            System.out.println("\n--- Summary ---");
            System.out.println("Total stops: " + (path.size() - 1));
            System.out.println("Estimated Total Time: " + totalMinutes + " minute(s) and " + remainingSeconds + " second(s)");
        } else {
            System.out.println("No path found from " + start + " to " + end);
        }
    }
}