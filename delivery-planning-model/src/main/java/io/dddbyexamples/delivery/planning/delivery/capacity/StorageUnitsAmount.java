package io.dddbyexamples.delivery.planning.delivery.capacity;

import io.dddbyexamples.delivery.planning.Amounts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StorageUnitsAmount extends Amounts {

    public StorageUnitsAmount(Map<String, Long> storageUnitsAmount) {
        super(storageUnitsAmount);
    }

    public static StorageUnitsAmount empty() {
        return new StorageUnitsAmount(Collections.emptyMap());
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getPallets() {
        return get("pallet");
    }

    public long getTrailers() {
        return get("trailer");
    }

    public long getCages() {
        return get("cage");
    }

    public static class Builder {
        private Map<String, Long> amounts = new HashMap<>();

        public StorageUnitsAmount build() {
            return new StorageUnitsAmount(amounts);
        }

        public Builder addPallet(long amount) {
            add("pallet", amount);
            return this;
        }

        public Builder addTrailers(long amount) {
            add("trailer", amount);
            return this;
        }

        public Builder addCages(long amount) {
            add("cage", amount);
            return this;
        }

        public Builder addPallet(long amount, long limit) {
            add("pallet", Math.min(amount, limit));
            return this;
        }

        public Builder addTrailers(long amount, long limit) {
            add("trailer", Math.min(amount, limit));
            return this;
        }

        public Builder addCages(long amount, long limit) {
            add("cage", Math.min(amount, limit));
            return this;
        }

        private void add(String unit, long amount) {
            amounts.put(unit,
                    amounts.getOrDefault(unit, 0L) + Math.max(amount, 0));
        }
    }
}
