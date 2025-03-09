import java.net.InetAddress;
import java.net.UnknownHostException;

public class AddressRecord
{
    private final int nodeId;
    private final int port;
    private final String address;
    private byte[] secretKey;

    public AddressRecord(int nodeId, int port, String address) {
        this.nodeId = nodeId;
        this.port = port;
        this.address = address;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey){
        this.secretKey = secretKey;
    }
}
