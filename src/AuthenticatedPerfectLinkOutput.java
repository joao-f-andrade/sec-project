import java.util.Objects;

public record AuthenticatedPerfectLinkOutput(int nodeId, String content) {
    public AuthenticatedPerfectLinkOutput {
        Objects.requireNonNull(content);
    }
}