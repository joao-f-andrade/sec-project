import java.util.HashSet;
import java.util.Set;

public class Writeset{

    Set<Pair<Integer, String>> _writeset;

    public Writeset(){
        _writeset = new HashSet<>();
    }

    public void addElement(Integer timestamp, String value){
        Pair element = new Pair(timestamp, value);
        _writeset.add(element);
    }

    public String writesetToString(){
        String string = "{";
        for (Pair<Integer, String> pair : _writeset){
            string = "<"+pair.key+","+pair.value+">";
        }
        return string + "}";
    }

    //Helper Class, no need to look at it   
    static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return key.equals(pair.key) && value.equals(pair.value);
        }

        @Override
        public int hashCode() {
            return key.hashCode() + value.hashCode();
        }
    }
}