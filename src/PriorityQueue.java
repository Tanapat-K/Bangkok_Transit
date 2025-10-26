public interface PriorityQueue<K, V> {
    int size();
    boolean isEmpty();
    void insert(K key, V value);
    Entry<K, V> min();
    Entry<K, V> removeMin();
}
