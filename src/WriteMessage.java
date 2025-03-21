public class WriteMessage extends Message{

    public WriteMessage(String value, int processID){
        super(value, processID);
    }

    @Override
    public String toString(){
        return "WRITE|" + value + "|" + processID;
    }
}