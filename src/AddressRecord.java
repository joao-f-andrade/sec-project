import javax.crypto.SecretKey;

public class AddressRecord
{
    private final int nodeId;
    private final int port;
    private final String address;
    private SecretKey secretKey;

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

    public String getAddress() {
        return address;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey){
        this.secretKey = secretKey;
    }
}
