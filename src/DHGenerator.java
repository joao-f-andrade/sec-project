import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class DHGenerator {
    private static final String DH_PARAMS_FILE = "dh_params.txt";

    public static void main(String[] args) throws Exception {
        // This tests the generation of keys and secret

        generateAndSaveDHParams();
        // Generate key pairs for Alice and Bob
        generateSaveKeys("alice");
        generateSaveKeys("bob");
        KeyPair aliceKeyPair = loadKeyPair("alice");
        KeyPair bobKeyPair = loadKeyPair("bob");

        // Generate shared secrets
        byte[] aliceSharedSecret = generateRawSharedSecret(aliceKeyPair.getPrivate(), bobKeyPair.getPublic());
        byte[] bobSharedSecret = generateRawSharedSecret(bobKeyPair.getPrivate(), aliceKeyPair.getPublic());

        // Derive AES key from shared secret
        SecretKey aliceAESKey = deriveAESKey(aliceSharedSecret);
        SecretKey bobAESKey = deriveAESKey(bobSharedSecret);

        // Print keys (they should be identical)
        System.out.println("Alice's AES Key: " + bytesToHex(aliceAESKey.getEncoded()));
        System.out.println("Bob's AES Key:   " + bytesToHex(bobAESKey.getEncoded()));

        // Verify both keys match
        if (MessageDigest.isEqual(aliceAESKey.getEncoded(), bobAESKey.getEncoded())) {
            System.out.println("Key exchange successful: Both parties have the same AES key.");
        } else {
            System.out.println("Key exchange failed: AES keys do not match.");
        }
    }

    public static byte[] getSecret (String namePriv, String namePub) throws Exception {
        PrivateKey privateKey = loadPrivateKey(namePriv);
        PublicKey publicKey = loadPublicKey(namePub);
        return generateRawSharedSecret(privateKey, publicKey);
    }
    // Generate raw shared secret (needs processing before use)
    private static byte[] generateRawSharedSecret(PrivateKey privateKey, PublicKey publicKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret(); // Raw secret (must be hashed)
    }

    // Derive an AES key from the shared secret using SHA-256
    private static SecretKey deriveAESKey(byte[] sharedSecret) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(sharedSecret); // Hash the shared secret
        keyBytes = Arrays.copyOf(keyBytes, 16); // Use only first 16 bytes for AES-128
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Convert bytes to hex (for printing)
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }


    public static void generateAndSaveDHParams () throws NoSuchAlgorithmException, IOException {
        File file = new File(DH_PARAMS_FILE);
        if (file.exists()) {
            System.out.println(DH_PARAMS_FILE+" already exists");
            return;
        } else {
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

    public static void generateSaveKeys (String name) throws Exception {
        // Load p and g from file
        BufferedReader reader = new BufferedReader(new FileReader(DH_PARAMS_FILE));
        BigInteger p = new BigInteger(reader.readLine());
        BigInteger g = new BigInteger(reader.readLine());
        reader.close();

        // Use the same p and g to generate a compatible key pair
        DHParameterSpec dhParams = new DHParameterSpec(p, g);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhParams);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        saveKey(String.format("./keys/%s_dh_pub.key", name), keyPair.getPublic().getEncoded());
        saveKey(String.format("./keys/%s_dh.key", name), keyPair.getPrivate().getEncoded());

        System.out.println("Keys saved successfully!");

    }
    private static void saveKey(String filename, byte[] keyBytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(keyBytes);
        }
    }
    private static KeyPair loadKeyPair(String name) throws Exception {
        PublicKey publicKey = loadPublicKey(name);
        PrivateKey privateKey = loadPrivateKey(name);
        return new KeyPair(publicKey, privateKey); // Create KeyPair object
    }

    private static PublicKey loadPublicKey(String name) throws Exception {
        byte[] keyBytes = readFile(String.format("./keys/%s_dh_pub.key", name));
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

        return keyFactory.generatePublic(keySpec);
    }

    private static PrivateKey loadPrivateKey(String name) throws Exception {
        byte[] keyBytes = readFile(String.format("./keys/%s_dh.key", name));
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        return keyFactory.generatePrivate(keySpec);
    }

    private static byte[] readFile(String filename) throws IOException {
        try (FileInputStream fis = new FileInputStream(filename)) {
            return fis.readAllBytes();
        }
    }
}