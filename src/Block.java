import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
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
    }

    private void startReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                authenticatedLink.receiver();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                String message = authenticatedLink.getReceivedMessage();
                System.out.println("mesnagem no block " + message);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
        receiverThread.start();
    }

//    private void startReceiver() {
//        MessageReceiver receiver = new MessageReceiver();
//        receiver.startReceiver(authenticatedLink);
//        String message = null;
//        try {
//            message = receiver.getMessage();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("Received: " + message);
//    }

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

    public void testSender(){
        try {
            authenticatedLink.sendMessage("ola de" +senderPort, 1237);
        } catch (Exception e) {
            System.out.println("falhou envio de "+ senderPort);
        }
    }

    public String getMessage() throws InterruptedException {
        return messageQueue.take(); // Blocks until a message is available
    }

}