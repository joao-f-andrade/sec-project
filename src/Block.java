import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Set;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>();
    private final int receiverPort;
    private final boolean isLeader;
    private final boolean isClient;
    private final RSAPrivateKey rsaPrivateKey;
    private final int blockId;
    private final ArrayList<String> state;
    private final Set<BlockMessageStorage> blockMessageStorageSet = new HashSet<>();


    public Block(int nodeId, AddressBook addressBook) throws Exception {
        // Setup of block ad addressbook properties
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);
        receiverPort = addressRecord.getReceiverPort();
        isLeader = addressRecord.isLeader();
        isClient = addressRecord.isClient();
        blockId = addressRecord.getNodeId();
        state = new ArrayList<>();
        rsaPrivateKey = KeyLoader.loadPrivateKeyFromFile(nodeId);

        // start perfect link, message listener and logic thread
        authenticatedLink = new AuthenticatedPerfectLink(blockId, receiverPort , addressRecord.getAddress());
        startReceiver();
        if ( this.addressBook.getOwnerId() == 0 ) {
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
                logic.ByzantineEpochConsensus(0, authenticatedLink, addressBook, rsaPrivateKey);
                while (true) {
                    AuthenticatedPerfectLinkOutput rawMessage = authenticatedLink.getReceivedMessage();
                    String[] messageAndHash = BlockMessage.splitSignatureMessage(rawMessage.content());

                    // check if message is correctly signed
                    boolean isCorrectlySigned = false;
                    if (!messageAndHash[1].isEmpty()) {
                        RSAPublicKey publicKey = KeyLoader.loadPublicKeyFromFile(rawMessage.nodeId());
                        isCorrectlySigned = MessageSigner.verifySignature(messageAndHash[0], messageAndHash[1], publicKey);
                    }
                    String[] parts = BlockMessage.decodeMessage(messageAndHash[0]);

                    /*
                    if ( blockId == 1){
                        System.out.println("no leader "+ parts[0]);
                    }
                    */

                    // Discard duplicate messages
                    // todo por na proxima linha ets e valuets
                    BlockMessageStorage blockMessageStorage = new BlockMessageStorage( 0, Integer.parseInt(parts[1]), rawMessage.nodeId(), parts[0], parts[2]);
                    if ( !blockMessageStorageSet.contains(blockMessageStorage)) {
                        blockMessageStorageSet.add(blockMessageStorage);
                    // check if it's a message from client
                    switch (parts[0]){
                        case "APPEND":
                            if ( addressBook.getRecordById(rawMessage.nodeId()).isClient() && isCorrectlySigned) {
                                if ( isLeader ) {
                                        //TODO start consensus
                                        System.out.println("leader start consensus");
                                        logic.init(parts[2]);
                                    }
                                else {
                                    if (Objects.equals(parts[0], "APPEND")){
                                        //TODO start countdown to change leader if doesn't get message from leader
                                    }
                                }
                            }
                            break;
                        case "READ":
                            logic.onRead();
                            break;
                        case "STATE":
                            if (isCorrectlySigned) {
                                logic.onState(rawMessage);
                            }
                            break;
                            case "COLLECTED":
                                logic.onCollected(parts[2]);
                            break;
                    }


                    System.out.println("evento " + parts[0] + " na epoch " + parts[1] + " no block "+blockId+ ", " + parts[2] + " vindo de " + rawMessage.nodeId());

                    /*
                    if (isCorrectlySigned){
                        System.out.println("assinado evento " + parts[0] + " na epoch " + parts[1] + " no block "+blockId+ ", " + parts[2] + " vindo de " + rawMessage.nodeId());
                    } else {
                        System.out.println("nao assinado evento " + parts[0] + " na epoch " + parts[1] + " no block "+blockId+ ", " + parts[2] + " vindo de " + rawMessage.nodeId());
                    }
                    */}
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
                    String[] parts = BlockMessage.decodeMessage(messageAndHash[0]);
                    if (isCorrectlySigned){
                        System.out.println("cliente recebeu correctamente assinado evento " + parts[0] + " na epoch " + parts[1] + ", " + parts[2] + " vindo de " + rawMessage.nodeId());
                    } else {
                        System.out.println("cliente recebeu nao assinado evento " + parts[0] + " na epoch " + parts[1] + ", " + parts[2] + " vindo de " + rawMessage.nodeId());
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        clientLogicThread.start();
    }

    public void clientAppendRequest(String message){
        // Client method to request append of value
        List<AddressRecord> records=  addressBook.getAddressRecords();
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
    public void testSender(int port, String message){
        // Method to test sending a message
        try {
            String message1 = BlockMessage.createMessage("TEST", 0, message, false, rsaPrivateKey);
            authenticatedLink.sendMessage(message1, port);
        } catch (Exception e) {

            System.out.println("falhou envio de "+ blockId);
        }
    }
}