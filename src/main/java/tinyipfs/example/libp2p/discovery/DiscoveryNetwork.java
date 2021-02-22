package tinyipfs.example.libp2p.discovery;

import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes;
import tinyipfs.util.SafeFuture;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class DiscoveryNetwork {
    private final P2PNetwork p2pNetwork;
    private final DiscoveryService discoveryService;
    private final ConnectionManager connectionManager;

    DiscoveryNetwork(
            final P2PNetwork p2pNetwork,
            final DiscoveryService discoveryService,
            final ConnectionManager connectionManager) {
        this.p2pNetwork = p2pNetwork;
        this.discoveryService = discoveryService;
        this.connectionManager = connectionManager;
        initialize();
    }



    private void initialize() {
        getEnr().ifPresent(enr->log.info("PreGenesis Local ENR: {}", enr));
        // Set connection manager peer predicate so that we don't attempt to connect peers with
        // different fork digests
        connectionManager.addPeerPredicate(this::dontConnectPeersWithDifferentForkDigests);
    }
    //不连接不同分叉的节点，
    private boolean dontConnectPeersWithDifferentForkDigests(DiscoveryPeer peer) {
        return false;
    }

    public static  DiscoveryNetwork create(
            final AsyncRunner asyncRunner,
            final KeyValueStore<String, Bytes> kvStore,
            final P2PNetwork p2pNetwork,
            final DiscoveryConfig discoveryConfig,
            final NetworkConfig p2pConfig) {
        final DiscoveryService discoveryService =
                createDiscoveryService(discoveryConfig, p2pConfig, kvStore, p2pNetwork.getPrivateKey());
        final ConnectionManager connectionManager =
                new ConnectionManager(
                        discoveryService,
                        asyncRunner,
                        p2pNetwork,
                        discoveryConfig.getStaticPeers().stream()
                                .map(p2pNetwork::createPeerAddress)
                                .collect(toList()));
        return new DiscoveryNetwork(p2pNetwork, discoveryService, connectionManager);
    }

    private static DiscoveryService createDiscoveryService(
            final DiscoveryConfig discoConfig,
            final NetworkConfig p2pConfig,
            final KeyValueStore<String, Bytes> kvStore,
            final Bytes privateKey) {
        final DiscoveryService discoveryService;
        if (discoConfig.isDiscoveryEnabled()) {
            discoveryService = DiscV5Service.create(discoConfig, p2pConfig, kvStore, privateKey);
        } else {
            discoveryService = new NoOpDiscoveryService();
        }
        return discoveryService;
    }

    public SafeFuture<?> start() {
        return SafeFuture.allOfFailFast(p2pNetwork.start(), discoveryService.start())
                .thenCompose(__ -> connectionManager.start())
                .thenRun(() -> getEnr().ifPresent(enr->log.info("Local ENR: {}", enr)));
    }
    public void addStaticPeer(final String peerAddress) {
        connectionManager.addStaticPeer(p2pNetwork.createPeerAddress(peerAddress));
    }

    public Optional<String> getEnr() {
        return discoveryService.getEnr();
    }

    public Optional<String> getDiscoveryAddress() {
        return discoveryService.getDiscoveryAddress();
    }

    public Optional getPeer(final NodeId id) {
        return p2pNetwork.getPeer(id);
    }

    public Stream streamPeers() {
        return p2pNetwork.streamPeers();
    }
}
