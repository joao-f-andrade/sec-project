import org.json.JSONObject;

public abstract class Message{

    protected String _type;
    protected int _senderId;
    protected int _consensusTs;

    public Message (String type, int senderId, int consensusTs){
        _type = type;
        _senderId = senderId;
        _consensusTs = consensusTs;
    }

    public String toJsonString() {
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("event", _type);
        jsonContent.put("consensusInstance", _consensusTs);
        jsonContent.put("senderId", _senderId);
        return jsonContent.toString();
    }
}

