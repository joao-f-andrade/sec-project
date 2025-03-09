import javax.crypto.SecretKey;
import java.util.List;
import java.util.ArrayList;

public class AddressBook {
    private List<AddressRecord> records;

    public AddressBook() {
        this.records = new ArrayList<>();
    }

    public void addRecord(AddressRecord record) {
        records.add(record);
    }

    public AddressRecord getRecordById(int nodeId) {
        for (AddressRecord record : records) {
            if (record.getNodeId() == nodeId) {
                return record;
            }
        }
        return null; // Return null if not found
    }

    public void setSecretKeyById(int nodeId, SecretKey secretKey) {
        for (AddressRecord record : records) {
            if (record.getNodeId() == nodeId) { // Find the matching nodeId
                record.setSecretKey(secretKey);  // Update address
                return;  // Exit after updating
            }
        }
    }

    public int size(){
        return this.records.size();
    }

}
