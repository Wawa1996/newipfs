package tinyipfs.example.libp2p.discovery;


import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.ethereum.beacon.discovery.DiscoverySystem;
import org.ethereum.beacon.discovery.DiscoverySystemBuilder;
import org.ethereum.beacon.discovery.schema.*;
import org.ethereum.beacon.discovery.storage.NewAddressHandler;
import tinyipfs.util.MultiaddrUtil;
import tinyipfs.util.SafeFuture;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class DiscV5Service extends Service implements DiscoveryService {
    private static final String SEQ_NO_STORE_KEY = "local-enr-seqno";
    int ATTESTATION_SUBNET_COUNT = 64;
    public static DiscoveryService create(
            final DiscoveryConfig discoConfig,
            final NetworkConfig p2pConfig,
            final KeyValueStore<String, Bytes> kvStore,
            final Bytes privateKey) {
        return new DiscV5Service(discoConfig, p2pConfig, kvStore, privateKey);
    }

    private final DiscoverySystem discoverySystem;
    private final KeyValueStore<String, Bytes> kvStore;

    private DiscV5Service(
            final DiscoveryConfig discoConfig,
            NetworkConfig p2pConfig,
            KeyValueStore<String, Bytes> kvStore,
            final Bytes privateKey) {
        final String listenAddress = p2pConfig.getNetworkInterface();
        final int listenPort = p2pConfig.getListenPort();
        final String advertisedAddress = p2pConfig.getAdvertisedIp();
        final int advertisedPort = p2pConfig.getAdvertisedPort();
        final List<String> bootnodes = discoConfig.getBootnodes();
        final UInt64 seqNo =
                kvStore.get(SEQ_NO_STORE_KEY).map(UInt64::fromBytes).orElse(UInt64.ZERO).add(1);
        final NewAddressHandler maybeUpdateNodeRecordHandler =
                maybeUpdateNodeRecord(p2pConfig.hasUserExplicitlySetAdvertisedIp());
        discoverySystem =
                new DiscoverySystemBuilder()
                        .listen(listenAddress, listenPort)
                        .privateKey(privateKey)
                        .bootnodes(bootnodes.toArray(new String[0]))
                        .localNodeRecord(
                                new NodeRecordBuilder()
                                        .privateKey(privateKey)
                                        .address(advertisedAddress, advertisedPort)
                                        .seq(seqNo)
                                        .build())
                        .newAddressHandler(maybeUpdateNodeRecordHandler)
                        .localNodeRecordListener(this::localNodeRecordUpdated)
                        .build();
        this.kvStore = kvStore;
    }

    private NewAddressHandler maybeUpdateNodeRecord(boolean userExplicitlySetAdvertisedIpOrPort) {
        return (oldRecord, proposedNewRecord) -> {
            if (userExplicitlySetAdvertisedIpOrPort) {
                return Optional.of(oldRecord);
            } else {
                return Optional.of(proposedNewRecord);
            }
        };
    }

    private void localNodeRecordUpdated(NodeRecord oldRecord, NodeRecord newRecord) {
        kvStore.put(SEQ_NO_STORE_KEY, newRecord.getSeq().toBytes());
    }

    @Override
    protected SafeFuture<?> doStart() {
        return SafeFuture.of(discoverySystem.start());
    }

    @Override
    protected SafeFuture<?> doStop() {
        discoverySystem.stop();
        return SafeFuture.completedFuture(null);
    }

    @Override
    public Stream<DiscoveryPeer> streamKnownPeers() {
        return activeNodes().map(NodeRecordConverter::convertToDiscoveryPeer).flatMap(Optional::stream);
    }

    @Override
    public SafeFuture<Void> searchForPeers() {
        return SafeFuture.of(discoverySystem.searchForNewPeers());
    }

    @Override
    public Optional<String> getEnr() {
        return Optional.of(discoverySystem.getLocalNodeRecord().asEnr());
    }

    @Override
    public Optional<String> getDiscoveryAddress() {
        final NodeRecord nodeRecord = discoverySystem.getLocalNodeRecord();
        if (nodeRecord.getUdpAddress().isEmpty()) {
            return Optional.empty();
        }
        final DiscoveryPeer discoveryPeer =
                new DiscoveryPeer(
                        (Bytes) nodeRecord.get(EnrField.PKEY_SECP256K1),
                        nodeRecord.getUdpAddress().get(),
                        new Bitvector(ATTESTATION_SUBNET_COUNT));

        return Optional.of(MultiaddrUtil.fromDiscoveryPeerAsUdp(discoveryPeer).toString());
    }

    @Override
    public void updateCustomENRField(String fieldName, Bytes value) {
        discoverySystem.updateCustomFieldValue(fieldName, value);
    }

    private Stream<NodeRecord> activeNodes() {
        return discoverySystem
                .streamKnownNodes()
                .filter(record -> record.getStatus() == NodeStatus.ACTIVE)
                .map(NodeRecordInfo::getNode);
    }
}
