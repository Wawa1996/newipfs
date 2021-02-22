package tinyipfs.example.libp2p.discovery;

@FunctionalInterface
public interface PeerConnectedSubscriber {

    void onConnected( );
}
