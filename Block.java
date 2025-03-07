import java.net.InetAddress;
import java.util.Scanner;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Block{

    private AuthenticatedPerfectLink authenticatedLink;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Block(InetAddress address, int port) throws Exception {
        authenticatedLink = new AuthenticatedPerfectLink(address, port);
        generateAndPrintKeys();
    }

    private void generateAndPrintKeys() throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        publicKey = pair.getPublic();
        privateKey = pair.getPrivate();

        System.out.println("Public Key: " + Base64.getEncoder().encodeToString(publicKey.getEncoded()));
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
                System.out.println("Enter port number: ");
                int port = Integer.parseInt(scanner.nextLine());
                while (true) {
                    System.out.print("Enter message to send: ");
                    String message = scanner.nextLine();
                    authenticatedLink.sendMessage(message, port);
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

        Block block = new Block(address, port);

        block.startReceiver();

        block.startSender();
    }
}