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
    private final int senderId;
    private static int _messageID = 0;
    private static final int TIMEOUT = 5000;
    private static final int MAX_PACKET_SIZE = 20480;
    private final Map<Integer, byte[]> keysColection;
    private final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DatagramSocket> senders = new ConcurrentHashMap<>();
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue = new LinkedBlockingQueue<>(); // Shared queue


    public AuthenticatedPerfectLink(int senderId, int receiverPort, InetAddress address) {
        this.address = address;
        this.senderId = senderId;
        this.receiverPort = receiverPort;
        keysColection = new HashMap<>();
    }

    public void sendMessage(String mainMessage, int destinationPort) {
        _messageID += 1;

        senders.compute(destinationPort, (key, socket) -> {
            try {
                if (socket == null) {
                    socket = new DatagramSocket();
                }

                socket.setSoTimeout(TIMEOUT);

                // set up do segredo inicial
                if (!keysColection.containsKey(destinationPort)) {
                    KeyPair keyPair = DHGenerator.generateKeyPair();
                    byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
                    String message = AuthenticatedPerfectLinkMessage.createDHMessage(senderId, publicKeyBytes);
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

                    String message = AuthenticatedPerfectLinkMessage.createMessage(senderId, mainMessage, _messageID, SECRET_KEY);

                    if (message.getBytes().length > MAX_PACKET_SIZE) {
                        System.out.println("Message too big " + message);
                        throw new IllegalArgumentException("Message size exceeds max buffer size: " + MAX_PACKET_SIZE);
                    }

                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, destinationPort);
                    socket.send(packet);

                    try {

                        byte[] buffer = new byte[MAX_PACKET_SIZE];

                        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(ackPacket);
                        String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());

                        if (ackMessage.equals("ACK:" + _messageID)) {
                            ack = true;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout! Resending message...");
                    }

                }

                //     socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("message sent fail by " + senderId + " " + mainMessage);
            }
            return socket;
        });
    }

    public void receiver() throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(receiverPort);  // Main socket for receiving

        while (true) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int[] typeAndId = AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
            int packetSenderId = typeAndId[1];

            clients.compute(packetSenderId, (key, handler) -> {

                if (handler == null) {
                    handler = new ClientHandler(packet, serverSocket, keysColection, messageQueue, address);  // Pass the main socket
                    handler.start();
                    handler.handlePacket(packet, keysColection, messageQueue);
                } else {
                    handler.handlePacket(packet, keysColection, messageQueue);
                }
                return handler;
            });

        }
    }

    public AuthenticatedPerfectLinkOutput getReceivedMessage() throws InterruptedException {
        return messageQueue.take(); // Blocks until a message is available
    }
}

class ClientHandler extends Thread {
    private final InetAddress address;
    private DatagramSocket socket;
    private int packetSenderId;
    private final BlockingQueue<AuthenticatedPerfectLinkOutput> messageQueue; // Shared queue reference


    public ClientHandler(DatagramPacket packet, DatagramSocket serverSocket, Map<Integer, byte[]> keysColection, BlockingQueue<AuthenticatedPerfectLinkOutput> queue, InetAddress address) {
        String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        int[] typeAndId = AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
        this.packetSenderId = typeAndId[1];
        this.messageQueue = queue; // Store queue reference
        this.address = address;
        this.socket = serverSocket;
    }

    public void handlePacket(DatagramPacket packet, Map<Integer, byte[]> keysColection, BlockingQueue<AuthenticatedPerfectLinkOutput> queue) {
        try {
            String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            int[] typeAndId = AuthenticatedPerfectLinkMessage.getTypeAndPort(receivedMessage);
            int type = typeAndId[0];
            this.packetSenderId = typeAndId[1];

            if (type == 1) {
                byte[] publicKeyBytesClient = AuthenticatedPerfectLinkMessage.decodeDHMessage(receivedMessage);
                PublicKey clientPublicKey = DHGenerator.bytesToPublicKey(publicKeyBytesClient);
                try {
                    KeyPair serverKeyPair = DHGenerator.generateKeyPair();
                    byte[] secret = DHGenerator.generateSharedSecret(serverKeyPair.getPrivate(), clientPublicKey);
                    keysColection.put(packetSenderId, secret);
                    byte[] publicKeyBytes = serverKeyPair.getPublic().getEncoded();
                    String message = AuthenticatedPerfectLinkMessage.createDHMessage(packet.getPort(), publicKeyBytes);
                    DatagramPacket serverPacket = new DatagramPacket(message.getBytes(), message.length(), address, packet.getPort());
                    socket.send(serverPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed creating key pair");
                }
            }


            if (type == 0) {
                Map<String, Object> decoded = AuthenticatedPerfectLinkMessage.decodeMessage(receivedMessage, keysColection.get(packetSenderId));
                int messageId = (int) decoded.get("messageId");
                byte[] hmac = (byte[]) decoded.get("hmac");
                byte[] calculatedHMAC = (byte[]) decoded.get("calculatedHmac");
                String content = (String) decoded.get("content");
                if (Arrays.equals(calculatedHMAC, hmac)) {
                    //System.out.println("Received message: " + content + " from " + packetSenderPort);
                    AuthenticatedPerfectLinkOutput messageOutput = new AuthenticatedPerfectLinkOutput(packetSenderId, content);
                    queue.put(messageOutput);// Retorna mensagem para uma queue
                } else {
                    System.out.println("Tampered Packet detected!");
                }
                String ackMessage = "ACK:" + messageId;
                byte[] ackData = ackMessage.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, packet.getPort());
                socket.send(ackPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
