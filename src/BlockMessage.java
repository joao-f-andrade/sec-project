import java.security.interfaces.RSAPrivateKey;
import org.json.JSONObject;

public class BlockMessage {
    public static String createMessage (String event, int consensusInstance, String content, boolean signed, RSAPrivateKey privateKey) {
        // create message of the format {event: ,content: , consensusInstance: }:signature
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("event", event);
        jsonContent.put("content", content);
        jsonContent.put("consensusInstance", consensusInstance);

        String unsignedMessage = jsonContent.toString();
        if (signed){
            try {
                String signature = MessageSigner.signMessage(unsignedMessage, privateKey);
                return unsignedMessage + ":" + signature;
            }
            catch (Exception e) {
                System.out.println("key " + privateKey);
                e.printStackTrace();
                throw new IllegalStateException("Failed signing message");
            }
        }
        else {
            return unsignedMessage + ":";
        }
    }

    public static String[] splitSignatureMessage(String fullMessage) {
        // json:signature

        int lastColonIndex = fullMessage.lastIndexOf(':');
        String beforeLastColon = fullMessage.substring(0, lastColonIndex);
        String afterLastColon = fullMessage.substring(lastColonIndex + 1);
        return new String[]{beforeLastColon, afterLastColon};
    }



   public static JSONObject decodeMessage(String message) {
       return new JSONObject(message);
   }

   public static JSONObject decodeCollectedMessage(Object objectMessage){
       return new JSONObject(objectMessage.toString());
   };
}
