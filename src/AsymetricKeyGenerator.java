import java.io.*;
import java.security.*;

public class AsymetricKeyGenerator {
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048; // Standard RSA key size

    public static void main(String[] args) {
        for (int i = 0; i <= 5; i++) {
                generateAndStoreKeys(Integer.toString(i));
        }
    }

    private static void generateAndStoreKeys(String processName) {
        try {
            // Generate Key Pair
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGen.generateKeyPair();

            // Retrieve Keys
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Store the keys as Java objects
            storeKey("keys/"+ processName + "_id_rsa", privateKey);
            storeKey("keys/"+ processName + "_id_rsa.pub", publicKey);

            System.out.println("Keys generated and stored for: " + processName);

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void storeKey(String filename, Key key) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(key);
        }
    }
}
