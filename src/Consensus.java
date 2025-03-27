import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.stream.Collectors;

public class Consensus {

    private int processID;
    private int leaderID;
    private int ets;

    private int _nodes;
    private int _fs;

    private AuthenticatedPerfectLink comms;

    private int valts; // time stamp of value
    private String val;
    private Writeset writeSet;
    private State _state;

    private Map<Integer, String> _collected;
    private boolean _collectedSent;
    private Map<Integer, String> _written;
    private Map<Integer, String> _accepted;
    private String value;

    // mudancas joao
    private AddressBook addressBook;
    private RSAPrivateKey privateKey;

    public void ByzantineEpochConsensus(
            int ets, AuthenticatedPerfectLink apl, AddressBook addressBook, RSAPrivateKey privateKey
    ){
        for (AddressRecord record: addressBook.getAddressRecords()) {
            if (record.isLeader()) {
                this.leaderID = record.getNodeId();
            }
        }
        this.addressBook = addressBook;
        this.processID = addressBook.getOwnerId();
        this.ets = ets;
        _written = new HashMap<>();
        _accepted = new HashMap<>();
        this.value = "UNDEFINED";
        this.comms = apl;
        _nodes = addressBook.getAddressRecords().size()-1; // total nodes - client
        _fs = (_nodes - 1)/3;
        this.privateKey=privateKey;
        _state = new State(0, "", new Writeset());
        _collected = new HashMap<>();
        _collectedSent = false;
        valts = 0;
    }

    public void init(String value){
        // Only leader runs this
        _state.setVal(value);

        for(int i=0; i<_nodes; i++){
            _written.put(i, "UNDEFINED");
            _accepted.put(i, "UNDEFINED");
        }
        // Leader method to start read phase
        List<AddressRecord> records=  addressBook.getAddressRecords();
        for (AddressRecord record : records) {
            if (!record.isClient()) {
                Thread nodeLoginThread = new Thread(() -> {
                    try {
                        String message1 = BlockMessage.createMessage("READ", ets, "", false, privateKey);

                        comms.sendMessage(message1, record.getReceiverPort());
                    } catch (Exception e) {
                        System.out.println("Failed sending read");
                    }
                });
                nodeLoginThread.start();
            }
        }

        // puts own state on collected
      //  String message = BlockMessage.createMessage("STATE", ets, _state.getVal()+_state.getWriteset().writesetToString(), true, privateKey);
     //   _collected.put(processID, message);

    }

    public void onRead(){
       // if ( processID != leaderID) {
            String message = BlockMessage.createMessage("STATE", ets,  _state.getValts() +"," + _state.getVal()+","+_state.getWriteset().writesetToString(), true, privateKey);
            comms.sendMessage(message, addressBook.getRecordById(leaderID).getReceiverPort());
        //}

    }
    public void propose(String value){
    /*
        if (this.value.equals("UNDEFINED")){
            this.value = value;
        }

        Message message = new ReadMessage(value, processID);

        for(AuthenticatedPerfectLink al : this.comms){
            al.sendMessage(message.toString(), 1234);
        } */
    }
    public void onState(AuthenticatedPerfectLinkOutput message ) {
        if (processID == this.leaderID) {
            if (!_collected.containsKey(message.nodeId())){
                _collected.put(message.nodeId(), message.content());
            }
            if (_collected.size()>(_nodes + _fs)/2 && !_collectedSent){
                _collectedSent=true;
                List<AddressRecord> records=  addressBook.getAddressRecords();
                for (AddressRecord record : records) {
                    if (!record.isClient()) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                String stringCollected = _collected.entrySet().stream()
                                        .map(entry -> "<" + entry.getKey() + "," + entry.getValue() + ">")
                                        .collect(Collectors.joining(""));
                                String message1 = BlockMessage.createMessage("COLLECTED", ets, stringCollected, false, privateKey);
                                comms.sendMessage(message1, record.getReceiverPort());
                            } catch (Exception e) {
                                System.out.println("Failed sending COLLECTED");
                            }
                        });
                        nodeLoginThread.start();
                    }
                }

            //    System.out.println("colected"+ _collected);
            }
        }
    }

    public void onCollected(String message){
        Map<Integer, String[]> matches = CollectedStateRegex.MatchState(message);
        // Unpacks collected state into Map and checks if state is correctly signed
        if (processID ==2) {
            for (Map.Entry<Integer, String[]> match : matches.entrySet()) {
                String state = match.getValue()[0];
                String hash = match.getValue()[1];
                Integer nodeId = match.getKey();
                System.out.println("matches "+ match.getKey() + " "  + state + " " + hash);
                boolean stateIsVerified = MessageSigner.verifySignature(state, hash, KeyLoader.loadPublicKeyFromFile(nodeId));
               if (!stateIsVerified) {
                    System.out.println("collected is tampered, leader change needed!");
                    return;
               }
            }
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
        
 /*       _written.put(processID, value);

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

            Message message = new AcceptMessage(value, processID);

            for(AuthenticatedPerfectLink al : this.comms){
                al.sendMessage(message.toString(), 1234);
            }

        }*/
    }



    public void onReceiveAccepted(int processID, String value){
        
       /* _accepted.put(processID, value);

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
        }*/
    }

    public void decide(String value){
        //logic for deciding
        System.out.println(value);
    }

}