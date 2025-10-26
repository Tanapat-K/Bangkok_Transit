import java.util.Iterator;

public abstract class AbstractMap<K,V> implements Map<K,V>{

    public boolean isEmpty() { return size() == 0;}

    //----------nested MapEntry class----------

    public static class MapEntry<K,V> extends Entry<K,V> {

        public MapEntry(K key,V value){
            // Call the parent 'Entry' class constructor
            super(key, value);
        }

        // Override setValue to return the old value,
        // as required by the Map interface.
        @Override
        public V setValue(V value){
            V old = getValue(); // Get the old value (from parent)
            super.setValue(value); // Set the new value (in parent)
            return old; // Return the old value
        }

        // getKey(), getValue(), setKey(), and toString()
        // are all inherited automatically from the Entry class.

    }//----------end of nested MapEntry----------


    //Support for keySet method
    private class KeyIterator implements Iterator<K> {
        private Iterator<Entry<K,V>> entries = entrySet().iterator(); // reuse EntrySet
        public boolean hasNext(){return entries.hasNext();}
        public K next(){return entries.next().getKey();}
        public void remove(){throw new UnsupportedOperationException();}
    }
    private class KeyIterable implements Iterable<K>{
        public Iterator<K> iterator(){return new KeyIterator();}
    }
    public Iterable<K> keySet(){return new KeyIterable();}

    //Support for public values method
    private class ValueIterator implements Iterator<V>{
        private Iterator<Entry<K,V>> entries = entrySet().iterator();
        public boolean hasNext(){return entries.hasNext();}
        public V next(){return entries.next().getValue();}
        public void remove(){throw new UnsupportedOperationException();}
    }
    private class ValueIterable implements Iterable<V>{
        public Iterator<V> iterator(){return new ValueIterator();}
    }
    public Iterable<V> values() { return new ValueIterable();}
}