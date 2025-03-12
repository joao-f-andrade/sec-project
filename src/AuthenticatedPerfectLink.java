import java.net.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class AuthenticatedPerfectLink {

    private InetAddress _receiverAddress;
    private int _port;
    private int _messageID = 0;
    private static final int TIMEOUT = 1000;
    private static final int MAX_PACKET_SIZE = 1024;
    private AddressBook _addressBook;
    private static final ConcurrentHashMap<SocketAddress, ClientHandler> clients = new ConcurrentHashMap<>();
    private Set<Integer> receivedMessages = new HashSet<>();


    public AuthenticatedPerfectLink(AddressBook addressBook) {
        _receiverAddress = addressBook.getRecordById(addressBook.getOwnerId()).getAddress();
        _port = addressBook.getRecordById(addressBook.getOwnerId()).getReceiverPort();
        _addressBook = addressBook;
    }

    public void sendMessage(String mainMessage, int destinationNodeId, int ownerPort) throws Exception {

        int port = _addressBook.getRecordById(destinationNodeId).getReceiverPort();
        DatagramSocket socket = new DatagramSocket(ownerPort);
        socket.setSoTimeout(TIMEOUT);

        // set up do segredo inicial
        if (_addressBook.getRecordById(destinationNodeId).getSecretKey() == null) {
            KeyPair keyPair = DHGenerator.generateKeyPair();
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            DatagramPacket packet = new DatagramPacket(publicKeyBytes, publicKeyBytes.length, _receiverAddress, port);
            socket.send(packet);

            // Receive server's public key
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            byte[] serverPublicKeyBytes = receivePacket.getData();
            PublicKey serverPublicKey = DHGenerator.bytesToPublicKey(serverPublicKeyBytes);
            byte[] secret = DHGenerator.generateSharedSecret(keyPair.getPrivate(), serverPublicKey);

            _addressBook.getRecordById(destinationNodeId).setSecretKey(secret);
            System.out.println("DH handshake generated");
        }

        byte[] SECRET_KEY = _addressBook.getRecordById(destinationNodeId).getSecretKey();

        boolean ack = false;

        while (!ack) {

            Message message = new Message();
            message.createMessage(_addressBook.getOwnerId(), mainMessage, _messageID, SECRET_KEY);


            DatagramPacket packet = new DatagramPacket(message.getFullData(), message.getFullData().length, _receiverAddress, port);
            socket.send(packet);

            try {

                byte[] buffer = new byte[MAX_PACKET_SIZE];

                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());

                if (ackMessage.equals("ACK:" + _messageID)) {
                    ack = true;
                    System.out.println("Message delivered successfully!");
                    _messageID += 1;
                }


            } catch (SocketTimeoutException e) {
                System.out.println("Timeout! Resending message...");
            }

        }

        socket.close();
    }

    public void receiver() throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(_port);  // Main socket for receiving
        System.out.println("Receiver is now listening "+_port);

        while (true) {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);

            SocketAddress clientAddress = packet.getSocketAddress();
            System.out.println("Received packet from " + clientAddress);

            clients.compute(clientAddress, (key, handler) -> {
                if (handler == null) {
                    handler = new ClientHandler(packet, serverSocket, _addressBook);  // Pass the main socket
                    handler.start();
                } else {
                    handler.handlePacket(packet, _addressBook, receivedMessages);
                }
                return handler;
            });
        }
    }
}
class ClientHandler extends Thread {
    private final SocketAddress clientAddress;
    private DatagramSocket socket;
    private final int clientPort;
    private final AddressRecord clientAddressRecord;

    public ClientHandler(DatagramPacket packet, DatagramSocket serverSocket, AddressBook addressBook) {
            clientPort = packet.getPort();
            clientAddressRecord = addressBook.getRecordBySenderPort(clientPort);

            this.clientAddress = packet.getSocketAddress();
            this.socket = serverSocket;

            PublicKey clientPublicKey = DHGenerator.bytesToPublicKey(packet.getData());
            try {
                KeyPair serverKeyPair = DHGenerator.generateKeyPair();
                byte[] secret = DHGenerator.generateSharedSecret(serverKeyPair.getPrivate(), clientPublicKey);
                clientAddressRecord.setSecretKey(secret);
                byte[] publicKeyBytes = serverKeyPair.getPublic().getEncoded();
                DatagramPacket serverPacket = new DatagramPacket(publicKeyBytes, publicKeyBytes.length, clientAddress);
                socket.send(serverPacket);
            } catch (Exception e){
                System.out.println("Failed creating key pair");
            }
            

    }

    public void handlePacket(DatagramPacket packet, AddressBook addressBook, Set<Integer> receivedMessages) {
        try {
            String clientMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received from client: " + clientMessage);

            Message message = new Message();
            message.decodeMessage(packet);

            int messageId = message.getMessageId();
            if (!receivedMessages.contains(messageId)) {
                receivedMessages.add(messageId);
                System.out.println("Received message: " + message);

                byte[] calculatedHMAC = message.getCalculatedHmac(clientAddressRecord.getSecretKey());
                byte[] hmac = message.getHmac();

                if (Arrays.equals(calculatedHMAC, hmac)) {
                    System.out.println("Message is authentic.");
                } else {
                    System.out.println("Tampered Packet detected!");
                }
            } else {
                System.out.println("Duplicate message with sequence number " + messageId + " ignored.");
            }

            // Send acknowledgment
            String ackMessage = "ACK:" + messageId;
            byte[] ackData = ackMessage.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), clientPort);
            socket.send(ackPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
