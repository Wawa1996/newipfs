package tinyipfs.example.libp2p.discovery;

public interface Cancellable {

    void cancel();

    boolean isCancelled();
}
