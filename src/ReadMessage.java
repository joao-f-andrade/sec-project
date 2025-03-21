public class ReadMessage extends Message{

    public ReadMessage(String value, int processID){
        super(value, processID);
    }

    @Override
    public String toString(){
        return "READ|" + value + "|" + processID;
    }
}