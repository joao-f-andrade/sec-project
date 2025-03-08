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

    public void removeRecord(int nodeId) {
        records.removeIf(record -> record.getNodeId()==nodeId);
    }
    public AddressRecord getRecordById(int nodeId) {
        for (AddressRecord record : records) {
            if (record.getNodeId() == nodeId) {
                return record;
            }
        }
        return null; // Return null if not found
    }

}
