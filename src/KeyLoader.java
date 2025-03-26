import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class KeyLoader {
    private static Key loadKeyFromFile(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Key) ois.readObject();
        }
    }

    public static RSAPrivateKey loadPrivateKeyFromFile(int id) {
        try {
            // Load the private key from the file
            return (RSAPrivateKey) loadKeyFromFile("keys/" + id + "_id_rsa");
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if there's an error
        }
    }

    public static RSAPublicKey loadPublicKeyFromFile(int id) {
        try {
            // Load the private key from the file
            return (RSAPublicKey) loadKeyFromFile("keys/" + id + "_id_rsa.pub");
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if there's an error
        }
    }
}
