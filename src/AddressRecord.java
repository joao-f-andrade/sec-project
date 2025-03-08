public class AddressRecord
{
    private final int nodeId;
    private final int port;
    private final String address;
    private String secretKey;

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

    public String getAdress() {
        return address;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
