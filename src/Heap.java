import java.util.ArrayList;
import java.util.Comparator;

/**
 * A binary-heap based priority queue implementation.
 * This class provides O(log n) performance for insert and removeMin,
 * making it an efficient choice for use in algorithms like Dijkstra's.
 */
public class Heap<K,V> implements PriorityQueue<K, V>{

    // The underlying data structure for the heap: an array list to store entries.
    protected ArrayList<Entry<K,V>> heap = new ArrayList<>();

    // Comparator used to define the priority/ordering of keys (min-heap standard).
    private Comparator<K> comparator;

    // --- Constructors ---

    /**
     * Creates an empty priority queue with a default comparator, assuming keys are Comparable.
     */
    public Heap(){
        this.heap = new ArrayList<>();
        this.comparator = new Comparator<K>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(K o1, K o2) {
                if (o1 instanceof Comparable) {
                    // Default behavior: use the key's natural ordering.
                    return ((Comparable<K>) o1).compareTo(o2);
                } else {
                    throw new IllegalArgumentException("Key must be Comparable or a Comparator must be provided");
                }
            }
        };
    }

    /**
     * Creates an empty priority queue using a custom external comparator.
     */
    public Heap(Comparator<K> comp){
        this.heap = new ArrayList<>();
        this.comparator = comp;
    }

    /**
     * Creates a priority queue from a bulk load of keys and values, followed by heapification.
     * @param keys Array of keys.
     * @param values Array of associated values.
     */
    public Heap(K[] keys, V[] values){
        this(); // 1. Set up heap and comparator.
        for(int j = 0; j < Math.min(keys.length, values.length); j++){
            // 2. Load all initial elements into the array.
            heap.add(new Entry<>(keys[j],values[j]));
        }
        heapify(); // 3. Reorganize the array into a valid heap structure.
    }

    /**
     * Reorganizes an arbitrary array into a valid heap structure (bottom-up approach).
     * Achieves O(n) construction time.
     */
    private void heapify(){
        // Start downheap from the last non-leaf node (parent of the last element).
        int startIndex = parent(size()-1);
        for(int j = startIndex;j>=0;j--){
            downheap(j);
        }
    }

    // --- Index Helper Methods ---

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

    // --- Validation and Comparison ---

    private void checkKey(K key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
    }

    private int compare(K a, K b) {
        // Delegates comparison logic to the stored comparator.
        return comparator.compare(a, b);
    }

    // --- Core Heap Maintenance Logic ---

    /**
     * Restores the heap property by moving the entry at index j up the tree (bubble-up).
     * Used after insertion. O(log n).
     */
    private void upheap(int j){
        while (j>0){
            int p = parent(j);
            // Stop if the current key is greater than or equal to the parent key (min-heap property satisfied).
            if(compare(heap.get(j).getKey(), heap.get(p).getKey()) >= 0){
                break;
            }
            swap(j,p);
            j = p; // Continue up from the new parent position.
        }
    }

    /**
     * Restores the heap property by moving the entry at index j down the tree (bubble-down).
     * Used after removeMin and in heapify. O(log n).
     */
    protected void downheap(int j){
        int size = heap.size();
        while (true) {
            int leftIdx = left(j);
            int rightIdx = right(j);
            int smallest = j;

            // Find the smaller of the two children (if they exist).
            if (leftIdx < size && compare(heap.get(leftIdx).getKey(), heap.get(smallest).getKey()) < 0) {
                smallest = leftIdx;
            }
            if (rightIdx < size && compare(heap.get(rightIdx).getKey(), heap.get(smallest).getKey()) < 0) {
                smallest = rightIdx;
            }

            if (smallest == j) break; // Heap property is satisfied, stop.

            swap(j, smallest);
            j = smallest; // Continue down from the position of the swapped child.
        }
    }

    // --- Public PriorityQueue Interface Methods ---

    @Override
    public int size(){return heap.size();}

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public Entry<K, V> min(){
        // The minimum element is always at the root (index 0). O(1).
        if(heap.isEmpty()){
            return  null;
        }
        return heap.get(0);
    }

    @Override
    public void insert(K key,V value) throws IllegalArgumentException{
        checkKey(key);
        // Add the new entry to the end of the array.
        Entry<K,V> newest = new Entry<>(key,value);
        heap.add(newest);
        // Restore heap property by moving the new element up. O(log n).
        upheap(heap.size()-1);
    }

    @Override
    public Entry<K,V> removeMin(){
        if(heap.isEmpty()){
            return null;
        }
        Entry<K,V> min = heap.get(0);
        // Replace the root with the last element and remove the last element.
        Entry<K,V> last = heap.remove(heap.size() - 1);

        if(!heap.isEmpty()){
            // Place the former last element at the root.
            heap.set(0, last);
            // Restore heap property by moving the new root down. O(log n).
            downheap(0);
        }
        return min;
    }

    // --- Static Utility Method ---

    /**
     * Implements Heap Sort (in-place) using the Heap Priority Queue logic.
     * Note: This implementation constructs a separate HeapPQ instance for clarity.
     * @param arr The array to be sorted.
     */
    public static <E extends Comparable<E>> void heapSort(E[] arr) {
        // Construct the PQ (implicitly heapifying the array data).
        Heap<E,Object> pq =
                new Heap<>(arr, (Object[]) new Object[arr.length]);

        // Extract elements one by one (min first) back into the array, resulting in ascending order.
        for (int i = 0; i < arr.length; i++) {
            arr[i] = pq.removeMin().getKey();
        }
    }
}