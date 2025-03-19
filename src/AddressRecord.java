import java.net.InetAddress;
import java.net.UnknownHostException;

public class AddressRecord
{
    private final int nodeId;
    private final int receiverPort;
    private final String address;
    private final boolean isClient;
    private final boolean isLeader;

    public AddressRecord(int nodeId, int receiverPort, int senderPort, String address, Boolean isClient, Boolean isLeader) {
        this.nodeId = nodeId;
        this.receiverPort = receiverPort;
        this.address = address;
        this.isClient = isClient;
        this.isLeader = isLeader;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getReceiverPort() {
        return receiverPort;
    }

    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClient() {
        return isClient;
    }

    public boolean isLeader() {
        return isLeader;
    }
}
