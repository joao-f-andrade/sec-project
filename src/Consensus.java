import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Consensus{

    private int processID;
    private int leaderID;
    private int ets;

    private int _nodes;
    private int _fs;

    private List<AuthenticatedPerfectLink> comms;
    //conditional collect variable

    private int valts;
    private String val;
    private Writeset writeSet;

    private State _state;

    private Map<Integer, String> _written;
    private Map<Integer, String> _accepted;

    private String value;

    public ByzantineEpochConsensus(int processID, int leaderId, int ets, List<AuthenticatedPerfectLink> apl){
        this.processID = processID;
        this.leaderId = leaderId;
        this.ets = ets;
        _written = new HashMap<>();
        _accepted = new HashMap<>();
        this.value = "UNDEFINED";
        this.comms = apl;
        _nodes = this.comms.size();
        _fs = (_nodes - 1)/3
        //this.cc = cc;
    }

    public void init(State state){
        
        _state = state;

        for(int i=0; i<comms.size(); i++){
            _written.put(i, "UNDEFINED");
            _accepted.put(i, "UNDEFINED");
        }
    }

    public void propose(String value){

        if (this.value.equals("UNDEFINED")){
            this.value = value;
        }

        Message message = new ReadMessage(value, processID);

        for(AuthenticatedPerfectLink al : this.comms){
            al.sendMessage(message.toString(), 1234);
        }
    }

    public void onReading(int processID){
        if(processID == this.leaderID){
            //cc.Input
        }
    }

    public void onCollecting(List<State> states){

        String tmpVal = "UNDEFINED";

        /*
        *TODO: need to finish this buy building the correct predicates described in the book
        *
        *quorumhighest() 
        *certifiedvalue() 
        *#(S) 
        * 
        */
    }

    public void onReceiveWrite(int processID, String value){
        
        _written.put(processID, value);

        int count = 0;

        for(String values : _written.values()){
            if(values.equals(value)){
                count++;
            }
        }

        if(count > (_nodes + _fs)/2){
            _state.setValts(ets);
            _state.setVal(value);

            for(int i = 0; i<_nodes; i++){
                _written.put(i, "UNDEFINED");
            }

            Message message = new AcceptMessage(value, processID):

            for(AuthenticatedPerfectLink al : this.comms){
                al.sendMessage(message.toString(), 1234);
            }

        }
    }



    public void onReceiveAccepted(int processID, String value){
        
        accepted.put(processID, value);

        int count = 0;

        for(String values : _accepted.values()){
            if(values.equals(value)){
                count++;
            }
        }

         if(count > (_nodes + _fs)/2){

            for(int i = 0; i<_nodes; i++){
                _accepted.put(i, "UNDEFINED");
            }
            decide(value);
        }
    }

    public void decide(String value){
        //logic for deciding
        System.out.println(value);
    }

}