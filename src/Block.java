import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.security.*;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>();
    private final int receiverPort;
    private final boolean isLeader;
    private final boolean isClient;
    private final Key rsaPrivateKey;
    private final int senderId;


    public Block(int nodeId, AddressBook addressBook) throws Exception {
        // Setup das propriedades de bloco e addressbook
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);
        receiverPort = addressRecord.getReceiverPort();
        isLeader = addressRecord.isLeader();
        isClient = addressRecord.isClient();
        senderId = addressRecord.getNodeId();

        // Inicia o perfect link, o listener de mensagens e a thread de logica
        authenticatedLink = new AuthenticatedPerfectLink(senderId, receiverPort , addressRecord.getAddress());
        startReceiver();
        nodeLogic();

        rsaPrivateKey = KeyLoader.loadKeyFromFile("keys/" + nodeId + "_id_rsa");
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
                    AuthenticatedPerfectLinkOutput message = authenticatedLink.getReceivedMessage();

                    System.out.println("mensagem no block "+senderId+ ", " + message.content() + " vindo de " + message.nodeId());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        nodeLoginThread.start();
    }

    public void startSender() {
        Thread senderThread = new Thread(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter node id number: ");
                int destinationId = Integer.parseInt(scanner.nextLine());
                while (true) {
                    int destinationPort = addressBook.getRecordById(destinationId).getReceiverPort();
                    System.out.print("Enter message to send: ");
                    String message = scanner.nextLine();
                    authenticatedLink.sendMessage(message,  destinationPort);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        senderThread.start();
    }

    public void testSender(int port){
        try {
            authenticatedLink.sendMessage("ola " +port, port);
        } catch (Exception e) {

            System.out.println("falhou envio de "+ senderId);
        }
    }

    public void appendRequest(String message){
        List<AddressRecord> records=  addressBook.getAddressRecords();
        for (AddressRecord record : records) {
            if (!record.isClient()) {
                Thread nodeLoginThread = new Thread(() -> {
                        try {
                            authenticatedLink.sendMessage(message, record.getReceiverPort());
                        } catch (Exception e) {
                            System.out.println("Failed sending " + message + " to " + record.getNodeId());
                        }
                });
                nodeLoginThread.start();
            } 
        }
    }
}