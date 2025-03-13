import java.net.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.charset.StandardCharsets;

public class AuthenticatedPerfectLink {

    private final InetAddress address;
    private final int receiverPort;
    private final int senderPort;
    private int _messageID = 0;
    private static final int TIMEOUT = 1000;
    private static final int MAX_PACKET_SIZE = 1024;
    private Map<Integer, byte[]> keysColection;
    // private static final ConcurrentHashMap<SocketAddress, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(); // Shared queue


    public AuthenticatedPerfectLink(int senderPort, int receiverPort, InetAddress address ) {
        this.address = address;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
        keysColection = new HashMap<>();
    }

    public void sendMessage(String mainMessage, int destinationPort) throws Exception {

        DatagramSocket socket = new DatagramSocket(senderPort);
        socket.setSoTimeout(TIMEOUT);

        // set up do segredo inicial
        if (!keysColection.containsKey(destinationPort)) {
            KeyPair keyPair = DHGenerator.generateKeyPair();
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            String message = AuthenticatedPerfectLinkMessage.createDHMessage(senderPort, publicKeyBytes);
            DatagramPacket packet = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.length(), address, destinationPort);
            socket.send(packet);

            // Receive server's public key
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
            byte[] serverPublicKeyBytes = AuthenticatedPerfectLinkMessage.decodeDHMessage(receivedMessage);
            PublicKey serverPublicKey = DHGenerator.bytesToPublicKey(serverPublicKeyBytes);
            byte[] secret = DHGenerator.generateSharedSecret(keyPair.getPrivate(), serverPublicKey);

            keysColection.put(destinationPort, secret);
            // System.out.println("DH handshake generated");
        }

        byte[] SECRET_KEY = keysColection.get(destinationPort);

        boolean ack = false;

        while (!ack) {

            String message = AuthenticatedPerfectLinkMessage.createMessage(senderPort, mainMessage, _messageID, SECRET_KEY);


            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, destinationPort);
            socket.send(packet);

            try {

                byte[] buffer = new byte[MAX_PACKET_SIZE];

                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());

                if (ackMessage.equals("ACK:" + _messageID)) {
                    ack = true;
                    _messageID += 1;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout! Resending message...");
            }

        }

        socket.close();
    }

    public String receiver() throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(receiverPort);  // Main socket for receiving

        while (true) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int[] typeAndPort =  AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
            int packetSenderPort = typeAndPort[1];

            clients.compute(packetSenderPort, (key, handler) -> {

                    if (handler == null) {
                    handler = new ClientHandler(packet, serverSocket, keysColection, messageQueue, address);  // Pass the main socket
                    handler.start();
                } else {
                    handler.handlePacket(packet, keysColection, messageQueue);
                }
                return handler;
            });

        }
    }
    public String getReceivedMessage() throws InterruptedException {
        return messageQueue.take(); // Blocks until a message is available
    }
}
class ClientHandler extends Thread {
    private final InetAddress address;
    private DatagramSocket socket;
    private  int packetSenderPort;
    private final BlockingQueue<String> messageQueue; // Shared queue reference
    private Set<Integer> receivedMessages = new HashSet<>();


    public ClientHandler(DatagramPacket packet, DatagramSocket serverSocket, Map<Integer, byte[]> keysColection, BlockingQueue<String> queue, InetAddress address) {
        String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        int[] typeAndPort =  AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
        int type = typeAndPort[0];
        this.packetSenderPort = typeAndPort[1];
        this.messageQueue = queue; // Store queue reference
        this.address = address;
        this.socket = serverSocket;

        if (type==1) {
            byte[] publicKeyBytesClient = AuthenticatedPerfectLinkMessage.decodeDHMessage(receivedMessage);
            PublicKey clientPublicKey = DHGenerator.bytesToPublicKey(publicKeyBytesClient);
            try {
                KeyPair serverKeyPair = DHGenerator.generateKeyPair();
                byte[] secret = DHGenerator.generateSharedSecret(serverKeyPair.getPrivate(), clientPublicKey);
                keysColection.put(packetSenderPort, secret);
                byte[] publicKeyBytes = serverKeyPair.getPublic().getEncoded();
                String message = AuthenticatedPerfectLinkMessage.createDHMessage(packet.getPort(), publicKeyBytes);
                DatagramPacket serverPacket = new DatagramPacket(message.getBytes(), message.length(), address, packetSenderPort);
                socket.send(serverPacket);
            } catch (Exception e) {
                System.out.println("Failed creating key pair");
            }
        }

    }

    public void handlePacket(DatagramPacket packet, Map<Integer, byte[]> keysColection, BlockingQueue<String> queue) {
        try {
            String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int[] typeAndPort = AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
            int type = typeAndPort[0];
            this.packetSenderPort = typeAndPort[1];

            if (type == 0) {
                Map<String, Object> decoded = AuthenticatedPerfectLinkMessage.decodeMessage(receivedMessage, keysColection.get(packetSenderPort));
                int messageId = (int) decoded.get("messageId");
                byte[] hmac = (byte[]) decoded.get("hmac");
                byte[] calculatedHMAC = (byte[]) decoded.get("calculatedHmac");
                String content = (String) decoded.get("content");
                if (!receivedMessages.contains(messageId)) {
                    receivedMessages.add(messageId);
                    if (Arrays.equals(calculatedHMAC, hmac)) {
                        System.out.println("Received message: " + content + " from " + packetSenderPort);
                        queue.put(content);// Add message to queue
                    } else {
                        System.out.println("Tampered Packet detected!");
                    }
                } else {
                    System.out.println("Duplicate message with sequence number " + messageId + " ignored.");
                }

                // Send acknowledgment
                String ackMessage = "ACK:" + messageId;
                byte[] ackData = ackMessage.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, packetSenderPort);
                socket.send(ackPacket);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
