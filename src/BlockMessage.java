import java.security.interfaces.RSAPrivateKey;

public class BlockMessage {
    public static String createMessage (String event, int epoch, String content, boolean signed, RSAPrivateKey privateKey) {
        // create message of the format event:epoch:message:signature
        String escapedContent = escapeDelimiter(content);  // Escape any `:` in content
        String unsignedMessage = event + ":" + epoch + ":" + escapedContent;
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
        // return the escaped message and the rsaHash or empty string
        int lastColonIndex = fullMessage.lastIndexOf(':');
        String beforeLastColon = fullMessage.substring(0, lastColonIndex);
        String afterLastColon = fullMessage.substring(lastColonIndex + 1);
        return new String[]{beforeLastColon, afterLastColon};
    }



   public static String[] decodeMessage(String escapedMessage) {
        String[] splitMessage = escapedMessage.split(":", -1);
        for (int i = 0; i < splitMessage.length; i++) {
            splitMessage[i] = unescapeDelimiter(splitMessage[i]);

        }
        return splitMessage;
   }

    // Escape `|` in message content
    private static String escapeDelimiter(String input) {
        return input.replace(":", "~~");
    }

    // Unescape `|` in message content
    private static String unescapeDelimiter(String input) {
        return input.replace("~~", ":");
    }
}
