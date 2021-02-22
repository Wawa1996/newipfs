package tinyipfs.example.libp2p.discovery;

public class InvalidConfigurationException extends RuntimeException {
    public InvalidConfigurationException(final String message) {
        super(message);
    }

    public InvalidConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigurationException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
