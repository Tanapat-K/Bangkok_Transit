

import java.util.ArrayList;
import java.util.Comparator;

/**
 * A binary-heap based priority queue implementation.
 * This version is standalone and includes a heapify constructor.
 */
// Fixed: implements PriorityQueue, does not extend AbstractPriorityQueue
public class Heap<K,V> implements PriorityQueue<K, V>{

    protected ArrayList<Entry<K,V>> heap = new ArrayList<>();

    // Added: Comparator field, same as HeapPQ
    private Comparator<K> comparator;

    /**
     * Creates an empty priority queue with a default comparator.
     */
    public Heap(){
        // Replaced super() with HeapPQ's default constructor logic
        this.heap = new ArrayList<>();
        this.comparator = new Comparator<K>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(K o1, K o2) {
                if (o1 instanceof Comparable) {
                    return ((Comparable<K>) o1).compareTo(o2);
                } else {
                    throw new IllegalArgumentException("Key must be Comparable or a Comparator must be provided");
                }
            }
        };
    }

    /**
     * Creates an empty priority queue using the given comparator.
     */
    public Heap(Comparator<K> comp){
        // Replaced super(comp)
        this.heap = new ArrayList<>();
        this.comparator = comp;
    }

    /**
     * Creates a priority queue from a given set of keys and values.
     * (This constructor is unique to this class)
     */
    public Heap(K[] keys, V[] values){
        this(); // Call default constructor to set up heap and comparator
        for(int j = 0; j < Math.min(keys.length, values.length); j++){
            // Updated to use 'new Entry<>()' instead of 'PQEntry'
            heap.add(new Entry<>(keys[j],values[j]));
        }
        heapify();
    }

    /**
     * (Unique to this class)
     */
    private void heapify(){
        int startIndex = parent(size()-1);
        for(int j = startIndex;j>=0;j--){
            downheap(j);
        }
    }

    // --- Helper methods (Kept from original Heap class) ---
    protected int parent(int j){
        return (j-1)/2;
    }

    protected int left(int j){
        return 2*j+1;
    }

    protected int right (int j){
        return 2*j+2;
    }

    protected boolean hasLeft(int j){
        return left(j) < heap.size();
    }

    protected boolean hasRight(int j){
        return right(j)<heap.size();
    }

    protected void swap(int i , int j){
        Entry<K,V> temp = heap.get(i);
        heap.set(i,heap.get(j));
        heap.set(j,temp);
    }

    // Added: checkKey method from HeapPQ
    private void checkKey(K key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
    }

    // Added: compare method (like HeapPQ) to compare KEYS
    private int compare(K a, K b) {
        return comparator.compare(a, b);
    }

    // --- Core Heap Logic ---

    /**
     * Moves the entry at index j higher in the heap, if necessary.
     */
    private void upheap(int j){
        while (j>0){
            int p = parent(j);
            // Updated to compare KEYS, not Entries
            if(compare(heap.get(j).getKey(), heap.get(p).getKey()) >= 0){
                break;
            }
            swap(j,p);
            j = p;
        }
    }

    /**
     * Moves the entry at index j lower in the heap, if necessary.
     */
    protected void downheap(int j){
        int size = heap.size();
        while (true) {
            int leftIdx = left(j);
            int rightIdx = right(j);
            int smallest = j;

            // Updated to compare KEYS, not Entries
            if (leftIdx < size && compare(heap.get(leftIdx).getKey(), heap.get(smallest).getKey()) < 0) {
                smallest = leftIdx;
            }
            // Updated to compare KEYS, not Entries
            if (rightIdx < size && compare(heap.get(rightIdx).getKey(), heap.get(smallest).getKey()) < 0) {
                smallest = rightIdx;
            }
            if (smallest == j) break;
            swap(j, smallest);
            j = smallest;
        }
    }

    // --- Public PriorityQueue Methods ---

    @Override
    public int size(){return heap.size();}

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public Entry<K, V> min(){
        if(heap.isEmpty()){
            return  null;
        }
        return heap.get(0);
    }

    @Override
    public void insert(K key,V value) throws IllegalArgumentException{
        checkKey(key); // Now uses the local checkKey method
        // Updated to use 'new Entry<>()'
        Entry<K,V> newest = new Entry<>(key,value);
        heap.add(newest);
        upheap(heap.size()-1);
    }

    @Override
    public Entry<K,V> removeMin(){
        if(heap.isEmpty()){
            return null;
        }
        Entry<K,V> min = heap.get(0);
        Entry<K,V> last = heap.remove(heap.size() - 1);
        if(!heap.isEmpty()){
            heap.set(0, last);
            downheap(0);
        }
        return min;
    }

    // --- Static Utility Method (Kept from original Heap class) ---
    public static <E extends Comparable<E>> void heapSort(E[] arr) {
        // This continues to work, as it uses the public constructors/methods
        Heap<E,Object> pq =
                new Heap<>(arr, (Object[]) new Object[arr.length]);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = pq.removeMin().getKey();
        }
    }
}