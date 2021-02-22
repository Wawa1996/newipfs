package tinyipfs.example.libp2p.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.EnrField;
import org.ethereum.beacon.discovery.schema.NodeRecord;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.function.Function;

public class NodeRecordConverter {
    private static final Logger LOG = LogManager.getLogger();

    static Optional<DiscoveryPeer> convertToDiscoveryPeer(final NodeRecord nodeRecord) {
        return nodeRecord
                .getTcpAddress()
                .map(address -> socketAddressToDiscoveryPeer(nodeRecord, address));
    }

    private static DiscoveryPeer socketAddressToDiscoveryPeer(
            final NodeRecord nodeRecord, final InetSocketAddress address) {


        final Bitvector persistentSubnets =
                parseField(
                        nodeRecord,
                        "attnets",
                        attestionSubnetsField ->
                                Bitvector.fromBytes(attestionSubnetsField, 64))
                        .orElse(new Bitvector(64));

        return new DiscoveryPeer(
                ((Bytes) nodeRecord.get(EnrField.PKEY_SECP256K1)), address, persistentSubnets);
    }

    private static <T> Optional<T> parseField(
            final NodeRecord nodeRecord, final String fieldName, final Function<Bytes, T> parse) {
        try {
            return Optional.ofNullable((Bytes) nodeRecord.get(fieldName)).map(parse);
        } catch (final Exception e) {
            LOG.debug("Failed to parse ENR field {}", fieldName, e);
            return Optional.empty();
        }
    }
}
