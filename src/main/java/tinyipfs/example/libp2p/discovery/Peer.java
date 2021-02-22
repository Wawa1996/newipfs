package tinyipfs.example.libp2p.discovery;

import java.util.Objects;

public interface Peer {

    default NodeId getId() {
        return getAddress().getId();
    }

    PeerAddress getAddress();

    boolean isConnected();



    boolean connectionInitiatedLocally();

    boolean connectionInitiatedRemotely();

    default boolean idMatches(final Peer other) {
        return other != null && Objects.equals(getId(), other.getId());
    }

}

