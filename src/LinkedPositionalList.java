import java.util.Iterator;
import java.util.NoSuchElementException;


// The main class now only implements PositionalList<E>
// and its iterator() method must be changed to return E
public class LinkedPositionalList<E> implements PositionalList<E> {

    // ------------------- NEW NESTED CLASS -------------------
    /**
     * An iterator for the elements stored in the list.
     * This is what the main 'for-each' loops need.
     */
    private class ElementIterator implements Iterator<E> {
        // We reuse the PositionIterator logic but yield the element
        Iterator<Position<E>> posIterator = new PositionIterator();

        @Override
        public boolean hasNext() {
            return posIterator.hasNext();
        }

        @Override
        public E next() {
            // Get the Position (Node) and extract the element
            return posIterator.next().getElement();
        }

        @Override
        public void remove() {
            posIterator.remove();
        }
    }
    // ----------------- END OF NEW NESTED CLASS -----------------


    // ------------------- MODIFIED METHOD -------------------
    public Iterator<E> iterator() {
        // Change the return type and the object returned!
        return new ElementIterator(); // Use the new iterator that returns E (the element)
    }

    // NOTE: We no longer implement Iterable<Position<E>> and do not need positions() or PositionIterable
    // However, if you want to keep positions(), it should be fine as a separate utility.
    public Iterable<Position<E>> positions() {
        return new PositionIterable();
    }


    private class PositionIterable implements Iterable<Position<E>> {
        @Override
        public Iterator<Position<E>> iterator() {
            return new PositionIterator();
        }
    }


    private class PositionIterator implements Iterator<Position<E>> {
        private Node<E> cursor = header.getNext();  // node to report next
        private Node<E> recent = null;              // last reported node


        @Override
        public boolean hasNext() {
            return cursor != trailer;
        }


        @Override
        public Position<E> next() {
            if (cursor == trailer) throw new NoSuchElementException();
            recent = cursor;
            cursor = cursor.getNext();
            return recent;
        }


        @Override
        public void remove() {
            if (recent == null) throw new IllegalStateException();
            LinkedPositionalList.this.remove(recent);
            recent = null;
        }
    }


    // nested Node class
    private static class Node<E> implements Position<E> {
        private E element;
        private Node<E> prev;
        private Node<E> next;

        public Node(E e, Node<E> p, Node<E> n) {
            element = e;
            prev = p;
            next = n;
        }

        @Override
        public E getElement() { return element; }

        public Node<E> getPrev() { return prev; }
        public Node<E> getNext() { return next; }

        public void setPrev(Node<E> p) { prev = p; }
        public void setNext(Node<E> n) { next = n; }
    } //----------- end of nested Node class -----------


    private Node<E> header;
    private Node<E> trailer;
    private int size = 0;

    public LinkedPositionalList() {
        header = new Node<>(null, null, null);
        trailer = new Node<>(null, header, null);
        header.setNext(trailer);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<E> current = header.getNext(); // Start after header
        while (current != trailer) {
            sb.append(current.element);
            if (current.getNext() != trailer) sb.append(", ");
            current = current.getNext();
        }
        sb.append("]");
        return sb.toString();
    }


    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public Position<E> first() {
        if (isEmpty()) return null;
        return position(header.getNext());
    }
    public Position<E> last() {
        if (isEmpty()) return null;
        return position(trailer.getPrev());
    }
    @Override
    public Position<E> before(Position<E> p) {
        Node<E> node = isvalid(p);
        if(node.getPrev() == header){
            return null;
        }
        else{
            return position(node.getPrev());
        }
    }
    @Override
    public Position<E> after(Position<E> p) {
        Node<E> node = isvalid(p);
        if(node.getNext() == trailer){
            return null;
        }
        else{
            return position(node.getNext());
        }
    }

    //update methods
    public void addFirst(E e) {
        addBetween(e, header, header.getNext());
    }
    public Position<E> addLast(E e) {
        return addBetween(e, trailer.getPrev(), trailer);
    }
    public E removeFirst() {
        if (isEmpty()) return null;
        return remove(header.getNext());
    }
    public E removeLast() {
        if (isEmpty()) return null;
        return remove(trailer.getPrev());
    }
    @Override
    public void addBefore(Position<E> p, E e) {
        Node<E> node = isvalid(p);
        addBetween(e, node.getPrev(), node);
    }
    @Override
    public void addAfter(Position<E> p, E e) {
        Node<E> node = isvalid(p);
        addBetween(e, node, node.getNext());
    }


    //helper method
    private Position<E> addBetween(E e, Node<E> predecessor, Node<E> successor) {
        Node<E> newest = new Node<>(e, predecessor, successor);
        predecessor.setNext(newest);
        successor.setPrev(newest);
        size++;
        return newest;
    }
    private Position<E> position(Node<E> node) {
        if (node == header || node == trailer) return null;
        return node;
    }
    private E remove(Node<E> node) {
        Node<E> predecessor = node.getPrev();
        Node<E> successor = node.getNext();
        predecessor.setNext(successor);
        successor.setPrev(predecessor);
        size--;
        return node.getElement();
    }


    @Override
    public E set(Position<E> p, E e) {
        Node<E> node = isvalid(p);
        E old = node.getElement();
        node.element = e;
        return old;
    }
    @Override
    public E remove(Position<E> p) {
        Node<E> node = isvalid(p);
        return remove(node);
    }
    private Node<E> isvalid(Position<E> p) {
        if (!(p instanceof Node)) throw new IllegalArgumentException("Invalid position");
        Node<E> node = (Node<E>) p;
        if (node.getNext() == null) throw new IllegalStateException("Position is no longer in the list");
        return node;
    }
}