import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import org.json.JSONArray;
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
    private Map<Integer, String> _written;
    private boolean _writtenSent;
    private Map<Integer, String> _accepted;
    private boolean _acceptedSent;
    private int _consensusInstance;

    private AddressBook addressBook;
    private RSAPrivateKey privateKey;

    public void ByzantineEpochConsensus(AuthenticatedPerfectLink apl, AddressBook addressBook, RSAPrivateKey privateKey, int consensusInstance) {
        for (AddressRecord record : addressBook.getAddressRecords()) {
            if (record.isLeader()) {
                this.leaderID = record.getNodeId();
            }
        }
        this.addressBook = addressBook;
        this.processID = addressBook.getOwnerId();
        _written = new HashMap<>();
        _accepted = new HashMap<>();
        this.comms = apl;
        _nodes = addressBook.getAddressRecords().size() - 1; // total nodes - client
        _fs = (_nodes - 1) / 3;
        this.privateKey = privateKey;
        _collectedState = new HashMap<>();
        _collectedSent = false;
        _writtenSent = false;
        _state = new State(0, "UNDIFINED", 0, new Writeset());
        _state.setVal("UNDEFINED");
        _jsonCollected = new JSONObject();
        _consensusInstance = consensusInstance;
    }

    public void init(String value) {
        // Only leader runs this
        _state.setVal(value);
        _state.getWriteset().addElement(processID, "Porto"+processID);
        // Leader method to start read phase
        List<AddressRecord> records = addressBook.getAddressRecords();
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
    }

    public void onRead() {
        StateMessage message = new StateMessage(processID, _consensusInstance, _state);
        String signedMessage = message.createSignedJsonString(privateKey);
        comms.sendMessage(signedMessage, addressBook.getRecordById(leaderID).getReceiverPort());
    }

    public void onState(AuthenticatedPerfectLinkOutput message, JSONObject jsonMessage) {
        if (processID == this.leaderID) {
            StateMessage stateMessage = StateMessage.fromJson(message.content());
            boolean isVerified = stateMessage.verifyStateHash();
            if (!_collectedState.containsKey(message.nodeId()) && isVerified) {
                _collectedState.put(stateMessage._senderId, stateMessage);
            }
            //if (_collectedState.size()>(_nodes + _fs)/2 && !_collectedSent){ //todo alterar para esta linha
            if (_collectedState.size() > 4 && !_collectedSent) {
                _collectedSent = true;
                List<AddressRecord> records = addressBook.getAddressRecords();
                CollectedMessage collectedMessage = new CollectedMessage(processID, _consensusInstance, _collectedState);
                for (AddressRecord record : records) {
                    if (!record.isClient()) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                comms.sendMessage(collectedMessage.toJsonString(), record.getReceiverPort());
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

    public void onCollected(AuthenticatedPerfectLinkOutput rawMessage) {
        if (rawMessage.nodeId() == 1) { // Only accepts Collected events from leader
            JSONObject json = new JSONObject(rawMessage.content());
            JSONArray collectedStateJson = json.getJSONArray("collectedStateList");
            for (int i = 0; i < collectedStateJson.length(); i++) {
                JSONObject jsonStateMessage = collectedStateJson.getJSONObject(i);
                String hash = jsonStateMessage.getString("hash");
                int id = jsonStateMessage.getInt("senderId");
                RSAPublicKey key = KeyLoader.loadPublicKeyFromFile(id);

                boolean isVerified = MessageSigner.verifySignature(jsonStateMessage.getJSONObject("state").toString(), hash, key);
                if (!isVerified) {
                    throw new RuntimeException("collected is tampered, leader change needed!");
                }
            }
            if (!_writtenSent) {
                _writtenSent = true;
                List<AddressRecord> records = addressBook.getAddressRecords();
                for (AddressRecord record : records) {
                    if (!record.isClient()) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                String val = findBySenderId(collectedStateJson, 1);
                                WriteMessage writeMessage = new WriteMessage(processID, _consensusInstance, val); // todo fazer o predicado
                                comms.sendMessage(writeMessage.toJsonString(), record.getReceiverPort());
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

    // todo fazer verificacao de quorum
    public void onWrite(JSONObject objectMessage) {
        String val = objectMessage.getString("val");
        int senderId = objectMessage.getInt("senderId");
        _written.put(senderId, val);
        if (_written.size() > 4 && !_acceptedSent) {
            _acceptedSent = true;
            List<AddressRecord> records = addressBook.getAddressRecords();
            AcceptMessage acceptMessage = new AcceptMessage(processID, _consensusInstance, _written.get(1));
            for (AddressRecord record : records) {
                if (!record.isClient()) {
                    Thread nodeLoginThread = new Thread(() -> {
                        try {
                            comms.sendMessage(acceptMessage.toJsonString(), record.getReceiverPort());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Failed sending ACCEPTED");
                        }
                    });
                    nodeLoginThread.start();
                }
            }
        }

    }


    public void onAccepted(JSONObject objectMessage) {
        String val = objectMessage.getString("val");
        int senderId = objectMessage.getInt("senderId");
        _accepted.put(senderId, val);
        if (_accepted.size() > 4) {
            decide(val);
        }
       /* int count = 0;

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
        */
    }

    public void decide(String value) {
        //logic for deciding
        System.out.println("decided " + value + " in " + processID);
    }

    //todo apagar
    private String findBySenderId(JSONArray array, int senderId) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject state = array.getJSONObject(i);
            if (state.getInt("senderId") == senderId) {
                return state.getJSONObject("state").getString("val");
            }
        }
        return ""; // not found
    }

}