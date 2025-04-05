import org.json.JSONObject;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class StateMessage extends Message{
    private final State _state;
    private String _stateHash;

    public StateMessage(int senderId, int consensusTs, State state) {
        super("STATE", senderId, consensusTs);
        _state = state;
    }

    public StateMessage(int senderId, int consensusTs, State state, String hash) {
        super("STATE", senderId, consensusTs);
        _state = state;
        _stateHash = hash;
    }

    public static StateMessage fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);

        // Extract values from JSON
        int senderId = json.getInt("senderId");
        int consensusTs = json.getInt("consensusInstance");
        String stateHash = json.getString("hash");
        State state = State.fromJson(json.getJSONObject("state"));
        return new StateMessage(senderId, consensusTs, state, stateHash);
    }

    public String createSignedJsonString(RSAPrivateKey privateKey){
        JSONObject jsonState =_state.toJson();
        try {
            _stateHash = MessageSigner.signMessage(jsonState.toString(), privateKey);
        } catch (Exception e){
            System.out.println("failed signing message with state " + _stateHash);
            e.printStackTrace();
        }
        return toJsonString();
    }

    @Override
    public String toJsonString(){
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("event", _type);
        jsonContent.put("state", _state.toJson());
        jsonContent.put("consensusInstance", _consensusTs);
        jsonContent.put("senderId", _senderId);
        jsonContent.put("hash", _stateHash);
        return jsonContent.toString();
    }

    public boolean verifyStateHash() {
        RSAPublicKey publicKey = KeyLoader.loadPublicKeyFromFile(_senderId);
        JSONObject jsonState = _state.toJson();

        return MessageSigner.verifySignature(jsonState.toString(), _stateHash, publicKey);
    }

    public State getState(){
        return _state;
    }

    public String getHash(){
        return _stateHash;
    }


}
