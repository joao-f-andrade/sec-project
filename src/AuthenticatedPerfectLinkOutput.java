import java.util.Objects;

public record AuthenticatedPerfectLinkOutput(int port, String content) {
    public AuthenticatedPerfectLinkOutput {
        Objects.requireNonNull(content);
    }
}