package tinyipfs.example.libp2p.discovery;

import org.apache.tuweni.bytes.Bytes;
import tinyipfs.util.SafeFuture;

import java.util.Optional;
import java.util.stream.Stream;

public interface P2PNetwork<P> {

    enum State {
        IDLE,
        RUNNING,
        STOPPED
    }

    /**
     * Connects to a Peer using a user supplied address. The address format is specific to the network
     * implementation. If a connection already exists for this peer, the future completes with the
     * existing peer.
     *
     * <p>The {@link PeerAddress} must have been created using the {@link #createPeerAddress(String)}
     * method of this same implementation.
     *
     * @param peer Peer to connect to.
     * @return A future which completes when the connection is established, containing the newly
     *     connected peer.
     */
    SafeFuture connect(PeerAddress peer);

//    void connect1(String peer);

    /**
     * Parses a peer address in any of this network's supported formats.
     *
     * @param peerAddress the address to parse
     * @return a {@link PeerAddress} which is supported by {@link #connect(PeerAddress)} for
     *     initiating connections
     */
    PeerAddress createPeerAddress(String peerAddress);

    /**
     * Converts a {@link DiscoveryPeer} to a {@link PeerAddress} which can be used with this network's
     * {@link #connect(PeerAddress)} method.
     *
     * @param discoveryPeer the discovery peer to convert
     * @return a {@link PeerAddress} which is supported by {@link #connect(PeerAddress)} for
     *     initiating connections
     */
    PeerAddress createPeerAddress(DiscoveryPeer discoveryPeer);

    long subscribeConnect(PeerConnectedSubscriber subscriber);

    void unsubscribeConnect(long subscriptionId);

    boolean isConnected(PeerAddress peerAddress);

    Bytes getPrivateKey();

    Optional getPeer(NodeId id);

    Stream streamPeers();

    NodeId parseNodeId(final String nodeId);

    int getPeerCount();

    String getNodeAddress();

    NodeId getNodeId();

    int getListenPort();

    /**
     * Get the Ethereum Node Record (ENR) for the local node, if one exists.
     *
     * @return the local ENR.
     */
    Optional<String> getEnr();

    Optional<String> getDiscoveryAddress();

    /**
     * Starts the P2P network layer.
     *
     * @return
     */
    SafeFuture<?> start();

    /** Stops the P2P network layer. */
    SafeFuture<?> stop();
}

