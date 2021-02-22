package tinyipfs.example.libp2p.libp2p;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.KEY_TYPE;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.dsl.Builder;
import io.libp2p.core.dsl.BuilderJKt;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.dsl.MuxersBuilder;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.mux.mplex.MplexStreamMuxer;
import io.libp2p.protocol.Ping;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tuweni.bytes.Bytes;
import tinyipfs.example.libp2p.discovery.*;
import tinyipfs.util.IpUtil;
import tinyipfs.util.MultiaddrUtil;
import tinyipfs.util.SafeFuture;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static tinyipfs.util.SafeFuture.failedFuture;


/**
 * description: ChatNode <br>
 *
 * @author xie hui <br>
 * @version 1.0 <br>
 * @date 2020/9/8 18:43 <br>
 */
@Slf4j
public class Libp2pNetwork implements P2PNetwork<Peer> {

    private final Host host;
    private final PrivKey privKeyBytes;
    private final NodeId nodeId;

    private final InetAddress privateAddress;
    PeerManager peerManager;
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    int listenPort = 11112;
    private final Multiaddr advertisedAddr;
    public HandlerT handlerT = new HandlerT();
    public Libp2pNetwork(){
        privateAddress = IpUtil.getLocalAddress();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("libp2p-%d").build());
        peerManager = new PeerManager(scheduler);
//        host = new HostBuilder().protocol(handlerT).secureChannel(NoiseXXSecureChannel::new).listen("/ip4/"+privateAddress.getHostAddress()+"/tcp/"+listenPort).build();

//        String prikey = "0x0802122074ca7d1380b2c407be6878669ebb5c7a2ee751bb18198f1a0f214bcb93b894b5";
        String prikey = "0x0802122074ca7d1380b2c407be6878669ebb5c7a2ee751bb18198f1a0f214bcb93b89411";
        Bytes pub = Bytes.fromHexString(prikey);
        this.privKeyBytes = KeyKt.unmarshalPrivateKey(pub.toArrayUnsafe());

        this.nodeId = new LibP2PNodeId(PeerId.fromPubKey(privKeyBytes.publicKey()));
        advertisedAddr =
                MultiaddrUtil.fromInetSocketAddress(
                        new InetSocketAddress("192.168.3.5", listenPort),nodeId);
        System.out.println("nodeid = "+nodeId);
        host = BuilderJKt.hostJ(Builder.Defaults.None,
                b->{
                    b.getIdentity().setFactory(()-> privKeyBytes);
                    b.getTransports().add(TcpTransport::new);
                    b.getSecureChannels().add(NoiseXXSecureChannel::new);
                    b.getMuxers().add(StreamMuxerProtocol.getMplex());
                    b.getNetwork().listen(advertisedAddr.toString());
                    b.getProtocols().add(handlerT);
                    b.getDebug().getBeforeSecureHandler().addLogger(LogLevel.DEBUG, "wire.ciphered");
                    Firewall firewall = new Firewall(Duration.ofSeconds(30));
                    b.getDebug().getBeforeSecureHandler().addNettyHandler(firewall);
                    b.getDebug().getMuxFramesHandler().addLogger(LogLevel.DEBUG, "wire.mux");

                    b.getConnectionHandlers().add(peerManager);
                });
        System.out.println("host = "+host.getPeerId().toString());
    }
    @Override
    public SafeFuture<?> start() {
        if (!state.compareAndSet(State.IDLE, State.RUNNING)) {
            return SafeFuture.failedFuture(new IllegalStateException("Network already started"));
        }
        log.info("Starting libp2p network...");
        return SafeFuture.of(host.start())
                .thenApply(
                        i -> {
                            log.info(getNodeAddress());
                            return null;
                        });
    }

    @Override
    public SafeFuture<Peer> connect(final PeerAddress peer) {

        return peer.as(MultiaddrPeerAddress.class)
                .map(staticPeer -> peerManager.connect(staticPeer, host.getNetwork()))
                .orElseGet(
                        () ->
                                failedFuture(
                                        new IllegalArgumentException(
                                                "Unsupported peer address: " + peer.getClass().getName())));
    }

    public void connect1(String peer) {
        Multiaddr address = Multiaddr.fromString(peer);
        handlerT.dial(host,address);
    }




    /**
     * Parses a peer address in any of this network's supported formats.
     *
     * @param peerAddress the address to parse
     * @return a {@link PeerAddress} which is supported by {@link #connect(PeerAddress)} for
     * initiating connections
     */
    @Override
    public PeerAddress createPeerAddress(final String peerAddress) {
        return MultiaddrPeerAddress.fromAddress(peerAddress);
    }

    public MultiaddrPeerAddress createPeerAddress1(final String peerAddress) {
        return MultiaddrPeerAddress.fromAddress(peerAddress);
    }
    /**
     * Converts a {@link DiscoveryPeer} to a {@link PeerAddress} which can be used with this network's
     * {@link #connect(PeerAddress)} method.
     *
     * @param discoveryPeer the discovery peer to convert
     * @return a {@link PeerAddress} which is supported by {@link #connect(PeerAddress)} for
     * initiating connections
     */
    @Override
    public PeerAddress createPeerAddress(final DiscoveryPeer discoveryPeer) {
        return MultiaddrPeerAddress.fromDiscoveryPeer(discoveryPeer);
    }

    @Override
    public long subscribeConnect(PeerConnectedSubscriber subscriber) {
        return 0;
    }

    @Override
    public void unsubscribeConnect(long subscriptionId) {

    }

    @Override
    public boolean isConnected(final PeerAddress peerAddress) {
        return peerManager.getPeer(peerAddress.getId()).isPresent();
    }

    @Override
    public Bytes getPrivateKey() {
        return Bytes.wrap(privKeyBytes.raw());
    }

    @Override
    public Optional<Peer> getPeer(final NodeId id) {
        return peerManager.getPeer(id);
    }

    @Override
    public Stream<Peer> streamPeers() {
        return peerManager.streamPeers();
    }

    @Override
    public NodeId parseNodeId(final String nodeId) {
        return new LibP2PNodeId(PeerId.fromBase58(nodeId));
    }

    @Override
    public int getPeerCount() {
        return peerManager.getPeerCount();
    }

    @Override
    public String getNodeAddress() {
        return advertisedAddr.toString();
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
    }

    @Override
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Get the Ethereum Node Record (ENR) for the local node, if one exists.
     *
     * @return the local ENR.
     */
    @Override
    public Optional<String> getEnr() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDiscoveryAddress() {
        return Optional.empty();
    }

//    public void start(){
//        try {
//            host.start().get();
//            log.info("Node started and listening on ");
//            log.info(host.listenAddresses().toString());
//
//            int queryInterval = 6000;
//            String serviceTag = "_ipfs-discovery._udp";
//            String serviceTagLocal = serviceTag + ".local.";
//            peerFinder = new MDnsDiscovery(host, serviceTagLocal, queryInterval,privateAddress);
//            peerFinder.getNewPeerFoundListeners().add(peerInfo -> {
//                System.out.println("find peer : " + peerInfo.getPeerId().toString());
//                Unit u = Unit.INSTANCE;
//
//                if (!peerInfo.getAddresses().toString().contains(this.getAddress()) && !knownNodes.containsKey(peerInfo.getPeerId())) {
//                    node.setPeerInfo(peerInfo);
//                    knownNodes.put(peerInfo.getPeerId(), node);
//                    peers.add(node);
//                    String ip = peerInfo.getAddresses().toString() + "/ipfs/" +
//                            peerInfo.getPeerId().toString();
//                    ip = ip.replace("[", "").replace("]", "");
//                    System.out.println("ip = " + ip);
//                    Multiaddr address = Multiaddr.fromString(ip);
//                    handler.dial(this.host, address);
//                }
//                return u;
//            });
//            peerFinder.start();
//            log.info("Peer finder started ");
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
    public static byte[] toByteArray(String hexString) {
        if (StringUtils.isEmpty(hexString))
            throw new IllegalArgumentException("this hexString must not be empty");

        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    @Override
    public SafeFuture<?> stop() {
        if (!state.compareAndSet(State.RUNNING, State.STOPPED)) {
            return SafeFuture.COMPLETE;
        }
        log.debug("JvmLibP2PNetwork.stop()");
        return SafeFuture.of(host.stop());
    }

    public Host getHost(){
        return host;
    }

        public String getAddress(){
            return "/ip4/"+privateAddress.getHostAddress()+
                    "/tcp/"+listenPort;
        }
    @FunctionalInterface
    public interface PrivateKeyProvider {
        PrivKey get();
    }
    public static String bytes2hex01(byte[] bytes)
    {
        /**
         * 第一个参数的解释，记得一定要设置为1
         *  signum of the number (-1 for negative, 0 for zero, 1 for positive).
         */
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }
}
