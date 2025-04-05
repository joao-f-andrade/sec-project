import org.json.JSONObject;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Set;

public class Block {
    private final AuthenticatedPerfectLink authenticatedLink;
    private final AddressBook addressBook;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>();
    private final boolean isLeader;
    private final RSAPrivateKey rsaPrivateKey;
    private final Set<BlockMessageStorage> blockMessageStorageSet = new HashSet<>(); // colection of messages received
    private final Set<String> messageHistory = new HashSet<>(); // collection of new messages

    public Block(int nodeId, AddressBook addressBook) throws Exception {
        // Setup of block ad addressbook properties
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);
        int receiverPort = addressRecord.getReceiverPort();
        isLeader = addressRecord.isLeader();
        int blockId = addressRecord.getNodeId();
        rsaPrivateKey = KeyLoader.loadPrivateKeyFromFile(nodeId);

        // start perfect link, message listener and logic thread
        authenticatedLink = new AuthenticatedPerfectLink(blockId, receiverPort, addressRecord.getAddress());
        startReceiver();
        if (this.addressBook.getOwnerId() == 0) {
            clientLogic();
        } else {
            nodeLogic();
        }
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
            // TODO add value timestamp
            try {
                Consensus logic = new Consensus();
                logic.ByzantineEpochConsensus(authenticatedLink, addressBook, rsaPrivateKey, 0); // TODO implementar logica de consensus instance
                while (true) {
                    AuthenticatedPerfectLinkOutput rawMessage = authenticatedLink.getReceivedMessage();
                    // todo por na proxima linha ets e valuets

                    // Discard duplicate messages
                    if (!messageHistory.contains(rawMessage.content())) {
                        messageHistory.add(rawMessage.content());
                        JSONObject jsonMessage = BlockMessage.decodeMessage(rawMessage.content());


                        switch (jsonMessage.get("event").toString()) {
                            case "APPEND":
                                if (isLeader) {
                                    //TODO start consensus
                                    System.out.println("leader start consensus");
                                    logic.init(jsonMessage.get("content").toString());
                                }
                                break;
                            case "READ":
                                logic.onRead();
                                break;
                            case "STATE":
                                logic.onState(rawMessage, jsonMessage);
                                break;
                            case "COLLECTED":
                                logic.onCollected(rawMessage);
                                break;
                            case "WRITE":
                                logic.onWrite(jsonMessage);
                                break;
                            case "ACCEPT":
                                logic.onAccepted(jsonMessage);
                                break;
                        }
                        // System.out.println("event " + jsonMessage.getString("event") + " em " + blockId + " " + jsonMessage);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        nodeLoginThread.start();
    }

    private void clientLogic() {
        Thread clientLogicThread = new Thread(() -> {
            try {
                while (true) {
                    AuthenticatedPerfectLinkOutput rawMessage = authenticatedLink.getReceivedMessage();

                    String[] messageAndHash = BlockMessage.splitSignatureMessage(rawMessage.content());

                    // check if message is correctly signed
                    boolean isCorrectlySigned = false;
                    if (!messageAndHash[1].isEmpty()) {
                        RSAPublicKey publicKey = KeyLoader.loadPublicKeyFromFile(rawMessage.nodeId());
                        isCorrectlySigned = MessageSigner.verifySignature(messageAndHash[0], messageAndHash[1], publicKey);
                    }
                    JSONObject jsonMessage = BlockMessage.decodeMessage(messageAndHash[0]);
                    if (isCorrectlySigned) {
                        System.out.println("cliente recebeu correctamente assinado evento " + jsonMessage.get("event") + " na epoch " + jsonMessage.get("consensusInstance") + ", " + jsonMessage.get("content") + " vindo de " + rawMessage.nodeId());
                    } else {
                        System.out.println("cliente recebeu nao assinado evento " + jsonMessage.get("event") + " na epoch " + jsonMessage.get("consensusInstance") + ", " + jsonMessage.get("content") + " vindo de " + rawMessage.nodeId());
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        clientLogicThread.start();
    }

    public void clientAppendRequest(String message) {
        // Client method to request append of value
        List<AddressRecord> records = addressBook.getAddressRecords();
        for (AddressRecord record : records) {
            if (!record.isClient()) {
                Thread nodeLoginThread = new Thread(() -> {
                    try {
                        String message1 = BlockMessage.createMessage("APPEND", 0, message, true, rsaPrivateKey);

                        authenticatedLink.sendMessage(message1, record.getReceiverPort());
                    } catch (Exception e) {
                        System.out.println("Failed sending " + message + " to " + record.getNodeId());
                    }
                });
                nodeLoginThread.start();
            }
        }
    }
}