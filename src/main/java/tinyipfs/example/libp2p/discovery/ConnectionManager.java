package tinyipfs.example.libp2p.discovery;


import lombok.extern.slf4j.Slf4j;
import tinyipfs.util.SafeFuture;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


@Slf4j
public class ConnectionManager extends Service{
    private static final Duration DISCOVERY_INTERVAL = Duration.ofSeconds(30);
    Set<PeerAddress> staticPeers;
    private final AsyncRunner asyncRunner;
    private final P2PNetwork network;
    DiscoveryService discoveryService;
    private volatile Cancellable periodicPeerSearch;
    private final Collection<Predicate<DiscoveryPeer>> peerPredicates = new CopyOnWriteArrayList<>();
    public ConnectionManager(
            final DiscoveryService discoveryService,
            final AsyncRunner asyncRunner,
            final P2PNetwork network,
            final List<PeerAddress> peerAddresses) {
        this.asyncRunner = asyncRunner;
        this.network = network;
        staticPeers = new HashSet<>(peerAddresses);
        this.discoveryService = discoveryService;
    }


    @Override
    protected SafeFuture<?> doStart() {
        log.trace("Starting discovery manager");
        synchronized (this) {
            staticPeers.forEach(this::createPersistentConnection);
        }
        periodicPeerSearch =
                asyncRunner.runWithFixedDelay(
                        this::searchForPeers,
                        DISCOVERY_INTERVAL,
                        error -> log.error("Error while searching for peers", error));
        connectToKnownPeers();
        searchForPeers();
        return SafeFuture.COMPLETE;
    }


    private void connectToKnownPeers() {
                 discoveryService.streamKnownPeers().filter(this::isPeerValid).
                    map(network::createPeerAddress).filter(peerAddress -> !network.isConnected(peerAddress))
                         .forEach(this::attemptConnection);
    }



//改成dial连接方式
    private void attemptConnection(final PeerAddress peerAddress) {
        log.trace("Attempting to connect to {}", peerAddress.getId());
//        network.connect1(peerAddress);
    }
    private void searchForPeers() {
        if (!isRunning()) {
            log.trace("Not running so not searching for peers");
            return;
        }
        log.trace("Searching for peers");
        discoveryService
                .searchForPeers()
                .orTimeout(10, TimeUnit.SECONDS)
                .finish(
                        this::connectToKnownPeers,
                        error -> {
                            log.debug("Discovery failed", error);
                            connectToKnownPeers();
                        });
    }

    private void createPersistentConnection(final PeerAddress peerAddress) {
        maintainPersistentConnection(peerAddress);
    }

    private void maintainPersistentConnection(final PeerAddress peerAddress) {
        if (!isRunning()) {
            // We've been stopped so halt the process.
            return ;
        }
        log.debug("Connecting to peer {}", peerAddress);
        network.connect(peerAddress);
    }

    @Override
    protected SafeFuture<?> doStop() {
        final Cancellable peerSearchTask = this.periodicPeerSearch;
        if (peerSearchTask != null) {
            peerSearchTask.cancel();
        }
        return SafeFuture.COMPLETE;
    }

    public synchronized void addStaticPeer(final PeerAddress peerAddress) {
        if (!staticPeers.contains(peerAddress)) {
            staticPeers.add(peerAddress);
            createPersistentConnection(peerAddress);
        }
    }

    private boolean isPeerValid(DiscoveryPeer peer) {
        return !peer.getNodeAddress().getAddress().isAnyLocalAddress()
                && peerPredicates.stream().allMatch(predicate -> predicate.test(peer));
    }
    public void addPeerPredicate(final Predicate<DiscoveryPeer> predicate) {
        peerPredicates.add(predicate);
    }
}
