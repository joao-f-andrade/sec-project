import java.security.interfaces.RSAPrivateKey;
import java.util.*;

import org.json.JSONObject;

public class Consensus {

    private int processID;
    private int leaderID;
    private int _nodes;
    private int _fs;

    private AuthenticatedPerfectLink comms;

    private State _state;

    private Map<Integer, StateMessage> _collectedState; // collected map for leader to broadcast
    private JSONObject _jsonCollected;
    private boolean _collectedSent;
    private Writeset _written;
    private boolean _writtenSent;
    private Map<Integer, String> _accepted;
    private int _consensusInstance;

    private AddressBook addressBook;
    private RSAPrivateKey privateKey;

    public void ByzantineEpochConsensus(AuthenticatedPerfectLink apl, AddressBook addressBook, RSAPrivateKey privateKey, int consensusInstance
    ){
        for (AddressRecord record: addressBook.getAddressRecords()) {
            if (record.isLeader()) {
                this.leaderID = record.getNodeId();
            }
        }
        this.addressBook = addressBook;
        this.processID = addressBook.getOwnerId();
        _written = new Writeset();
        _accepted = new HashMap<>();
        this.comms = apl;
        _nodes = addressBook.getAddressRecords().size()-1; // total nodes - client
        _fs = (_nodes - 1)/3;
        this.privateKey=privateKey;
        _collectedState = new HashMap<>();
        _collectedSent = false;
        _writtenSent = false;
        _state= new State(-1, "UNDIFINED",0, new Writeset() );
        _state.setVal("UNDEFINED");
        _jsonCollected = new JSONObject();
        _consensusInstance = consensusInstance;
    }

    public void init(String value){
        // Only leader runs this
        _state.setVal(value);

        for(int i=0; i<_nodes; i++){
            _written.addElement(0, "UNDEFINED");
            _accepted.put(0, "UNDEFINED");
        }
        // Leader method to start read phase
        List<AddressRecord> records=  addressBook.getAddressRecords();
        for (AddressRecord record : records) {
            if (!record.isClient()) {
                Thread nodeLoginThread = new Thread(() -> {
                    try {
                        ReadMessage readMessage = new ReadMessage("READ", record.getNodeId(), _consensusInstance);
                        //String message1 = BlockMessage.createMessage("READ", _consensusInstance, "", false, privateKey);

                        comms.sendMessage(readMessage.toJsonString(), record.getReceiverPort());

                        //comms.sendMessage(readMessage.messageToJsonString(), record.getReceiverPort());
                    } catch (Exception e) {
                        System.out.println("Failed sending read");
                    }
                });
                nodeLoginThread.start();
            }
        }

        // puts own state on collected
      //  String message = BlockMessage.createMessage("STATE", ets, _state.getVal()+_state.getWriteset().writesetToString(), true, privateKey);
     //   collectedState.put(processID, message);

    }

    public void onRead(){
        /*JSONObject jsonContent = new JSONObject();
        jsonContent.put("val", _state.getVal());
        jsonContent.put("valts", _state.getValts());
        jsonContent.put("writeset", _state.getWriteset().writesetToString());
        String stringContent = jsonContent.toString();

        String message = BlockMessage.createMessage("STATE", _consensusInstance, stringContent, true, privateKey);
        comms.sendMessage(message, addressBook.getRecordById(leaderID).getReceiverPort());
       */
        StateMessage message = new StateMessage(processID, _consensusInstance, _state);
        String signedMessage = message.createSignedJsonString(privateKey);
        comms.sendMessage(signedMessage, addressBook.getRecordById(leaderID).getReceiverPort());

    }

    public void onState(AuthenticatedPerfectLinkOutput message, JSONObject jsonMessage ) {
        if (processID == this.leaderID) {
            StateMessage stateMessage = StateMessage.fromJson(message.content());
            boolean isVerified = stateMessage.verifyStateHash();
            if (!_collectedState.containsKey(message.nodeId()) && isVerified){
                _collectedState.put(stateMessage._senderId, stateMessage);
                            /*
                String[] messageAndHash = BlockMessage.splitSignatureMessage(message.content());
                boolean stateIsVerified = MessageSigner.verifySignature(messageAndHash[0], messageAndHash[1], KeyLoader.loadPublicKeyFromFile(message.nodeId()));
                    if( stateIsVerified) {
                        _collectedState.put(message.nodeId(), messageAndHash[0]);
                        _jsonCollected.put(Integer.toString(message.nodeId()) , message.content());
                    }*/
            }
            //if (_collectedState.size()>(_nodes + _fs)/2 && !_collectedSent){ //todo alterar para esta linha
            if (_collectedState.size()>4 && !_collectedSent){
                _collectedSent=true;
                List<AddressRecord> records=  addressBook.getAddressRecords();
                for (AddressRecord record : records) {
                    if (!record.isClient()) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                String message1 = BlockMessage.createMessage("COLLECTED", _consensusInstance, _jsonCollected.toString(), false, privateKey);
                                comms.sendMessage(message1, record.getReceiverPort());
                            } catch (Exception e) {
                                e.printStackTrace(); // Prints the full stack trace to standard error
                                System.out.println("Failed sending COLLECTED");
                            }
                        });
                        nodeLoginThread.start();
                    }
                }
            }
        }
    }

    public void onCollected(Object objectMessage){
        if (processID ==2) { // Only accepts Collected events from leader
            Map<Integer, JSONObject> collectedWrite = new HashMap<>();

            JSONObject jsonCollected = BlockMessage.decodeCollectedMessage(objectMessage);
            Iterator<String> keys = jsonCollected.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonCollected.get(key);
                String[] stateAndHash = BlockMessage.splitSignatureMessage(value.toString());
                boolean stateIsVerified = MessageSigner.verifySignature(stateAndHash[0], stateAndHash[1], KeyLoader.loadPublicKeyFromFile(Integer.parseInt(key)));

                if (!stateIsVerified) {
                    System.out.println("collected is tampered, leader change needed!");
                    return;
                } else {
                    JSONObject jsonState = new JSONObject(stateAndHash[0]);
                    collectedWrite.put(Integer.parseInt(key), jsonState);
                }
            }
        //if ( _collectedWrite.size()> (_nodes + _fs)/2 && !_writtenSent){ //todo alterar para isto
            if ( collectedWrite.size()>4 && !_writtenSent){
            _writtenSent = true;
            List<AddressRecord> records=  addressBook.getAddressRecords();
            for (AddressRecord record : records) {
                if (!record.isClient()) {
                    Thread nodeLoginThread = new Thread(() -> {
                        try {
                            String message1 = BlockMessage.createMessage("WRITE", _consensusInstance, collectedWrite.get(1).toString(), false, privateKey); // todo fazer o predicado
                            comms.sendMessage(message1, record.getReceiverPort());
                        } catch (Exception e) {
                            System.out.println("Failed sending WRITE");
                            e.printStackTrace(); // Prints the full stack trace to standard error
                        }
                    });
                    nodeLoginThread.start();
                }
            }
        }
        }
    }

    public void onReceiveWrite(Object objectMessage){
        
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