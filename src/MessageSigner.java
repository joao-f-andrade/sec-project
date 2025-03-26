import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class MessageSigner {
    public static String signMessage(String message, RSAPrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");

        if (privateKey == null) {
            throw new IllegalStateException("Private key could not be loaded");
        }

        signature.initSign(privateKey);
        signature.update(message.getBytes());
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    public static boolean verifySignature( String message, String rsaHash, RSAPublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes());
            byte[] hashBytes = Base64.getDecoder().decode(rsaHash);
            return signature.verify(hashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}