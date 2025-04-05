import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Writeset {

    Set<Pair<Integer, String>> _writeset;

    public Writeset() {
        _writeset = new HashSet<>();
    }

    public static Writeset fromJson(JSONArray json) {
        Writeset writeset = new Writeset();
        for (int i = 0; i < json.length(); i++) {
            JSONObject obj = json.getJSONObject(i);
            int timestamp = obj.getInt("ts");
            String value = obj.getString("value");
            writeset.addElement(timestamp, value);
        }
        return writeset;
    }

    public void addElement(Integer timestamp, String value) {
        Pair element = new Pair(timestamp, value);
        _writeset.add(element);
    }

    public Set<Pair<Integer, String>> getWriteSet() {
        return _writeset;
    }

    public JSONArray toJson() {
        JSONArray jsonArray = new JSONArray();
        for (Pair<Integer, String> pair : _writeset) {
            JSONObject object = new JSONObject();
            object.put("ts", pair.getKey());
            object.put("value", pair.getValue());
            jsonArray.put(object);
        }
        return jsonArray;
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