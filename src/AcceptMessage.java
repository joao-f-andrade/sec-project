public class AcceptMessage extends Message{

    public AcceptMessage(String value, int processID){
        super(value, processID);
    }

    @Override
    public String toString(){
        return "ACCEPT|" + value + "|" + processID;
    }
}