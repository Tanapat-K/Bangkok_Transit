# Bangkok Transit Project

## By TANAPAT KANGSADAN ID: 6701013610051

This repository contains the source code for a console application designed to calculate the optimal travel route between any two stations on the Bangkok rail transit system. It leverages graph theory, specifically Dijkstra's algorithm, to minimize either total estimated travel time or the number of transfers.

## Compilation and Execution
This project is structured using several interdependent data structure files. All helper files (Entry.java, Graph.java, Heap.java, etc.) must be present in the project structure for successful compilation.

## Prerequisites
Java Development Kit (JDK) 8 or higher.

The project assumes a specific directory structure (e.g., src/BTS/connections.csv). Make sure it the correcte directory on your device

Compilation Steps
Gather Helper Files: Ensure all necessary interfaces and abstract classes are present:

AbstractHashMap.java 
AbstractMap.java
AdjacencyMapGraph.java
DefaultComparator.java
Edge.java 
Entry.java 
Entry2.java 
Graph.java 
Heap.java
LinkedPositionalList.java 
Map.java 
Position.java 
PositionalList.java 
PriorityQueue.java
ProbeHashMap.java
Vertex.java

## Compilation
: Compile all .java files from your project root or src directory. If using IntelliJ IDEA, simply building the project handles this automatically.


### ******Execute: Run the Main class.******


The program will load the station data, print the list of available stations, and then prompt the user for the starting and destination stations, followed by the choice of optimization (Time or Transfers).

## Analysis and Justification
Graph Representation: Adjacency Map
 The Adjacency Map model is the best choice for this transit network primarily	 because the network is sparse (stations have very few connections), and the model offers the most efficient balance of space and operational speed for sparse graphs.
The core reason this structure is ideal is Space Efficiency. A transit network is naturally sparse—a station might link to only 2 or 3 neighbors, while an Adjacency Matrix would reserve space for every possible connection, resulting in a large amount of wasted memory O(V^2). The Adjacency Map model only stores the existing connections (E) for each station (V), making its space complexity O(V + E), which is far more efficient.
The second major benefit is Speed for Traversal. Dijkstra's algorithm relies on quickly finding all neighbors of a given station to perform relaxation. In the Adjacency Map, finding all neighbors is exceptionally fast, taking time proportional to the station's degree O(degree(v)). This low complexity is crucial because the maximum number of tracks leaving any single station is very small, allowing the algorithm to traverse the network with optimal efficiency. This speed directly contributes to the overall O(E log V) complexity of your pathfinding solution.
The graph is implemented using an Adjacency Map model, realized through the AdjacencyMapGraph<V, E> class.

### Structure: Each station (Vertex) stores two ProbeHashMaps: one for outgoing edges and one for incoming edges. The map keys are the neighboring Vertices, and the values are the Edges (connections) themselves.

 Reasoning (Sparsity): The Bangkok rail network is an inherently sparse graph; most stations connect to only 2 or 3 neighbors.

#### Time Complexity: An Adjacency Map is ideal for sparse graphs.

Checking for a specific edge (getEdge(u, v)) is O(1) on average (due to the HashMap backing).

Iterating over neighbors (outgoingEdges(v)) is O(degree(v)), which is very fast for a transit network where the maximum degree is small.

#### Space Complexity: O(V + E). Space is proportional only to the number of stations (V) and the number of connections (E). This is far more efficient than an Adjacency Matrix, which would waste space storing many non-existent connections (O(V²)).

### Weighting Model: Total Estimated Minutes (Model B)
I chose a weighting model based on total estimated minutes, where:

#### Station-to-Station Travel: Each segment between two regular stations is assigned a weight of 3 minutes (stationtime).

#### Interchange Penalty: Any transfer edge is assigned a penalty weight of 10 minutes (interchangetime) to account for the time lost in walking, changing platforms, and waiting.

This model provides a practical route for the user by balancing distance (implicit in the number of segments) with the real-world inconvenience of transfers.

### Implementation Logic:

Find Shortest Path (Minimize Time): This uses standard Dijkstra's with total time as the single weight criterion.

Find Path Fewest Transfers (Minimize Transfers, then Time): This uses a lexicographic weight (DistancePair<Transfers, Time>). The primary goal is finding the minimum number of transfer edges. If two paths have the same number of transfers, the secondary goal is chosen: the path with the minimum total travel time.

### Priority Queue: Binary Heap (Heap.java)
A Binary Heap structure (implemented in Heap.java) was chosen to back the Priority Queue used by both Dijkstra's algorithms.

Efficiency: A Heap provides a high-efficiency implementation of the core PQ operations:

insert: O(log n)

removeMin: O(log n)

min: O(1)

### Effectiveness in Dijkstra's
: Using a heap ensures that the total runtime of the relaxation phase in Dijkstra's algorithm remains efficient, achieving a time complexity of O(E log V), which is crucial for handling large graphs quickly. The heap reliably identifies the unvisited vertex with the minimum current accumulated distance (time or lexicographic pair) in logarithmic time.
