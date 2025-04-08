import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static final int NUMBER_OF_NODES = 5; // Num de nodes
    public static void main(String[] args)
    {
        // Generate DH parameters file
        try {
            DHGenerator.generateAndSaveDHParams();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println("Failed generating DH params");
        }

        // Generate rsa keys
        //for (int n = 0; n < NUMBER_OF_NODES; n++ ) {
        //    AsymetricKeyGenerator.generateAndStoreKeys(String.valueOf(n));
        //}

        // Generate addressBook
        AddressBook[] addressBookArr = new AddressBook[NUMBER_OF_NODES];
        for (int n = 0; n < NUMBER_OF_NODES; n++ ) {
            addressBookArr[n] = new AddressBook();
        }
        for (int n = 0; n < NUMBER_OF_NODES; n++ ) {
            for (int m = 0; m < NUMBER_OF_NODES; m++ ) {
                boolean isLeader;
                if (n == 0) {
                    isLeader = true;
                } else {
                    isLeader=false;
                }
                AddressRecord addressRecord = new AddressRecord(n, 1235+n, "localhost", isLeader);
                addressBookArr[m].addRecord(addressRecord);
            }
        }

        Block[] blockArr = new Block[NUMBER_OF_NODES];
        // Generate Blocks
        for (int n = 0; n < NUMBER_OF_NODES; n++ ) {
            try {
                blockArr[n] = new Block(n, addressBookArr[n]);
            } catch (Exception e) {
                System.out.println("Failed generating block");
            }
        }
        propose(blockArr, "adeus");
        propose(blockArr, "ola");
    }

    private static void propose(Block[] nodes, String value){
        for (Block block : nodes){
            block.propose(value);
        }
    }

}
