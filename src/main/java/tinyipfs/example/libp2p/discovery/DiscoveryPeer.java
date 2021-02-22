package tinyipfs.example.libp2p.discovery;

import com.google.common.base.Objects;
import org.apache.tuweni.bytes.Bytes;

import java.net.InetSocketAddress;

public class DiscoveryPeer {
    private final Bytes publicKey;
    private final InetSocketAddress nodeAddress;
    private final Bitvector persistentSubnets;

    public DiscoveryPeer(
            final Bytes publicKey,
            final InetSocketAddress nodeAddress,
            final Bitvector persistentSubnets) {
        this.publicKey = publicKey;
        this.nodeAddress = nodeAddress;
        this.persistentSubnets = persistentSubnets;
    }


    public Bytes getPublicKey() {
        return publicKey;
    }

    public InetSocketAddress getNodeAddress() {
        return nodeAddress;
    }


    public Bitvector getPersistentSubnets() {
        return persistentSubnets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscoveryPeer)) return false;
        DiscoveryPeer that = (DiscoveryPeer) o;
        return Objects.equal(getPublicKey(), that.getPublicKey())
                && Objects.equal(getNodeAddress(), that.getNodeAddress())
                && Objects.equal(getPersistentSubnets(), that.getPersistentSubnets());
    }

}
