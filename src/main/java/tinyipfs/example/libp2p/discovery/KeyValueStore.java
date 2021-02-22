package tinyipfs.example.libp2p.discovery;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface KeyValueStore<TKey, TValue> {

    /** Puts a new value. If the value is {@code null} then the entry is removed if exist */
    void put(@NotNull TKey key, @NotNull TValue value);

    /** Removes entry with the specified key */
    void remove(@NotNull TKey key);

    /**
     * Returns a value corresponding to the key or {{@link Optional#empty()}} if entry doesn't exist
     * in the store
     */
    Optional<TValue> get(@NotNull TKey key);

    /**
     * Performs batch store update.
     *
     * <p>The implementation may override this default method and declare it to be an atomic store
     * update. Though this generic interface makes no restrictions on atomicity of this method
     */
    default void updateAll(Iterable<EntryUpdate<? extends TKey, ? extends TValue>> data) {
        data.forEach(
                update -> {
                    if (update.getType() == UpdateType.UPDATE) {
                        put(update.getKey(), update.getValue());
                    } else if (update.getType() == UpdateType.REMOVE) {
                        remove(update.getKey());
                    } else {
                        throw new IllegalArgumentException("Unknown type: " + update.getType());
                    }
                });
    }

    enum UpdateType {
        UPDATE,
        REMOVE
    }

    /** Represents a batched update entry */
    class EntryUpdate<K, V> {
        private final UpdateType type;
        private final K key;
        private final V value;

        public static <K, V> EntryUpdate<K, V> update(K key, V value) {
            return new EntryUpdate<>(UpdateType.UPDATE, key, value);
        }

        public static <K, V> EntryUpdate<K, V> remove(K key) {
            return new EntryUpdate<>(UpdateType.REMOVE, key, null);
        }

        private EntryUpdate(UpdateType type, K key, V value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }

        public UpdateType getType() {
            return type;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            if (getType() != UpdateType.UPDATE) {
                throw new IllegalStateException("No value for this update");
            }
            return value;
        }
    }
}
