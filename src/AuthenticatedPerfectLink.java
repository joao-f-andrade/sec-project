import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthenticatedPerfectLink {

    private InetAddress _receiverAddress;
    private int _port;
    private int _messageID = 0;
    private static final int TIMEOUT = 1000;
    private static final int MAX_PACKET_SIZE = 1024;
    private AddressBook _addressBook;

    private Set<Integer> receivedMessages = new HashSet<>();


    public AuthenticatedPerfectLink(AddressBook addressBook){
        _receiverAddress = addressBook.getRecordById(addressBook.getOwnerId()).getAddress();
        _port = addressBook.getRecordById(addressBook.getOwnerId()).getPort();
        _addressBook=addressBook;
    }

    public void sendMessage(String message, int nodeId) throws Exception{

        byte[] SECRET_KEY = _addressBook.getRecordById(nodeId).getSecretKey();
        int port = _addressBook.getRecordById(nodeId).getPort();
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        boolean ack = false;

        while(!ack){

            String sendData = _messageID + ":" + message;
            byte[] hmac = calculateHMAC(sendData, SECRET_KEY);
            byte[] fullData = concatenate(sendData.getBytes() , hmac);

            DatagramPacket packet = new DatagramPacket(fullData, fullData.length, _receiverAddress, port);
            socket.send(packet);

            try{

                byte[] buffer = new byte[MAX_PACKET_SIZE];

                DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());

                if(ackMessage.equals("ACK:" + _messageID)){
                    ack = true;
                    System.out.println("Message delivered successfully!");
                    _messageID += 1;
                }

                
            } catch (SocketTimeoutException e){
                System.out.println("Timeout! Resending message...");
            }

        }

        socket.close();
    }

    public void receiver() throws Exception{

        DatagramSocket socket = new DatagramSocket(_port);
        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while(true){
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int senderPort = packet.getPort();
            AddressRecord senderRecord =  _addressBook.getRecordByPort(senderPort);

            byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength() - 32);
            byte[] receivedHmac = Arrays.copyOfRange(packet.getData(), packet.getLength() - 32, packet.getLength());

            String receivedMessage = new String(data);
            String[] parts = receivedMessage.split(":");
            int seq = Integer.parseInt(parts[0]);
            String message = parts[1];

            byte[] calculatedHmac = calculateHMAC(seq + ":" + message, senderRecord.getSecretKey());

            if (Arrays.equals(receivedHmac, calculatedHmac)) {
                if (!receivedMessages.contains(seq)){
                    receivedMessages.add(seq);
                    System.out.println("Received authenticated message: " + message);
                    String ackMessage = "ACK:" + seq;
                    byte[] ackData = ackMessage.getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
                    socket.send(ackPacket);
                } else {
                    System.out.println("Duplicate message with sequence number " + seq + " ignored.");
                }
            } else {
                System.out.println("Tampered Packet...");
            }
        }
    }

    private static byte[] calculateHMAC(String message, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(message.getBytes());
    }

    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}