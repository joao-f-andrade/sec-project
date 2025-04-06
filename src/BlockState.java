import java.util.ArrayList;

public class BlockState {
    private boolean _isBusy;
    private int _consensusInstance;
    private final ArrayList<String> _proposeQueue;
    BlockState(){
        _isBusy=false;
        _consensusInstance=0;
        _proposeQueue=new ArrayList<>();
    }

    public boolean getIsBusy(){
        return _isBusy;
    }

    public int getConsensusInstance(){
        return _consensusInstance;
    }

    public ArrayList<String> getProposeQueue(){
        return _proposeQueue;
    }

    public void setIsBusy(boolean value){
        _isBusy = value;
    }

    public void incrementConsensusInstance(){
        _consensusInstance++;
    }

    public void addToQueue(String value){
        _proposeQueue.add(value);
    }

    public void removedFirstFromQueue(){
        _proposeQueue.removeFirst();
    }
}
