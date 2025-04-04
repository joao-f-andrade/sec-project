import org.json.JSONObject;

public class State{

    private int _valts;
    private String _val;
    private int _ets;
    private Writeset _writeSet;

    public State(int valts, String val, int ets, Writeset writeSet){
        _valts = valts;
        _val = val;
        _ets = ets;
        _writeSet = writeSet;
    }

    public static State fromJson(JSONObject json){
        int valts = json.getInt("valts");
        int ets = json.getInt("ets");
        String val = json.getString("val");
        Writeset writeSet = Writeset.fromJson( json.getJSONArray("writeSet"));
        return new State(valts, val, ets, writeSet);
    }

    public JSONObject toJson(){
        JSONObject jsonState = new JSONObject();
        jsonState.put("valts", _valts);
        jsonState.put("ets", _ets);
        jsonState.put("val", _val);
        jsonState.put("writeSet", _writeSet.toJson());
        return jsonState;
    }

    public int getValts(){
        return _valts;
    }

    public String getVal(){
        return _val;
    }

    public int getEts() { return _ets; }

    public Writeset getWriteset(){
        return _writeSet;
    }

    public void setValts(int timestamp){
        _valts = timestamp;
    }

    public void setVal(String value){
        _val = value;
    }

    public void setEts(int ets) { _ets = ets; }

    public void setWriteset(int timestamp, String value){
        _writeSet.addElement( timestamp, value );
    }
}