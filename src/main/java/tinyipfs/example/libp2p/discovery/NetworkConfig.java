package tinyipfs.example.libp2p.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.InetAddresses.isInetAddress;

public class NetworkConfig {
    private static final Logger LOG = LogManager.getLogger();


    private final boolean isEnabled;
    private final Optional<String> privateKeyFile;
    private final String networkInterface;
    private final Optional<String> advertisedIp;
    private final int listenPort;
    private final OptionalInt advertisedPort;

    private NetworkConfig(
            final boolean isEnabled,
            final Optional<String> privateKeyFile,
            final String networkInterface,
            final Optional<String> advertisedIp,
            final int listenPort,
            final OptionalInt advertisedPort) {

        this.privateKeyFile = privateKeyFile;
        this.networkInterface = networkInterface;

        this.advertisedIp = advertisedIp.filter(ip -> !ip.isBlank());
        this.isEnabled = isEnabled;
        if (this.advertisedIp.map(ip -> !isInetAddress(ip)).orElse(false)) {
            throw new InvalidConfigurationException(
                    String.format(
                            "Advertised ip (%s) is set incorrectly.", this.advertisedIp.orElse("EMPTY")));
        }

        this.listenPort = listenPort;
        this.advertisedPort = advertisedPort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void validateListenPortAvailable() {
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Optional<String> getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public String getAdvertisedIp() {
        return resolveAnyLocalAddress(advertisedIp.orElse(networkInterface));
    }

    public boolean hasUserExplicitlySetAdvertisedIp() {
        return advertisedIp.isPresent();
    }

    public int getListenPort() {
        return listenPort;
    }

    public int getAdvertisedPort() {
        return advertisedPort.orElse(listenPort);
    }

    private String resolveAnyLocalAddress(final String ipAddress) {
        try {
            final InetAddress advertisedAddress = InetAddress.getByName(ipAddress);
            if (advertisedAddress.isAnyLocalAddress()) {
                return InetAddress.getLocalHost().getHostAddress();
            } else {
                return ipAddress;
            }
        } catch (UnknownHostException err) {
            LOG.error(
                    "Unable to start LibP2PNetwork due to failed attempt at obtaining host address", err);
            return ipAddress;
        }
    }

    public static class Builder {
        public static final int DEFAULT_P2P_PORT = 9000;


        private Boolean isEnabled = true;
        private Optional<String> privateKeyFile = Optional.empty();
        private String networkInterface = "0.0.0.0";
        private Optional<String> advertisedIp = Optional.empty();
        private Integer listenPort = DEFAULT_P2P_PORT;
        private OptionalInt advertisedPort = OptionalInt.empty();

        private Builder() {}

        public NetworkConfig build() {
            return new NetworkConfig(
                    isEnabled,
                    privateKeyFile,
                    networkInterface,
                    advertisedIp,
                    listenPort,
                    advertisedPort);
        }

        public Builder isEnabled(final Boolean enabled) {
            checkNotNull(enabled);
            isEnabled = enabled;
            return this;
        }


        public Builder privateKeyFile(final String privateKeyFile) {
            checkNotNull(privateKeyFile);
            this.privateKeyFile = Optional.of(privateKeyFile).filter(f -> !f.isBlank());
            return this;
        }

        public Builder networkInterface(final String networkInterface) {
            checkNotNull(networkInterface);
            this.networkInterface = networkInterface;
            return this;
        }

        public Builder advertisedIp(final Optional<String> advertisedIp) {
            checkNotNull(advertisedIp);
            this.advertisedIp = advertisedIp;
            return this;
        }

        public Builder listenPort(final Integer listenPort) {
            checkNotNull(listenPort);
            this.listenPort = listenPort;
            return this;
        }

        public Builder advertisedPort(final OptionalInt advertisedPort) {
            checkNotNull(advertisedPort);
            this.advertisedPort = advertisedPort;
            return this;
        }
    }
}
