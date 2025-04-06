import org.json.JSONObject;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Block {
    private final AuthenticatedPerfectLink authenticatedLink;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>();
    private final boolean isLeader;
    private final Set<String> messageHistory = new HashSet<>(); // collection of new messages
    private final Consensus _consensus;
    public BlockState _blockState;

    public Block(int nodeId, AddressBook addressBook) {
        // Setup of block ad addressbook properties
        addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = addressBook.getRecordById(nodeId);
        int receiverPort = addressRecord.getReceiverPort();
        isLeader = addressRecord.isLeader();
        int blockId = addressRecord.getNodeId();
        RSAPrivateKey rsaPrivateKey = KeyLoader.loadPrivateKeyFromFile(nodeId);
        _blockState=new BlockState();

        authenticatedLink = new AuthenticatedPerfectLink(blockId, receiverPort, addressRecord.getAddress());
        _consensus = new Consensus(authenticatedLink, addressBook, rsaPrivateKey, _blockState);

        startReceiver();
        nodeLogic();
        startLoop();
    }

    private void startReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                authenticatedLink.receiver();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

    private void nodeLogic() {
        Thread nodeLoginThread = new Thread(() -> {
            try {
                while (true) {
                    AuthenticatedPerfectLinkOutput rawMessage = authenticatedLink.getReceivedMessage();
                    // Discard duplicate messages
                    if (!messageHistory.contains(rawMessage.content())) {
                        messageHistory.add(rawMessage.content());
                        JSONObject jsonMessage = BlockMessage.decodeMessage(rawMessage.content());

                        switch (jsonMessage.get("event").toString()) {
                            case "READ":
                                _consensus.onRead();
                                break;
                            case "STATE":
                                _consensus.onState(rawMessage, jsonMessage);
                                break;
                            case "COLLECTED":
                                _consensus.onCollected(rawMessage);
                                break;
                            case "WRITE":
                                _consensus.onWrite(jsonMessage);
                                //System.out.println("event " + jsonMessage.getString("event") + " em " + blockId + " " + jsonMessage);
                                break;
                            case "ACCEPT":
                                _consensus.onAccepted(jsonMessage);
                                break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        nodeLoginThread.start();
    }

    public void startConsensus(String value){
        _consensus.init();
        if (isLeader) {
            _consensus.propose(value);
        }
    }

    public void propose(String value){
        _blockState.addToQueue(value);
    }

    private void startLoop() {
        Thread loopThread = new Thread(() -> {
            while (true) {
                if (!_blockState.getIsBusy() && !_blockState.getProposeQueue().isEmpty()) {
                    _blockState.setIsBusy(true);
                    startConsensus(_blockState.getProposeQueue().getFirst());
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        loopThread.start();
    }
}