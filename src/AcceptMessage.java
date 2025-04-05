import org.json.JSONObject;

public class AcceptMessage extends Message {
    private final String _val;

    public AcceptMessage(int senderId, int consensusTs, String val) {
        super("ACCEPT", senderId, consensusTs);
        _val = val;
    }

    @Override
    public String toJsonString() {
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("event", _type);
        jsonContent.put("val", _val);
        jsonContent.put("consensusInstance", _consensusTs);
        jsonContent.put("senderId", _senderId);
        return jsonContent.toString();
    }
}
