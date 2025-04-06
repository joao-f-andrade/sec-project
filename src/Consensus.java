import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class Consensus {

    private final int _processID;
    private int _leaderID;
    private final int _nodes;
    private final int _fs;
    private final AuthenticatedPerfectLink _comms;
    private State _state;
    private Map<Integer, StateMessage> _collectedState; // collected map for leader to broadcast
    private boolean _collectedSent;
    private Map<Integer, String> _written;
    private boolean _writtenSent;
    private Map<Integer, String> _accepted;
    private boolean _acceptedSent;
    private final AddressBook _addressBook;
    private final RSAPrivateKey _privateKey;
    public BlockState _blockState;
    private boolean _hasDecided;
    private int _ets;

    public Consensus(AuthenticatedPerfectLink apl, AddressBook addressBook, RSAPrivateKey privateKey, BlockState blockState) {
        for (AddressRecord record : addressBook.getAddressRecords()) {
            if (record.isLeader()) {
                _leaderID = record.getNodeId();
            }
        }
        _addressBook = addressBook;
        _processID = addressBook.getOwnerId();
        _comms = apl;
        _nodes = addressBook.getAddressRecords().size() - 1; // total nodes - client
        _fs = (_nodes - 1) / 3;
        _privateKey = privateKey;
        _blockState = blockState;
    }

    public void init() {
        //System.out.println("init start");
        _collectedState = new HashMap<>();
        _collectedSent = false;
        _writtenSent = false;
        _acceptedSent = false;
        _ets=0;
        _state = new State(0, "UNDEFINED", 0, new Writeset());
        _written = new HashMap<>();
        _accepted = new HashMap<>();
        _hasDecided=false;

    }

    public void propose(String value){
        _state.setVal(value);
        List<AddressRecord> records = _addressBook.getAddressRecords();

        for (AddressRecord record : records) {
                Thread nodeLoginThread = new Thread(() -> {
                    try {
                        ReadMessage readMessage = new ReadMessage("READ", record.getNodeId(), _blockState.getConsensusInstance());
                        _comms.sendMessage(readMessage.toJsonString(), record.getReceiverPort());
                    } catch (Exception e) {
                        System.out.println("Failed sending read");
                    }
                });
                nodeLoginThread.start();
        }
    }

    public void onRead() {
        StateMessage message = new StateMessage(_processID, _blockState.getConsensusInstance(), _state);
        String signedMessage = message.createSignedJsonString(_privateKey);
        _comms.sendMessage(signedMessage, _addressBook.getRecordById(_leaderID).getReceiverPort());
    }

    public void onState(AuthenticatedPerfectLinkOutput message, JSONObject jsonMessage) {
        if (_processID == _leaderID) {
            StateMessage stateMessage = StateMessage.fromJson(message.content());
            boolean isVerified = stateMessage.verifyStateHash();
            if (!_collectedState.containsKey(message.nodeId()) && isVerified) {
                _collectedState.put(stateMessage._senderId, stateMessage);
            }
            if (_collectedState.size()>4 && !_collectedSent){
                _collectedSent = true;
                List<AddressRecord> records = _addressBook.getAddressRecords();
                CollectedMessage collectedMessage = new CollectedMessage(_processID, _blockState.getConsensusInstance(), _collectedState);
                for (AddressRecord record : records) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                _comms.sendMessage(collectedMessage.toJsonString(), record.getReceiverPort());
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

    public void onCollected(AuthenticatedPerfectLinkOutput rawMessage) {
        if (rawMessage.nodeId() == _leaderID) { // Only accepts Collected events from leader
            JSONObject json = new JSONObject(rawMessage.content());
            JSONArray collectedStateJson = json.getJSONArray("collectedStateList");
            // Verifies signatures
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
                List<AddressRecord> records = _addressBook.getAddressRecords();
                for (AddressRecord record : records) {
                        Thread nodeLoginThread = new Thread(() -> {
                            try {
                                String val = findBySenderId(collectedStateJson, 0);
                                WriteMessage writeMessage = new WriteMessage(_processID, _blockState.getConsensusInstance(), val); // todo fazer o predicado
                                _comms.sendMessage(writeMessage.toJsonString(), record.getReceiverPort());
                            } catch (Exception e) {
                                System.out.println("Failed sending WRITE");
                                e.printStackTrace();
                            }
                        });
                        nodeLoginThread.start();
                }
            }
        }
    }

    public void onWrite(JSONObject objectMessage) {
        String val = objectMessage.getString("val");
        int senderId = objectMessage.getInt("senderId");
        _written.put(senderId, val);

        MajorityCheckResult majority=getMajorityValue(_written);

        if (majority.found()  && !_acceptedSent) {
            _acceptedSent = true;
            _state.setValts(_ets);
            _state.setVal(majority.value());
            List<AddressRecord> records = _addressBook.getAddressRecords();
            AcceptMessage acceptMessage = new AcceptMessage(_processID, _blockState.getConsensusInstance(), majority.value());
            for (AddressRecord record : records) {
                    Thread nodeLoginThread = new Thread(() -> {
                        try {
                            _comms.sendMessage(acceptMessage.toJsonString(), record.getReceiverPort());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Failed sending ACCEPTED");
                        }
                    });
                    nodeLoginThread.start();
            }
        }

    }


    public void onAccepted(JSONObject objectMessage) {
        //System.out.println("onAccepted "+ objectMessage);
        String val = objectMessage.getString("val");
        int senderId = objectMessage.getInt("senderId");
        _accepted.put(senderId, val);
        MajorityCheckResult majority=getMajorityValue(_accepted);
        if (majority.found() && !_hasDecided) {
            _hasDecided=true;
            decide(majority.value());
        }
    }

    public void decide(String value) {
        //logic for deciding
        System.out.println("decided " + value + " in " + _processID);
        _blockState.incrementConsensusInstance();
        _blockState.removedFirstFromQueue();
        _blockState.setIsBusy(false);
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

    private MajorityCheckResult getMajorityValue(Map<Integer, String> map) {
        int threshold = (_nodes + _fs) / 2;
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String value : map.values()) {
            int count = frequencyMap.getOrDefault(value, 0) + 1;
            if (count > threshold) {
                return new MajorityCheckResult(true, value);
            }
            frequencyMap.put(value, count);
        }

        return new MajorityCheckResult(false, null);    }
}
record MajorityCheckResult(boolean found, String value) {}

