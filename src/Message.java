public abstract class Message{

    private String value;
    private int processID;

    public Message(String value, int processID){
        this.value = value;
        this.processID = processID;
    }

}