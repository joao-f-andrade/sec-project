
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

    public void sendMessage(String mainMessage, int destinationNodeId) throws Exception{

        byte[] SECRET_KEY = _addressBook.getRecordById(destinationNodeId).getSecretKey();
        int port = _addressBook.getRecordById(destinationNodeId).getPort();
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);

        boolean ack = false;

        while(!ack){

            Message message = new Message();
            message.createMessage(_addressBook.getOwnerId(), mainMessage, _messageID, SECRET_KEY);


            DatagramPacket packet = new DatagramPacket(message.getFullData(), message.getFullData().length, _receiverAddress, port);
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

            Message message = new Message();
            message.decodeMessage(packet);
            AddressRecord senderRecord =  _addressBook.getRecordById(message.getSenderId());

            byte[] calulatedHMAC =message.getCalculatedHmac(senderRecord.getSecretKey());
            byte[] hmac= message.getHmac();
            if (Arrays.equals(calulatedHMAC, hmac)) {
                int messageId = message.getMessageId();
                if (!receivedMessages.contains(messageId)){
                    receivedMessages.add(messageId);
                    System.out.println("Received authenticated message: " + message);
                    String ackMessage = "ACK:" + messageId;
                    byte[] ackData = ackMessage.getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
                    socket.send(ackPacket);
                } else {
                    System.out.println("Duplicate message with sequence number " + messageId + " ignored.");
                }
            } else {
                System.out.println("Tampered Packet...");
            }
        }
    }


}