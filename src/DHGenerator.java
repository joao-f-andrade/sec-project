import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.io.*;
import java.math.BigInteger;

public class DHGenerator {
    private static final String DH_PARAMS_FILE = "dh_params.txt";

    // Generate raw shared secret (needs processing before use)
    public static byte[] generateSharedSecret(PrivateKey privateKey, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret(); // Raw secret (must be hashed)
    }

    public static KeyPair generateKeyPair () throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(DH_PARAMS_FILE));
        BigInteger p = new BigInteger(reader.readLine());
        BigInteger g = new BigInteger(reader.readLine());
        reader.close();

        DHParameterSpec dhParams = new DHParameterSpec(p, g);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhParams);
        return keyPairGenerator.generateKeyPair();
    }

    public static PublicKey bytesToPublicKey(byte[] bytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bytes);
            return keyFactory.generatePublic(x509KeySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateAndSaveDHParams () throws NoSuchAlgorithmException, IOException {
        File file = new File(DH_PARAMS_FILE);
        if (!file.exists()) {
           // System.out.println(DH_PARAMS_FILE+" already exists");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");

            keyPairGenerator.initialize(2048); // Generate 2048-bit DH parameters
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Get p and g from generated key
            DHPublicKey dhPublicKey = (DHPublicKey) keyPair.getPublic();
            DHParameterSpec dhParams = dhPublicKey.getParams();

            // Save p and g to a file
            try (FileWriter writer = new FileWriter(DH_PARAMS_FILE)) {
                writer.write(dhParams.getP().toString() + "\n");
                writer.write(dhParams.getG().toString() + "\n");
            }

            System.out.println("DH parameters saved to "+DH_PARAMS_FILE);
        }
    }

}