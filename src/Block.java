import java.util.Scanner;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;

    public Block(int nodeId, AddressBook addressBook) throws Exception {
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);

        authenticatedLink = new AuthenticatedPerfectLink(addressBook);
        startReceiver();
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

    public void startSender() {
        Thread senderThread = new Thread(() -> {
            try {
                System.out.print("Enter message to send: ") ;
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter node id number: ");
                int destinationId = Integer.parseInt(scanner.nextLine());
                int senderPort = addressBook.getRecordById( addressBook.getOwnerId()).getSenderPort();
                while (true) {
                    System.out.print("Enter message to send: ");
                    String message = scanner.nextLine();
                    authenticatedLink.sendMessage(message, destinationId, senderPort );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        senderThread.start();
    }
}