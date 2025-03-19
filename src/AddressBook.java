import java.util.List;
import java.util.ArrayList;

public class AddressBook {
    private int ownerId;
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

    public AddressRecord getRecordByReceiverPort(int port) {
        for (AddressRecord record : records) {
            if (record.getReceiverPort() == port) {
                return record;
            }
        }
        System.out.println("Failed getting receiver port");

        return null; // Return null if not found
    }

    public void setOwnerId (int nodeId) {
        ownerId = nodeId;
    }

    public int getOwnerId () {
        return ownerId;
    }

    public List<AddressRecord> getAddressRecords (){
        return records;
    }

}
