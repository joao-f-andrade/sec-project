import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.DatagramPacket;
import java.util.Arrays;

public class Message {
    private int nodeIdSender;
    private String message;
    private byte[] hmac;
    private int messageId;
    private byte[] fullData;

    public void createMessage (int nodeIdSender, String message, int messageID, byte[] SECRET_KEY) throws Exception{
        this.nodeIdSender = nodeIdSender;
        this.message = message;
        this.messageId = messageID;
        String sendData = messageID + ":"+ nodeIdSender + ":" + message;
        this.hmac = calculateHMAC(sendData, SECRET_KEY);
        this.fullData = concatenate(sendData.getBytes() , hmac);
    }

    public void decodeMessage(DatagramPacket packet){
        byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength() - 32);
        this.hmac = Arrays.copyOfRange(packet.getData(), packet.getLength() - 32, packet.getLength());

        String receivedMessage = new String(data);
        String[] parts = receivedMessage.split(":");
        this.messageId = Integer.parseInt(parts[0]);
        this.nodeIdSender= Integer.parseInt(parts[1]);
        this.message= parts[2];

    }

    public byte[] getFullData(){
        return this.fullData;
    }

    public int getSenderId (){
        return this.messageId;
    }

    public byte[] getHmac() {
        return hmac;
    }

    public int getMessageId() {
        return messageId;
    }

    public byte[] getCalculatedHmac(byte[] secretKey) throws Exception{
        return calculateHMAC(this.messageId + ":"+ nodeIdSender + ":" + message, secretKey);
    }

    private static byte[] calculateHMAC(String message, byte[] key) throws Exception  {
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
