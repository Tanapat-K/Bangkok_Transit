import java.util.HashSet;
import java.util.Set;

public class AdjacencyMapGraph<V,E> implements Graph<V,E> {

    //Inner private class
    private class InnerVertex<V> implements Vertex<V> {
        private V element;
        private Position<Vertex<V>> pos;
        private ProbeHashMap<Vertex<V>, Edge<E>> outgoing,incoming;

        //Constructs a new InnerVertex instance storing the given element.
        public InnerVertex(V elem, boolean graphIsDirected) {
            element = elem;
            outgoing = new ProbeHashMap<Vertex<V>, Edge<E>>();
            if (graphIsDirected)
                incoming = new ProbeHashMap<Vertex<V>, Edge<E>>();
            else
                incoming = outgoing; // if undirected, alias outgoing map
        }

        //Return the element associated with the vertex
        public V getElement() {
            return element;
        }
        //Stores the position of this vertex within the graph's vertex list.
        public void setPosition(Position<Vertex<V>> p)
        {
            pos = p;
        }
        //Returns the position of this vertex within the graph's vertex list.
        public Position<Vertex<V>> getPosition( ) {
            return pos;
        }
        //Returns reference to the underlying map of outgoing edges.
        public ProbeHashMap<Vertex<V>,Edge<E>> getOutgoing(){
            return outgoing;
        }
        //Returns reference to the underlying map of incoming edges.
        public ProbeHashMap<Vertex<V>,Edge<E>> getIncoming(){
            return incoming;
        }

        public boolean validate(Graph<V, E> graph){
            return AdjacencyMapGraph.this == graph && pos != null;
        }
        //End of innerVertex-class
    }

    //Private InnerEdge-class
    private class InnerEdge<E> implements Edge<E> {
        private E element;
        private Position <Edge<E>> pos;
        private Vertex<V>[] endpoints;
        //Construct inner instance
        public InnerEdge(Vertex<V> u ,Vertex<V> v,E elem){
            element = elem;
            endpoints = (Vertex<V>[]) new Vertex[]{u,v}; //array length of 2
        }
        //Return element associated with edge
        public E getElement() {
            return element;
        }
        //Return reference to the endpoint array
        public Vertex<V>[] getEndpoints(){
            return endpoints;
        }
        //Store the position of this edge  within the graph's vertex list.
        public void setPosition(Position<Edge<E>> p){
            pos = p;
        }
        //Return the postion of this edge within the graph's vertex list.
        public Position<Edge<E>> getPosition(){
            return pos;
        }
        public boolean validate(Graph<V, E> graph) {
            return AdjacencyMapGraph.this == graph && pos != null;
        }
        // End of innerEdge-class
    }


    private boolean isDirected;
    private PositionalList<Vertex<V>> vertices = new LinkedPositionalList<>();
    private PositionalList<Edge<E>> edges = new LinkedPositionalList<>();

    //Construct an Empty Graph(either undirected or directed)
    public AdjacencyMapGraph(boolean directed) {isDirected = directed; }
    //Return a number of vertices of the graph
    public int numVertices() {return vertices.size();}
    //Return a number of edges of the graph
    public int numEdges() {
        return edges.size();
    }
    //Returns the vertices of the graph as an iterable collection
    public Iterable<Vertex<V>> vertices( ) {
        return vertices ;
    }
    //Return the edges of the graph as an iterable collection
    public Iterable<Edge<E>> edges( ) {return (Iterable<Edge<E>>) edges; }
    //Returns the number of edges for which vertex v is the origin.
    public int outDegree(Vertex<V> v) {
    InnerVertex<V> vert = validate(v);
    return vert.getOutgoing( ).size( );
    }
    //Returns an iterable collection of edges for which vertex v is the origin.
    public Iterable<Edge<E>> outgoingEdges(Vertex<V> v) {
        InnerVertex<V> vert = validate(v);
        return vert.getOutgoing( ).values( ); // edges are the values in the adjacency map
    }
    //Returns the number of edges for which vertex v is the destination.
    public int inDegree(Vertex<V> v) {
        InnerVertex<V> vert = validate(v);
        return vert.getIncoming( ).size( );
        }
    //Returns an iterable collection of edges for which vertex v is the destination.
    public Iterable<Edge<E>> incomingEdges(Vertex<V> v) {
        InnerVertex<V> vert = validate(v);
        return vert.getIncoming( ).values( ); // edges are the values in the adjacency map
        }
    public Edge<E> getEdge(Vertex<V> u, Vertex<V> v) {
    //Returns the edge from u to v, or null if they are not adjacent.
        InnerVertex<V> origin = validate(u);
        return origin.getOutgoing( ).get(v); // will be null if no edge from u to v
        }
    //Returns the vertices of edge e as an array of length two
    public Vertex<V>[ ] endVertices(Edge<E> e) {
        InnerEdge<E> edge = validate(e);
        return edge.getEndpoints( );
        }

    @Override
    public Vertex<V> opposite(Vertex<V> vertex, Edge<E> edge) throws IllegalArgumentException {
        validate(vertex);
        InnerEdge<E> e = validate(edge);

        Vertex<V>[] endpoints = e.getEndpoints();
        if (endpoints[0] == vertex) {
            return endpoints[1];
        } else if (endpoints[1] == vertex) {
            return endpoints[0];
        } else {
            throw new IllegalArgumentException("v is not incident to this edge");
        }
    }

    @Override
    public Vertex<V> insertVertex(V element) {
        InnerVertex<V> v = new InnerVertex<>(element, isDirected);
        Position<Vertex<V>> position = vertices.addLast(v);
        v.setPosition(position);
        return v;
    }

    @Override
    public Edge<E> insertEdge(Vertex<V> u, Vertex<V> v, E element) throws IllegalArgumentException {
        if (getEdge(u, v) == null) {
            InnerEdge<E> e = new InnerEdge<>(u, v, element);
            Position<Edge<E>> position = edges.addLast(e);
            e.setPosition(position);

            InnerVertex<V> origin = validate(u);
            InnerVertex<V> dest = validate(v);

            // CORRECTED: u's outgoing map should map DESTINATION (v) to edge (e)
            origin.getOutgoing().put(v, e);

            // CORRECTED: v's incoming map should map ORIGIN (u) to edge (e)
            // Note: For undirected (where incoming = outgoing), this will add a reverse entry.
            dest.getIncoming().put(u, e);

            return e;
        } else {
            throw new IllegalArgumentException("Edge from u to v exists");
        }
    }

    @Override
    public void removeVertex(Vertex<V> vertex) throws IllegalArgumentException {
        InnerVertex<V> v = validate(vertex);

        // remove all incident edges from the graph
        for (Edge<E> e : v.getOutgoing().values()) {
            removeEdge(e);
        }

        for (Edge<E> e : v.getIncoming().values()) {
            removeEdge(e);
        }

        vertices.remove(v.getPosition());
    }

    @Override
    public void removeEdge(Edge<E> e) throws IllegalArgumentException {
        InnerEdge<E> edge = validate(e);

        Vertex<V>[] endpoints = edge.getEndpoints();
        InnerVertex<V> u = validate(endpoints[0]);
        InnerVertex<V> v = validate(endpoints[1]);

        u.getOutgoing().remove(v);
        v.getIncoming().remove(u);

        edges.remove(edge.getPosition());
    }

    @SuppressWarnings({"unchecked"})
    private InnerVertex<V> validate(Vertex<V> v) {
        if (!(v instanceof InnerVertex)) {
            throw new IllegalArgumentException("Invalid vertex");
        }
        InnerVertex<V> vert = (InnerVertex<V>) v;     // safe cast
        if (!vert.validate(this)) {
            throw new IllegalArgumentException("Invalid vertex");
        }
        return vert;
    }

    @SuppressWarnings({"unchecked"})
    private InnerEdge<E> validate(Edge<E> e) {
        if (!(e instanceof InnerEdge)) {
            throw new IllegalArgumentException("Invalid edge");
        }
        InnerEdge<E> edge = (InnerEdge<E>) e;     // safe cast
        if (!edge.validate(this)) {
            throw new IllegalArgumentException("Invalid edge");
        }
        return edge;
    }

    //Performs depth-first search of Graph g starting at Vertex u.
    public static <V,E> void DFS(Graph<V,E> g, Vertex<V> u, Set<Vertex<V>> known, Map<Vertex<V>,Edge<E>> forest) {
        known.add(u); // u has been discovered
        for (Edge<E> e : g.outgoingEdges(u)) { // for every outgoing edge from u
            Vertex<V> v = g.opposite(u, e);
            if (!known.contains(v)) {
                forest.put(v, e);           // e is the tree edge that discovered v
                DFS(g, v, known, forest);   // recursively explore from v

            }
        }
    }

    public static <V,E> ProbeHashMap<Vertex<V>,Edge<E>> DFSComplete(Graph<V,E> g) {
        Set<Vertex<V>> known = new HashSet<>( );
        ProbeHashMap<Vertex<V>,Edge<E>> forest = new ProbeHashMap<>( );
        for (Vertex<V> u : g.vertices( )) {
            if (!known.contains(u))
                DFS(g, u, known, forest); // (re)start the DFS process at u
        }
        return forest;
    }

    //Performs breadth-first search of Graph g starting at Vertex u.
    public static <V,E> void BFS(Graph<V,E> g, Vertex<V> s,
        Set<Vertex<V>> known, ProbeHashMap<Vertex<V>,Edge<E>> forest) {
        PositionalList<Vertex<V>> level = new LinkedPositionalList<>( );
        known.add(s);
        level.addLast(s); // first level includes only s
        while (!level.isEmpty( )) {
            PositionalList<Vertex<V>> nextLevel = new LinkedPositionalList<>( );
        for (Vertex<V> u : level)
           for (Edge<E> e : g.outgoingEdges(u)) {
              Vertex<V> v = g.opposite(u, e);
              if (!known.contains(v)) {
                  known.add(v);
                  forest.put(v, e);     // e is the tree edge that discovered v
                  nextLevel.addLast(v); // v will be further considered in next pass
              }
           }
        level = nextLevel; // relabel next level to become the current
        }
    }


}
