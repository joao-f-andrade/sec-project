import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthenticatedPerfectLinkMessage {

    public static String createMessage (int idSender, String content, int messageId, byte[] SECRET_KEY) throws Exception{
        // create message of the format idSender|0|messageId|message|hmac
        content = escapeDelimiter(content);  // Escape any `|` in content
        String sendData = idSender+"|0|"+ messageId + "|" + content;
        byte[] hmac = calculateHMAC(sendData, SECRET_KEY);
        String encodedHmac = Base64.getEncoder().encodeToString(hmac);
        return sendData + "|" + encodedHmac;
    }

    public static String createDHMessage(int idSender, byte[] publicKey){
        // create message of the format idSender|1|publicKey
        String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey);
        return idSender + "|1|" + encodedPublicKey;
    }

    public static int[] getTypeAndPort(String receivedMessage) {
        String[] parts = receivedMessage.split("\\|", -1);
        int messageType = Integer.parseInt(parts[1]);
        int port = Integer.parseInt(parts[0]);
        if (messageType==0| messageType==1){
            return new int[]{messageType, port };
        } else {
            throw new IllegalArgumentException("Invalid message type");
        }
    }

    public static byte[] decodeDHMessage(String message){

        String[] parts = message.split("\\|", -1);
        return Base64.getDecoder().decode(parts[2]);
    }

    public static Map<String, Object> decodeMessage(String receivedMessage, byte[] secret) throws Exception {
        String[] parts = receivedMessage.split("\\|", -1);
        int idSender = Integer.parseInt(parts[0]);
        int messageId = Integer.parseInt(parts[2]);
        String escapedContent = parts[3];
        byte[] computedHmac = calculateHMAC(idSender+"|0|"+ messageId + "|" + escapedContent, secret);
        String content = unescapeDelimiter(escapedContent);

        byte[] hmac = Base64.getDecoder().decode(parts[4]);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messageId);
        messageMap.put("content", content);
        messageMap.put("hmac", hmac);
        messageMap.put("calculatedHmac", computedHmac);

        return messageMap;
    }


    public static byte[] calculateHMAC(String message, byte[] key) throws Exception  {
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

    // Escape `|` in message content
    private static String escapeDelimiter(String input) {
        return input.replace("|", "\\|");
    }

    // Unescape `|` in message content
    private static String unescapeDelimiter(String input) {
        return input.replace("\\|", "|");
    }

}
