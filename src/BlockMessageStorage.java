import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BlockMessageStorage {
    int valueTs, epochTs, nodeId;
    String event;
    String content;

    public BlockMessageStorage(int valueTs, int epochTs, int nodeId, String event, String content) {
        this.valueTs = valueTs;
        this.epochTs = epochTs;
        this.nodeId = nodeId;
        this.event = event;
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockMessageStorage ts = (BlockMessageStorage) obj;
        return valueTs == ts.valueTs && epochTs == ts.epochTs && nodeId == ts.nodeId && Objects.equals(event, ts.event) && Objects.equals(content, this.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueTs, epochTs, nodeId, event, content);
    }

    @Override
    public String toString() {
        return "(" + valueTs + ", " + epochTs + ", " + nodeId + ") -> " + event + content;
    }
}
