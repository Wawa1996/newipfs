package tinyipfs.example.libp2p.discovery;

import org.apache.tuweni.bytes.Bytes;
import tinyipfs.util.SafeFuture;

import java.util.Optional;
import java.util.stream.Stream;

public interface DiscoveryService {

    SafeFuture<?> start();

    SafeFuture<?> stop();

    Stream<DiscoveryPeer> streamKnownPeers();

    SafeFuture<Void> searchForPeers();

    Optional<String> getEnr();

    Optional<String> getDiscoveryAddress();

    void updateCustomENRField(String fieldName, Bytes value);
}