import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class CollectedMessage extends Message {
    private final Map<Integer, StateMessage> _collectedStateMessage;

    public CollectedMessage(int senderId, int consensusTs, Map<Integer, StateMessage> collectedState) {
        super("COLLECTED", senderId, consensusTs);
        _collectedStateMessage = collectedState;
    }

    @Override
    public String toJsonString() {
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("event", _type);
        jsonContent.put("consensusInstance", _consensusTs);
        jsonContent.put("senderId", _senderId);

        JSONArray collectedStateList = new JSONArray();

        for (Map.Entry<Integer, StateMessage> stateMessage : _collectedStateMessage.entrySet()) {
            State state = stateMessage.getValue().getState();
            String hash = stateMessage.getValue().getHash();
            JSONObject jsonState = new JSONObject();
            jsonState.put("senderId", stateMessage.getKey());
            jsonState.put("hash", hash);
            jsonState.put("state", state.toJson());
            collectedStateList.put(jsonState);
        }
        jsonContent.put("collectedStateList", collectedStateList);
        return jsonContent.toString();
    }
}