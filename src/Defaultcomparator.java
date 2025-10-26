import java.util.Comparator;

public class Defaultcomparator<K> implements Comparator<K> {

    @SuppressWarnings("unchecked")
    public int compare(K a, K b)throws ClassCastException{
        return ((Comparable<K>)a).compareTo(b);
    }
}
