import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>();
    private final int senderPort;
    private final int receiverPort;

    public Block(int nodeId, AddressBook addressBook) throws Exception {
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);
        senderPort = addressRecord.getSenderPort();
        receiverPort = addressRecord.getReceiverPort();
        authenticatedLink = new AuthenticatedPerfectLink(senderPort, receiverPort , addressRecord.getAddress());
        startReceiver();
        nodeLogic();
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

                    System.out.println("mensagem no block " + message.content() + " vindo de " + message.port());
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
            System.out.println("falhou envio de "+ senderPort);
        }
    }
}