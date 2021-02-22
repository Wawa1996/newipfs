package tinyipfs.example.libp2p.discovery;

import org.apache.tuweni.bytes.Bytes;
import tinyipfs.util.SafeFuture;

import java.util.Optional;
import java.util.stream.Stream;

public class NoOpDiscoveryService implements DiscoveryService {

    @Override
    public SafeFuture<?> start() {
        return SafeFuture.COMPLETE;
    }

    @Override
    public SafeFuture<?> stop() {
        return SafeFuture.COMPLETE;
    }

    @Override
    public Stream<DiscoveryPeer> streamKnownPeers() {
        return Stream.empty();
    }

    @Override
    public SafeFuture<Void> searchForPeers() {
        return SafeFuture.COMPLETE;
    }

    @Override
    public Optional<String> getEnr() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDiscoveryAddress() {
        return Optional.empty();
    }

    @Override
    public void updateCustomENRField(String fieldName, Bytes value) {}
}
