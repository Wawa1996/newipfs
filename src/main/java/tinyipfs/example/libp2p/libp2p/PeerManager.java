package tinyipfs.example.libp2p.libp2p;

import io.libp2p.core.Connection;
import io.libp2p.core.ConnectionHandler;
import io.libp2p.core.Network;
import io.libp2p.core.PeerId;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import tinyipfs.example.libp2p.discovery.LibP2PNodeId;
import tinyipfs.example.libp2p.discovery.MultiaddrPeerAddress;
import tinyipfs.example.libp2p.discovery.NodeId;
import tinyipfs.example.libp2p.discovery.Peer;
import tinyipfs.util.SafeFuture;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

@Slf4j
public class PeerManager implements ConnectionHandler {


    private final ConcurrentHashMap<NodeId, Peer> connectedPeerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeId, SafeFuture<Peer>> pendingConnections =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public PeerManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    @Override
    public void handleConnection(@NotNull final Connection connection) {
        final PeerId remoteId = connection.secureSession().getRemoteId();
        log.debug( "Got new connection from " + remoteId);
        connection.closeFuture().thenRun(() -> log.debug( "Peer disconnected: " + remoteId));

    }
    public SafeFuture<Peer> connect(final MultiaddrPeerAddress peer, final Network network) {
        return pendingConnections.computeIfAbsent(peer.getId(), __ -> doConnect(peer, network));
    }

    private SafeFuture<Peer> doConnect(final MultiaddrPeerAddress peer, final Network network) {
        log.debug("Connecting to {}", peer);
        log.info("network = "+network.toString());

        return SafeFuture.of(() -> network.connect(peer.getMultiaddr()))
                .thenApply(
                        connection -> {
                            final LibP2PNodeId nodeId =
                                    new LibP2PNodeId(connection.secureSession().getRemoteId());
                            final Peer connectedPeer = connectedPeerMap.get(nodeId);
                            if (connectedPeer == null) {
                                if (connection.closeFuture().isDone()) {
                                    // Connection has been immediately closed and the peer already removed
                                    // Since the connection is closed anyway, we can create a new peer to wrap it.
                                } else {
                                    // Theoretically this should never happen because removing from the map is done
                                    // by the close future completing, but make a loud noise just in case.
                                    throw new IllegalStateException(
                                            "No peer registered for established connection to " + nodeId);
                                }
                            }
                            return connectedPeer;
                        })
                .whenComplete((result, error) -> pendingConnections.remove(peer.getId()));
    }
    public Optional<Peer> getPeer(NodeId id) {
        return Optional.ofNullable(connectedPeerMap.get(id));
    }
    public int getPeerCount() {
        return connectedPeerMap.size();
    }
    public Stream<Peer> streamPeers() {
        return connectedPeerMap.values().stream();
    }
}
