package tinyipfs.example.libp2p.discovery;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

import java.util.Optional;

public enum TekuMetricCategory implements MetricCategory {
    BEACON("beacon"),
    EVENTBUS("eventbus"),
    EXECUTOR("executor"),
    LIBP2P("libp2p"),
    NETWORK("network"),
    STORAGE("storage"),
    STORAGE_HOT_DB("storage_hot"),
    STORAGE_FINALIZED_DB("storage_finalized"),
    REMOTE_VALIDATOR("remote_validator"),
    VALIDATOR("validator"),
    VALIDATOR_PERFORMANCE("validator_performance");

    private final String name;

    TekuMetricCategory(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<String> getApplicationPrefix() {
        return Optional.empty();
    }
}
