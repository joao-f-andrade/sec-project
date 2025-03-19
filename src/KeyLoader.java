import java.io.*;
import java.security.*;

public class KeyLoader {
    public static Key loadKeyFromFile(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (Key) ois.readObject();
        }
    }
}
