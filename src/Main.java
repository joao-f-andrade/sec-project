import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static final int NUMBER_OF_NODES = 5;
    public static void main(String[] args)
    {
        // Generate DH parameters file
        try {
            DHGenerator.generateAndSaveDHParams();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println("Failed generating DH params");
        }

        // Generate addressBook and key pairs
        AddressBook addressBook = new AddressBook();
        System.out.println("Generating address book");
        for (int n = 0; n < NUMBER_OF_NODES; n++ ) {
            AddressRecord addressRecord = new AddressRecord(n, 1235+n, "localhost");
            addressBook.addRecord(addressRecord);
            try {
                DHGenerator.generateSaveKeys(Integer.toString(n));
            } catch (Exception e) {
                System.out.println("Failed generating key pair");
            }
        }
        try {
            new Block(0, addressBook );
        } catch (Exception e) {
            System.out.println("Failed generating block");

        }
    }

}