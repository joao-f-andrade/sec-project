import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.util.Scanner;

public class Block{
    private final AuthenticatedPerfectLink authenticatedLink;
    private AddressBook addressBook;

    public Block(int nodeId, AddressBook addressBook) throws Exception {
        this.addressBook = addressBook;
        this.addressBook.setOwnerId(nodeId);
        AddressRecord addressRecord = this.addressBook.getRecordById(nodeId);
        InetAddress address = addressRecord.getAddress();

        for(int n = 0; n < addressBook.size(); n++ ){
            if (nodeId != n){
                byte[] secretKey = DHGenerator.getSecret(Integer.toString(nodeId), Integer.toString(n));
                addressBook.setSecretKeyById(n, secretKey);
            }
        }

        authenticatedLink = new AuthenticatedPerfectLink(addressBook);
        startReceiver();
        startSender();
    }

    private void startReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                System.out.println("Receiver is now listening...");
                authenticatedLink.receiver();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

    private void startSender() {
        Thread senderThread = new Thread(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter node id number: ");
                int nodeId = Integer.parseInt(scanner.nextLine());
                while (true) {
                    System.out.print("Enter message to send: ");
                    String message = scanner.nextLine();
                    authenticatedLink.sendMessage(message, nodeId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        senderThread.start();
    }

    public static void main(String[] args) throws Exception{

        if (args.length != 2) {
            System.out.println("Usage: java Block <receiver_address> <port>");
            return;
        }

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        AddressBook addressBook = new AddressBook();
        Block block = new Block(0, addressBook);

    }

}