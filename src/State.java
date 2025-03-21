public class State{

    private int _valts;
    private String _val;
    private Writeset _writeSet;

    public State(int valts, String val, Writeset writeSet){
        _valts = valts;
        _val = val;
        _writeSet = writeSet;
    }

    public int getValts(){
        return _valts;
    }

    public String getVal(){
        return _val;
    }

    public Writeset getWriteset(){
        return _writeSet;
    }

    public void setValts(int timestamp){
        _valts = timestamp;
    }

    public void setVal(String value){
        _val = value;
    }

    public void setWriteset(int timestamp, String value){
        _writeSet.addElement(timestamp, value);
    }
}